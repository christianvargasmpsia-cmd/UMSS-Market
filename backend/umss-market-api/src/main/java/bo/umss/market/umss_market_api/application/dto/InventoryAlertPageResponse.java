package bo.umss.market.umss_market_api.application.dto;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/** Contrato REST estable e independiente de Page<T> de Spring Data. */
public record InventoryAlertPageResponse(
        UUID storeId, int threshold, int page, int size,
        long totalElements, int totalPages, List<InventoryAlertItem> items) {
    @Schema(name = "InventoryAlertItem", description = "Producto con inventario critico")
    public record InventoryAlertItem(UUID publicationId, String nombre, int stock, String estado) {}
}
