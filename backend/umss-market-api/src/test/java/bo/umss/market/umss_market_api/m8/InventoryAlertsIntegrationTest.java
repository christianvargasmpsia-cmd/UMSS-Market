package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.StoreEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.UserEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaStoreRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaUserRepository;
import bo.umss.market.umss_market_api.infrastructure.security.JwtService;

@SpringBootTest(properties = {"ia.enabled=false", "ai.monitoring.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class InventoryAlertsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JpaPublicationRepository publications;
    @Autowired JpaStoreRepository stores;
    @Autowired JpaUserRepository users;
    @Autowired JwtService jwt;
    UUID owner, foreignUser, shop, otherShop;

    String token(UUID user, String role) {
        return "Bearer " + jwt.generateToken(user, user + "@example.invalid", role);
    }
    String lowStockUrl(UUID shopId) { return "/api/stores/" + shopId + "/inventory/low-stock"; }
    String summaryUrl(UUID shopId) { return "/api/stores/" + shopId + "/inventory/summary"; }

    @BeforeEach void prepare() {
        owner = UUID.randomUUID(); foreignUser = UUID.randomUUID();
        shop = UUID.randomUUID(); otherShop = UUID.randomUUID();
        for (UUID uid : new UUID[] {owner, foreignUser}) {
            users.saveAndFlush(UserEntity.builder().id(uid).ru(uid.toString())
                    .nombre("Emprendedor").email(uid + "@example.invalid")
                    .passwordHash("test-hash").role(Role.EMPRENDEDOR).build());
        }
        stores.saveAndFlush(StoreEntity.builder().id(shop).userId(owner).nombre("Mi tienda").build());
        stores.saveAndFlush(StoreEntity.builder().id(otherShop).userId(foreignUser).nombre("Otra tienda").build());
        publication(shop, "Agotado", 0, true, PublicationType.PRODUCTO, 5);
        publication(shop, "Stock 2", 2, true, PublicationType.PRODUCTO, 4);
        publication(shop, "Stock 5", 5, true, PublicationType.PRODUCTO, 3);
        publication(shop, "Stock 20", 20, true, PublicationType.PRODUCTO, 2);
        publication(shop, "Servicio sin inventario", null, true, PublicationType.SERVICIO, 1);
        publication(shop, "Producto inactivo", 0, false, PublicationType.PRODUCTO, 1);
        publication(otherShop, "Secreto otra tienda", 0, true, PublicationType.PRODUCTO, 6);
    }
    void publication(UUID shopId, String name, Integer stock, boolean active, PublicationType type, int days) {
        publications.saveAndFlush(PublicationEntity.builder().id(UUID.randomUUID()).storeId(shopId)
                .nombre(name).descripcion("Demo 2").precio(BigDecimal.TEN)
                .tipo(type).stock(stock).activa(active)
                .createdAt(LocalDateTime.of(2026, 10, 26, 12, 0).minusDays(days)).build());
    }

    @Test void readOnlyInventoryListDoesNotRequestPessimisticLock() throws Exception {
        // H2 no reproduce siempre la restriccion de PostgreSQL sobre FOR NO KEY UPDATE
        // en una transaccion readOnly; verificamos la configuracion del repositorio.
        var method = JpaPublicationRepository.class.getMethod("findLowStock",
                UUID.class, PublicationType.class, int.class, Pageable.class);
        assertNull(method.getAnnotation(Lock.class),
                "El listado de alertas es de solo lectura y no debe bloquear filas");
    }

    @Test void listsOnlyOwnActivePhysicalProductsAndPaginates() throws Exception {
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("threshold", "5").param("page", "0").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].nombre").value("Stock 5"))
                .andExpect(jsonPath("$.items[1].nombre").value("Stock 2"));
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("threshold", "5").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].nombre").value("Agotado"))
                .andExpect(jsonPath("$.items[0].estado").value("AGOTADO"));
    }

    @Test void summariesCountsSeparatelyAndHandlesThresholdZero() throws Exception {
        mvc.perform(get(summaryUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("threshold","5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productosCriticos").value(3))
                .andExpect(jsonPath("$.agotados").value(1))
                .andExpect(jsonPath("$.stockBajo").value(2));
        mvc.perform(get(summaryUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("threshold","0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productosCriticos").value(1))
                .andExpect(jsonPath("$.stockBajo").value(0));
    }

    @Test void rejectsOtherStoreUntrustedRoleAndInvalidQuery() throws Exception {
        mvc.perform(get(lowStockUrl(otherShop)).header("Authorization", token(owner,"EMPRENDEDOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"COMPRADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("size","0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("threshold","-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR"))
                        .param("page","-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(lowStockUrl(UUID.randomUUID())).header("Authorization", token(owner,"EMPRENDEDOR")))
                .andExpect(status().isNotFound());
    }

    @Test void authMeRespondsToAValidEntrepreneurJwt() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .header("Authorization", token(owner, "EMPRENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.toString()))
                .andExpect(jsonPath("$.role").value("EMPRENDEDOR"));
    }

    @Test void requiresJwtAndPersistsNoInventoryChanges() throws Exception {
        long count = publications.count();
        mvc.perform(get(lowStockUrl(shop))).andExpect(status().isForbidden());
        mvc.perform(get(lowStockUrl(shop)).header("Authorization", token(owner,"EMPRENDEDOR")))
                .andExpect(status().isOk());
        assertEquals(count, publications.count(), "La consulta no debe crear ni eliminar publicaciones");
        assertTrue(publications.findAll().stream().anyMatch(p -> "Stock 20".equals(p.getNombre()) && p.getStock() == 20));
    }
}
