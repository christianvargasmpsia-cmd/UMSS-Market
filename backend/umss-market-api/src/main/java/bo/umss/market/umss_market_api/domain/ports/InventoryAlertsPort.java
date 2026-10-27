package bo.umss.market.umss_market_api.domain.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Puerto de lectura: las alertas no modifican reservas ni stock. */
public interface InventoryAlertsPort {
    record StockItem(UUID publicationId, String nombre, int stock, LocalDateTime createdAt) {}
    record StockPage(List<StockItem> items, long totalElements, int page, int size, int totalPages) {}
    record StockSummary(long productosCriticos, long agotados, long stockBajo) {}

    StockPage findLowStock(UUID storeId, int threshold, int page, int size);
    StockSummary summarize(UUID storeId, int threshold);
}
