package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.ports.CatalogQueryPort;
import bo.umss.market.umss_market_api.application.dto.PagedPublicationsResponse;
import bo.umss.market.umss_market_api.application.dto.PagedPublicationItem;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/** UC-010: consulta REST paginada del catalogo, reutilizando el puerto GraphQL. */
@RestController
@RequestMapping("/api/catalog/publications")
@RequiredArgsConstructor
public class PagedPublicationsController {

    private final CatalogQueryPort catalog;

    @GetMapping
    @Operation(summary = "Consultar publicaciones paginadas", description = "Paginas base cero, orden estable, filtros combinables")
    public PagedPublicationsResponse list(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) PublicationType tipo,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || page >= 100_000) {
            throw invalid("page debe estar entre 0 y 99999");
        }
        if (size < 1 || size > 50) {
            throw invalid("size debe estar entre 1 y 50");
        }
        if (precioMin != null && precioMin.signum() < 0 || precioMax != null && precioMax.signum() < 0) {
            throw invalid("El precio no puede ser negativo");
        }
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw invalid("precioMin no puede superar precioMax");
        }
        String searchText = normalize(texto, 120);
        String searchCategory = normalize(categoria, 100);

        // CatalogQuery usa paginas base uno; la API REST usa base cero.
        var query = new CatalogQuery(searchText, tipo, precioMin, precioMax,
                storeId, searchCategory, disponible, page + 1, size);
        var result = catalog.buscar(query);
        List<PagedPublicationItem> items = result.items().stream()
                .map(p -> new PagedPublicationItem(
                        p.getId(), p.getStoreId(), p.getNombre(), p.getDescripcion(),
                        p.getPrecio(), p.getTipo(), p.getStock()))
                .toList();
        return new PagedPublicationsResponse(items, page, size, result.total(), result.paginas());
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw invalid("Filtro de texto demasiado largo");
        }
        return normalized;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
