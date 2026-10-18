package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.CatalogQueryPort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@GraphQlTest(GraphqlCatalogController.class)
class GraphqlCatalogControllerTest {
    @Autowired GraphQlTester tester;
    @MockitoBean CatalogQueryPort catalogo;

    @Test void catalogoConsultaDatosRealesDelPuertoYResuelveTiendasEnLote() {
        UUID id = UUID.randomUUID(), tiendaId = UUID.randomUUID();
        Publication p = Publication.builder().id(id).storeId(tiendaId).nombre("Cuaderno")
                .precio(new BigDecimal("25.50")).tipo(PublicationType.PRODUCTO)
                .stock(8).activa(true).build();
        Store t = Store.builder().id(tiendaId).nombre("Papeleria UMSS").categoria("Libreria").build();
        when(catalogo.buscar(any(CatalogQuery.class))).thenReturn(new CatalogPage<>(List.of(p), 1, 1, 10, 1));
        when(catalogo.tiendasPorIds(eq(Set.of(tiendaId)))).thenReturn(List.of(t));
        tester.document("""
                query { catalogo { total items { nombre precio disponible tienda { nombre } } } }
                """).execute()
                .path("catalogo.total").entity(Integer.class).isEqualTo(1)
                .path("catalogo.items[0].precio").entity(String.class).isEqualTo("25.50")
                .path("catalogo.items[0].tienda.nombre").entity(String.class).isEqualTo("Papeleria UMSS");
        verify(catalogo).tiendasPorIds(Set.of(tiendaId));
    }

    @Test void limitaPaginacionInvalida() {
        tester.document("query { catalogo(limite: 500) { total } }").execute()
                .errors().expect(error -> error.getMessage().contains("limite"));
        verifyNoInteractions(catalogo);
    }

    @Test void detalleNoExponePublicacionInexistente() {
        UUID id = UUID.randomUUID();
        when(catalogo.detallePublico(id)).thenReturn(Optional.empty());
        tester.document("query ($id: ID!) { producto(id: $id) { nombre } }")
                .variable("id", id.toString()).execute()
                .path("producto").valueIsNull();
    }

    @Test void rechazaRangoDePreciosInvertido() {
        tester.document("""
                query { catalogo(filtro: {precioMin: "90", precioMax: "10"}) { total } }
                """).execute().errors().expect(error -> error.getMessage().contains("precioMin"));
        verifyNoInteractions(catalogo);
    }
}
