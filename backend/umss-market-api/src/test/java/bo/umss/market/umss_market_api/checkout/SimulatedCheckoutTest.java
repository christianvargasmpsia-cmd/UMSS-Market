package bo.umss.market.umss_market_api.checkout;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import bo.umss.market.umss_market_api.application.dto.SimulatedCheckoutRequest;
import bo.umss.market.umss_market_api.application.dto.SimulatedCheckoutRequest.Item;
import bo.umss.market.umss_market_api.application.usecases.*;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.exceptions.CheckoutException;
import bo.umss.market.umss_market_api.infrastructure.adapters.*;
import bo.umss.market.umss_market_api.infrastructure.controllers.SimulatedCheckoutController;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.*;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = SimulatedCheckoutTest.Config.class)
@Import({SimulatedCheckoutUseCase.class, GetOrderByIdUseCase.class,
    JpaCheckoutRepositoryAdapter.class, JpaOrderRepositoryAdapter.class,
    JpaOrderItemRepositoryAdapter.class, JpaPublicationRepositoryAdapter.class})
class SimulatedCheckoutTest {
    @Configuration
    @EnableJpaRepositories(basePackageClasses = JpaOrderRepository.class)
    @EntityScan(basePackageClasses = OrderEntity.class)
    static class Config {}

    @Autowired SimulatedCheckoutUseCase checkout;
    @Autowired JpaOrderRepository orders;
    @Autowired JpaOrderItemRepository items;
    @Autowired JpaUserRepository users;
    @Autowired JpaPublicationRepository publications;
    UUID user;
    UUID product;

    @BeforeEach void seed() {
        items.deleteAll(); orders.deleteAll(); publications.deleteAll(); users.deleteAll();
        user = UUID.randomUUID();
        users.saveAndFlush(UserEntity.builder().id(user).ru(user.toString()).nombre("Comprador")
            .email(user + "@test.invalid").passwordHash("test-only").build());
        product = UUID.randomUUID();
        publications.saveAndFlush(PublicationEntity.builder().id(product).storeId(UUID.randomUUID())
            .nombre("Libro").precio(new BigDecimal("12.50")).stock(5).activa(true).build());
    }

    SimulatedCheckoutRequest request(UUID id, int quantity) {
        return new SimulatedCheckoutRequest(id, user, List.of(new Item(product, quantity)));
    }

    @Test void persistsConfirmedPurchaseAtDatabasePriceAndReplaysWithoutDiscountingAgain() {
        var request = request(UUID.randomUUID(), 2);
        var result = checkout.execute(request);
        assertEquals(OrderStatus.CONFIRMADO, result.getEstado());
        assertEquals(0, new BigDecimal("25.00").compareTo(result.getTotal()));
        assertEquals(2, result.getItems().getFirst().getCantidad());
        assertEquals("Libro", result.getItems().getFirst().getProducto());
        assertEquals(3, publications.findById(product).orElseThrow().getStock());
        assertEquals(result.getId(), checkout.execute(request).getId());
        assertEquals(1, orders.count());
        assertEquals(1, items.count());
        assertEquals(3, publications.findById(product).orElseThrow().getStock());
    }

    @Test void rollsBackEarlierStockUpdatesWhenAnotherProductIsUnavailable() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID last = UUID.fromString("00000000-0000-0000-0000-000000000002");
        for (UUID id : List.of(first, last))
            publications.saveAndFlush(PublicationEntity.builder().id(id).storeId(UUID.randomUUID())
                .nombre("Producto").precio(BigDecimal.TEN).stock(id.equals(first) ? 5 : 0).activa(true).build());
        var request = new SimulatedCheckoutRequest(UUID.randomUUID(), user,
            List.of(new Item(first, 2), new Item(last, 1)));
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request)).getStatus());
        assertEquals(5, publications.findById(first).orElseThrow().getStock());
        assertEquals(0, orders.count());
        assertEquals(0, items.count());
    }

    @Test void simultaneousRetriesCreateOneOrder() throws Exception {
        var request = request(UUID.randomUUID(), 2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = executor.invokeAll(List.of(
                () -> checkout.execute(request).getId(), () -> checkout.execute(request).getId()));
            assertEquals(tasks.get(0).get(), tasks.get(1).get());
        }
        assertEquals(1, orders.count());
        assertEquals(3, publications.findById(product).orElseThrow().getStock());
    }

    @Test void competingBuyersCannotOversell() throws Exception {
        UUID other = UUID.randomUUID();
        users.saveAndFlush(UserEntity.builder().id(other).ru(other.toString()).nombre("Otro")
            .email(other + "@test.invalid").passwordHash("test-only").build());
        var first = request(UUID.randomUUID(), 4);
        var second = new SimulatedCheckoutRequest(UUID.randomUUID(), other, List.of(new Item(product, 4)));
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Boolean>> tasks = List.of(() -> attempt(first), () -> attempt(second));
            var outcomes = executor.invokeAll(tasks);
            assertNotEquals(outcomes.get(0).get(), outcomes.get(1).get());
        }
        assertEquals(1, publications.findById(product).orElseThrow().getStock());
        assertEquals(1, orders.count());
    }

    boolean attempt(SimulatedCheckoutRequest request) {
        try { checkout.execute(request); return true; }
        catch (CheckoutException ex) { assertEquals(409, ex.getStatus()); return false; }
    }

    @Test void rejectsMalformedRequests() {
        List<SimulatedCheckoutRequest> invalid = new ArrayList<>();
        invalid.add(null);
        invalid.add(new SimulatedCheckoutRequest(null, user, List.of(new Item(product, 1))));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), null, List.of(new Item(product, 1))));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, null));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of()));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, Collections.nCopies(101, new Item(product, 1))));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, Arrays.asList((Item) null)));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(null, 1))));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(product, null))));
        invalid.add(request(UUID.randomUUID(), 0));
        invalid.add(request(UUID.randomUUID(), -1));
        invalid.add(new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(product, 1), new Item(product, 1))));
        for (var request : invalid)
            assertEquals(400, assertThrows(CheckoutException.class, () -> checkout.execute(request)).getStatus());
        assertEquals(0, orders.count());
    }

    @Test void rejectsMissingUserAndPublication() {
        var missingUser = new SimulatedCheckoutRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(new Item(product, 1)));
        assertEquals(404, assertThrows(CheckoutException.class, () -> checkout.execute(missingUser)).getStatus());
        var missingPublication = new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(UUID.randomUUID(), 1)));
        assertEquals(404, assertThrows(CheckoutException.class, () -> checkout.execute(missingPublication)).getStatus());
    }

    @Test void rejectsUnavailableProductsAndInvalidPricesAndUnknownStock() {
        var publication = publications.findById(product).orElseThrow();
        for (Boolean active : Arrays.asList(false, null)) {
            publication.setActiva(active); publications.saveAndFlush(publication);
            assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(UUID.randomUUID(), 1))).getStatus());
        }
        publication.setActiva(true);
        for (BigDecimal price : List.of(BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            publication.setPrecio(price); publications.saveAndFlush(publication);
            assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(UUID.randomUUID(), 1))).getStatus());
        }
        publication.setPrecio(BigDecimal.TEN); publication.setStock(null); publications.saveAndFlush(publication);
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(UUID.randomUUID(), 1))).getStatus());
    }

    @Test void rejectsReuseWithDifferentItemsUserOrStatus() {
        UUID id = UUID.randomUUID();
        checkout.execute(request(id, 1));
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(id, 2))).getStatus());
        var saved = orders.findById(id).orElseThrow();
        saved.setUserId(UUID.randomUUID()); orders.saveAndFlush(saved);
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(id, 1))).getStatus());
        saved.setUserId(user); saved.setStatus(OrderStatus.CANCELADO); orders.saveAndFlush(saved);
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(id, 1))).getStatus());
        assertEquals(4, publications.findById(product).orElseThrow().getStock());
    }

    @Test void httpContractReturnsPurchaseAndReadableStockRejection() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SimulatedCheckoutController(checkout)).build();
        String body = "{\"requestId\":\"" + UUID.randomUUID() + "\",\"userId\":\"" + user
            + "\",\"items\":[{\"publicationId\":\"" + product + "\",\"quantity\":2}]}";
        mvc.perform(post("/api/orders/checkout/simulated").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CONFIRMADO"))
            .andExpect(jsonPath("$.total").value(25.0)).andExpect(jsonPath("$.items[0].cantidad").value(2));
        mvc.perform(post("/api/orders/checkout/simulated").contentType(MediaType.APPLICATION_JSON)
            .content(body.replace("\"quantity\":2", "\"quantity\":99").replaceAll(
                "(?<=requestId\\\":\\\")[^\\\"]+", UUID.randomUUID().toString())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
    }

    @Test void savesDeliveryAndDifferentiatesCashFromSimulatedQr() {
        for (String method : List.of("EFECTIVO", "QR_SIMULADO")) {
            var delivery = new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan Pérez", "70000000", method);
            var request = new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(product, 1)), delivery);
            var result = checkout.execute(request);
            assertEquals("Comedor Central", result.getMeetingPoint());
            assertEquals("Juan Pérez", result.getRecipientName());
            assertEquals("70000000", result.getRecipientPhone());
            assertEquals(method, result.getPaymentMethod());
            assertEquals(method.equals("EFECTIVO") ? "PENDIENTE" : "PAGADO_SIMULADO", result.getPaymentStatus());
            assertEquals(result.getId(), checkout.execute(request).getId());
            var stored = orders.findById(result.getId()).orElseThrow();
            assertEquals(result.getPaymentStatus(), stored.getPaymentStatus());
            assertEquals(result.getRecipientName(), stored.getRecipientName());
            var listed = new GetOrdersUseCase(new JpaOrderRepositoryAdapter(orders),
                new JpaOrderItemRepositoryAdapter(items), new JpaPublicationRepositoryAdapter(publications)).execute(user);
            assertTrue(listed.stream().anyMatch(o -> result.getId().equals(o.getId()) && method.equals(o.getPaymentMethod())));
        }
        assertEquals(3, publications.findById(product).orElseThrow().getStock());
    }

    @Test void rejectsInvalidDeliveryWithoutPersistingOrDiscountingStock() {
        var invalid = List.of(
            new SimulatedCheckoutRequest.Delivery(null, "Juan", "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Lugar inexistente", "Juan", "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", null, "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "  ", "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "a".repeat(101), "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", null, "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "123", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "70000000", null),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "70000000", "TARJETA")
        );
        for (var d : invalid) {
            var request = new SimulatedCheckoutRequest(UUID.randomUUID(), user, List.of(new Item(product, 1)), d);
            assertEquals(400, assertThrows(CheckoutException.class, () -> checkout.execute(request)).getStatus());
        }
        assertEquals(0, orders.count());
        assertEquals(5, publications.findById(product).orElseThrow().getStock());
    }

    @Test void cannotChangeDeliveryOrPaymentWhenReplayingAnOrder() {
        UUID id = UUID.randomUUID();
        var delivery = new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "70000000", "EFECTIVO");
        checkout.execute(new SimulatedCheckoutRequest(id, user, List.of(new Item(product, 1)), delivery));
        var altered = List.of(
            new SimulatedCheckoutRequest.Delivery("Plaza de Comidas", "Juan", "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "María", "70000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "60000000", "EFECTIVO"),
            new SimulatedCheckoutRequest.Delivery("Comedor Central", "Juan", "70000000", "QR_SIMULADO")
        );
        for (var d : altered) {
            var request = new SimulatedCheckoutRequest(id, user, List.of(new Item(product, 1)), d);
            assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request)).getStatus());
        }
        assertEquals(409, assertThrows(CheckoutException.class, () -> checkout.execute(request(id, 1))).getStatus());
        assertEquals(4, publications.findById(product).orElseThrow().getStock());
    }
}
