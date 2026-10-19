package bo.umss.market.umss_market_api.domain.model;
import java.time.Instant;
import java.util.UUID;
public record ProductReview(UUID id, UUID publicationId, UUID buyerId, Integer rating, String comment, Instant createdAt) {}
