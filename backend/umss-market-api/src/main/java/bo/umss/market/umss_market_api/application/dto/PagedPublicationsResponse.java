package bo.umss.market.umss_market_api.application.dto;

import java.util.List;

/** Pagina REST: page base cero, totalElements independiente de items.size. */
public record PagedPublicationsResponse(
        List<PagedPublicationItem> items, int page, int size,
        long totalElements, int totalPages) { }
