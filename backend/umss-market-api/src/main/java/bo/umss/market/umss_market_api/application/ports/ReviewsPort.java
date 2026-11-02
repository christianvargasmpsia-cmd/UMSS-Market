package bo.umss.market.umss_market_api.application.ports;

import java.util.List;
import java.util.UUID;

import bo.umss.market.umss_market_api.application.dto.ReviewRequest;
import bo.umss.market.umss_market_api.application.dto.ReviewResponse;
import bo.umss.market.umss_market_api.application.dto.ReviewSummaryResponse;

public interface ReviewsPort {

    ReviewResponse create(
        UUID publicationId,
        UUID buyerId,
        ReviewRequest request
    );

    List<ReviewResponse> list(UUID publicationId, int limit);

    ReviewSummaryResponse summarize(UUID publicationId);
}