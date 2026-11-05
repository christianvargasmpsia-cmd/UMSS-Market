package bo.umss.market.umss_market_api.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.CatalogQueryPort;

class PagedPublicationsControllerTest {

    private CatalogQueryPort repository;
    private PagedPublicationsController controller;

    @BeforeEach
    void setUp() {
        repository = mock(CatalogQueryPort.class);
        controller = new PagedPublicationsController(repository);
    }

    @Test
    void filtersAndConvertsPageIndexWithoutExposingEmbeddings() {
        UUID storeId = UUID.randomUUID();
        var product = Publication.builder().id(UUID.randomUUID()).storeId(storeId)
                .nombre("Brownie").descripcion("Cacao").precio(BigDecimal.TEN)
                .tipo(PublicationType.PRODUCTO).stock(8)
                .embedding("[0.1,0.3]").build();
        when(repository.buscar(any(CatalogQuery.class)))
                .thenReturn(new CatalogPage<>(List.of(product), 12, 2, 5, 3));

        var response = controller.list("  brownie  ", PublicationType.PRODUCTO,
                BigDecimal.ONE, BigDecimal.TEN, storeId, null, true, 1, 5);

        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(12, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals("Brownie", response.items().get(0).nombre());
        assertEquals(storeId, response.items().get(0).storeId());

        var captor = org.mockito.ArgumentCaptor.forClass(CatalogQuery.class);
        verify(repository).buscar(captor.capture());
        var query = captor.getValue();
        assertEquals(2, query.pagina()); // internal 1-based
        assertEquals(5, query.limite());
        assertEquals("brownie", query.texto());
        assertEquals(storeId, query.tiendaId());
        assertEquals(Boolean.TRUE, query.disponible());
        assertTrue(java.util.Arrays.stream(response.items().get(0).getClass().getRecordComponents())
                .noneMatch(c -> c.getName().equals("embedding")));
    }

    @Test
    void emptyPageRetainsTotalMetadata() {
        when(repository.buscar(any(CatalogQuery.class)))
                .thenReturn(new CatalogPage<>(List.of(), 0, 1, 10, 0));

        var result = controller.list(null, null, null, null, null, null, null, 0, 10);

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void rejectsInvalidPageAndSize() {
        assertBadRequest(() -> controller.list(null, null, null, null, null, null, null, -1, 10));
        assertBadRequest(() -> controller.list(null, null, null, null, null, null, null, 0, 0));
        assertBadRequest(() -> controller.list(null, null, null, null, null, null, null, 0, 51));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidPricesAndLongFilters() {
        assertBadRequest(() -> controller.list(null, null, BigDecimal.TEN, BigDecimal.ONE,
                null, null, null, 0, 10));
        assertBadRequest(() -> controller.list(null, null, BigDecimal.ONE.negate(), null,
                null, null, null, 0, 10));
        assertBadRequest(() -> controller.list("x".repeat(121), null, null, null,
                null, null, null, 0, 10));
        verifyNoInteractions(repository);
    }

    private static void assertBadRequest(org.junit.jupiter.api.function.Executable executable) {
        var exception = assertThrows(ResponseStatusException.class, executable);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
