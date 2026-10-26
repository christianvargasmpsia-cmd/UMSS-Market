package bo.umss.market.umss_market_api.application.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record RecommendationsResponse(
        @io.swagger.v3.oas.annotations.media.Schema(description = "Existe historial propio útil para calcular afinidad")
        boolean personalized, List<Item> items,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Explicación opcional de IA o texto determinista de fallback")
        String explanation,
        @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"GENERATED", "FALLBACK", "NOT_REQUESTED"})
        String aiStatus) {
    @io.swagger.v3.oas.annotations.media.Schema(name = "RecommendationItem", description = "Publicación disponible y su afinidad; affinity no es una probabilidad")
    public record Item(UUID id, String name, BigDecimal price, Integer stock, UUID storeId, double affinity) {}
}
