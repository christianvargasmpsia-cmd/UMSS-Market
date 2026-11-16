package bo.umss.market.umss_market_api.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.application.usecases.UpdateOrderStatusUseCase;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.model.User;
import bo.umss.market.umss_market_api.domain.ports.*;

class StoreReceivedOrdersControllerTest {

    private StoreRepositoryPort stores;
    private UserRepositoryPort users;
    private OrderRepositoryPort orders;
    private OrderItemRepositoryPort items;
    private PublicationRepositoryPort publications;
    private UpdateOrderStatusUseCase changeStatus;

    private StoreReceivedOrdersController controller;

    @BeforeEach
    void setUp() {
        stores = mock(StoreRepositoryPort.class);
        users = mock(UserRepositoryPort.class);
        orders = mock(OrderRepositoryPort.class);
        items = mock(OrderItemRepositoryPort.class);
        publications = mock(PublicationRepositoryPort.class);
        changeStatus = mock(UpdateOrderStatusUseCase.class);

        controller = new StoreReceivedOrdersController(
                stores, users, orders, items,
                publications, changeStatus
        );
    }

    @Test
    void rejectsMissingAuthenticatedPrincipal() {
        UUID storeId = UUID.randomUUID();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(storeId, null, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(users, stores, orders);
    }

    @Test
    void rejectsUnknownAuthenticatedUser() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(users.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(storeId, userId, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(stores, orders);
    }

    @Test
    void rejectsBuyerAccessToStoreOrders() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User buyer = mock(User.class);
        when(buyer.getRole()).thenReturn(Role.COMPRADOR);
        when(users.findById(userId)).thenReturn(Optional.of(buyer));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(storeId, userId, null)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(stores, orders);
    }

    @Test
    void rejectsAccessToAnotherEntrepreneurStore() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        User entrepreneur = mock(User.class);
        when(entrepreneur.getRole()).thenReturn(Role.EMPRENDEDOR);
        when(users.findById(userId))
                .thenReturn(Optional.of(entrepreneur));

        Store store = mock(Store.class);
        when(store.getUserId()).thenReturn(ownerId);
        when(stores.findById(storeId))
                .thenReturn(Optional.of(store));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(storeId, userId, null)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(orders);
    }

    @Test
    void returns404WhenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User entrepreneur = mock(User.class);
        when(entrepreneur.getRole()).thenReturn(Role.EMPRENDEDOR);
        when(users.findById(userId))
                .thenReturn(Optional.of(entrepreneur));
        when(stores.findById(storeId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(storeId, userId, null)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(orders);
    }

    @Test
    void listsOnlyItemsBelongingToStore() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownPublicationId = UUID.randomUUID();
        UUID otherPublicationId = UUID.randomUUID();

        User entrepreneur = mock(User.class);
        when(entrepreneur.getRole()).thenReturn(Role.EMPRENDEDOR);
        when(users.findById(ownerId))
                .thenReturn(Optional.of(entrepreneur));

        Store store = mock(Store.class);
        when(store.getUserId()).thenReturn(ownerId);
        when(stores.findById(storeId))
                .thenReturn(Optional.of(store));

        Order order = Order.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDIENTE)
                .build();

        OrderItem ownItem = OrderItem.builder()
                .publicationId(ownPublicationId)
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .subtotal(new BigDecimal("50.00"))
                .build();

        OrderItem otherItem = OrderItem.builder()
                .publicationId(otherPublicationId)
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("100.00"))
                .build();

        Publication ownPublication = Publication.builder()
                .id(ownPublicationId)
                .storeId(storeId)
                .nombre("Cuaderno")
                .build();

        Publication otherPublication = Publication.builder()
                .id(otherPublicationId)
                .storeId(otherStoreId)
                .nombre("Mochila")
                .build();

        when(orders.findAll()).thenReturn(List.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(ownItem, otherItem));
        when(publications.findById(ownPublicationId))
                .thenReturn(Optional.of(ownPublication));
        when(publications.findById(otherPublicationId))
                .thenReturn(Optional.of(otherPublication));

        var result = controller.list(storeId, ownerId, null);

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).orderId());
        assertEquals(1, result.get(0).items().size());
        assertEquals("Cuaderno", result.get(0).items().get(0).productName());
        assertEquals(0, new BigDecimal("50.00")
                .compareTo(result.get(0).storeSubtotal()));
    }

    @Test
    void returnsOrderDetailWithOnlyStoreItems() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID publicationId = UUID.randomUUID();

        User entrepreneur = mock(User.class);
        when(entrepreneur.getRole()).thenReturn(Role.EMPRENDEDOR);
        when(users.findById(ownerId))
                .thenReturn(Optional.of(entrepreneur));

        Store store = mock(Store.class);
        when(store.getUserId()).thenReturn(ownerId);
        when(stores.findById(storeId))
                .thenReturn(Optional.of(store));

        Order order = Order.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .status(OrderStatus.CONFIRMADO)
                .build();

        OrderItem item = OrderItem.builder()
                .publicationId(publicationId)
                .quantity(3)
                .unitPrice(new BigDecimal("20.00"))
                .subtotal(new BigDecimal("60.00"))
                .build();

        Publication publication = Publication.builder()
                .id(publicationId)
                .storeId(storeId)
                .nombre("Lapicero")
                .build();

        when(orders.findById(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(item));
        when(publications.findById(publicationId))
                .thenReturn(Optional.of(publication));

        var result = controller.detail(storeId, orderId, ownerId);

        assertEquals(orderId, result.orderId());
        assertEquals(OrderStatus.CONFIRMADO, result.status());
        assertEquals(1, result.items().size());
        assertEquals("Lapicero", result.items().get(0).productName());
        assertEquals(0, new BigDecimal("60.00")
                .compareTo(result.storeSubtotal()));
    }

    private void authorizeOwner(UUID storeId, UUID ownerId) {
        User entrepreneur = mock(User.class);
        when(entrepreneur.getRole()).thenReturn(Role.EMPRENDEDOR);
        when(users.findById(ownerId))
                .thenReturn(Optional.of(entrepreneur));

        Store store = mock(Store.class);
        when(store.getUserId()).thenReturn(ownerId);
        when(stores.findById(storeId))
                .thenReturn(Optional.of(store));
    }

    @Test
    void confirmsPendingOrder() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID publicationId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDIENTE)
                .build();

        Order confirmed = Order.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMADO)
                .build();

        OrderItem item = OrderItem.builder()
                .publicationId(publicationId)
                .quantity(1)
                .unitPrice(new BigDecimal("25.00"))
                .subtotal(new BigDecimal("25.00"))
                .build();

        Publication publication = Publication.builder()
                .id(publicationId)
                .storeId(storeId)
                .nombre("Cuaderno")
                .build();

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(item));
        when(publications.findById(publicationId))
                .thenReturn(Optional.of(publication));

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMADO);

        when(changeStatus.execute(orderId, request))
                .thenReturn(confirmed);

        var result = controller.status(
                storeId, orderId, ownerId, request);

        assertEquals(OrderStatus.CONFIRMADO, result.status());
        assertEquals(orderId, result.orderId());
        verify(changeStatus).execute(orderId, request);
    }

    @Test
    void returns404WhenUpdatingNonexistentOrder() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.empty());

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMADO);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(
                        storeId, orderId, ownerId, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(changeStatus);
    }

    @Test
    void rejectsMissingOrderStatus() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID publicationId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDIENTE)
                .build();

        OrderItem item = OrderItem.builder()
                .publicationId(publicationId)
                .build();

        Publication publication = Publication.builder()
                .id(publicationId)
                .storeId(storeId)
                .build();

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(item));
        when(publications.findById(publicationId))
                .thenReturn(Optional.of(publication));

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(
                        storeId, orderId, ownerId, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(changeStatus);
    }

    @Test
    void rejectsMultistoreOrderStatusUpdate() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownPublicationId = UUID.randomUUID();
        UUID otherPublicationId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDIENTE)
                .build();

        OrderItem ownItem = OrderItem.builder()
                .publicationId(ownPublicationId)
                .build();

        OrderItem otherItem = OrderItem.builder()
                .publicationId(otherPublicationId)
                .build();

        Publication ownPublication = Publication.builder()
                .id(ownPublicationId)
                .storeId(storeId)
                .build();

        Publication otherPublication = Publication.builder()
                .id(otherPublicationId)
                .storeId(otherStoreId)
                .build();

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(ownItem, otherItem));
        when(publications.findById(ownPublicationId))
                .thenReturn(Optional.of(ownPublication));
        when(publications.findById(otherPublicationId))
                .thenReturn(Optional.of(otherPublication));

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMADO);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(
                        storeId, orderId, ownerId, request)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(changeStatus);
        assertEquals(OrderStatus.PENDIENTE, order.getStatus());
    }

    @Test
    void rejectsInvalidOrderTransition() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID publicationId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMADO)
                .build();

        OrderItem item = OrderItem.builder()
                .publicationId(publicationId)
                .build();

        Publication publication = Publication.builder()
                .id(publicationId)
                .storeId(storeId)
                .build();

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of(item));
        when(publications.findById(publicationId))
                .thenReturn(Optional.of(publication));

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.PENDIENTE);

        when(changeStatus.execute(orderId, request))
                .thenThrow(new bo.umss.market.umss_market_api.domain.exceptions.InvalidOrderStateException(
                        "Transicion de estado no permitida"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(storeId, orderId, ownerId, request)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(changeStatus).execute(orderId, request);
    }

    @Test
    void rejectsOrderWithoutItems() {
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        authorizeOwner(storeId, ownerId);

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDIENTE)
                .build();

        when(orders.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId))
                .thenReturn(List.of());

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMADO);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.status(storeId, orderId, ownerId, request)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(changeStatus);
    }
}