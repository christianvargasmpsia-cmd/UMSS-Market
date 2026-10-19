package bo.umss.market.umss_market_api.application.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record RecommendationsResponse(boolean personalized, List<Item> items, String explanation, String aiStatus) {
    public record Item(UUID id, String name, BigDecimal price, Integer stock, UUID storeId, double affinity) {}
}
