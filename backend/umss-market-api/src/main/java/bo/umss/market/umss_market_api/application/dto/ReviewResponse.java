package bo.umss.market.umss_market_api.application.dto;
import java.time.Instant;
import java.util.UUID;
import bo.umss.market.umss_market_api.domain.model.ProductReview;
public record ReviewResponse(UUID id, UUID publicationId, int rating, String comment, Instant createdAt) {
    public static ReviewResponse from(ProductReview r) {
        return new ReviewResponse(r.id(), r.publicationId(), r.rating(), r.comment(), r.createdAt());
    }
}
