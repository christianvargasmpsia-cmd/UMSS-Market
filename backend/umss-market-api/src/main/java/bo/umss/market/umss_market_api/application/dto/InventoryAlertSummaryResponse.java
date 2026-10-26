package bo.umss.market.umss_market_api.application.dto;

import java.util.UUID;

public record InventoryAlertSummaryResponse(
        UUID storeId, int threshold, long productosCriticos, long agotados, long stockBajo) {}
