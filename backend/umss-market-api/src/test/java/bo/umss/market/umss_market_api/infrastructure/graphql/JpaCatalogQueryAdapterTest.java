package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.enums.StoreStatus;
import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.infrastructure.adapters.JpaCatalogQueryAdapter;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.StoreEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaStoreRepository;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:catalogotest;DB_CLOSE_DELAY=-1"
})
@Import(JpaCatalogQueryAdapter.class)
class JpaCatalogQueryAdapterTest {
    @Autowired JpaPublicationRepository publicaciones;
    @Autowired JpaStoreRepository tiendas;
    @Autowired JpaCatalogQueryAdapter catalogo;

    @Test
    void soloMuestraPublicacionesActivasEnTiendasActivas() {
        UUID activa = guardarTienda("Papeleria", "Estudio", StoreStatus.ACTIVE);
        UUID inactiva = guardarTienda("Otra", "Estudio", StoreStatus.INACTIVE);
        guardarProducto(activa, "Cuaderno A", true, 5);
        guardarProducto(activa, "Cuaderno oculto", false, 5);
        guardarProducto(inactiva, "Cuaderno B", true, 5);

        CatalogPage<Publication> res = catalogo.buscar(new CatalogQuery("Cuaderno", PublicationType.PRODUCTO,
                null, null, null, "Estudio", true, 1, 10));
        assertEquals(1, res.total());
        assertEquals("Cuaderno A", res.items().getFirst().getNombre());
        assertEquals(1, res.paginas());
    }

    @Test
    void paginaDesdeBaseDeDatosYSoloIncluyeStockDisponible() {
        UUID activa = guardarTienda("Tecnologia", "Electrónica", StoreStatus.ACTIVE);
        guardarProducto(activa, "Mouse A", true, 0);
        guardarProducto(activa, "Mouse B", true, 2);
        guardarProducto(activa, "Mouse C", true, 3);
        CatalogPage<Publication> page = catalogo.buscar(new CatalogQuery("Mouse", null,
                new BigDecimal("1"), null, null, "Electrónica", true, 1, 1));
        assertEquals(2, page.total());
        assertEquals(2, page.paginas());
        assertEquals(1, page.items().size());
    }

    private UUID guardarTienda(String nombre, String categoria, StoreStatus status) {
        UUID id = UUID.randomUUID();
        tiendas.save(StoreEntity.builder().id(id).userId(UUID.randomUUID()).nombre(nombre)
                .categoria(categoria).status(status).createdAt(LocalDateTime.now()).build());
        return id;
    }
    private void guardarProducto(UUID tiendaId, String nombre, boolean activa, int stock) {
        publicaciones.save(PublicationEntity.builder().id(UUID.randomUUID()).storeId(tiendaId)
                .nombre(nombre).descripcion("Articulo universitario").precio(new BigDecimal("30.00"))
                .tipo(PublicationType.PRODUCTO).stock(stock).activa(activa)
                .createdAt(LocalDateTime.now()).build());
    }
}
