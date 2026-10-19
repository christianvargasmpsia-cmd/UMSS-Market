package bo.umss.market.umss_market_api.application.dto;
import java.util.*;
public record ReviewSummaryResponse(UUID publicationId, int reviewCount, int consideredReviewCount, int sampleLimit, String status,
        String summary, List<Aspect> positiveAspects, List<Aspect> negativeAspects) {
    public record Aspect(UUID reviewId, String quote) {}
}
