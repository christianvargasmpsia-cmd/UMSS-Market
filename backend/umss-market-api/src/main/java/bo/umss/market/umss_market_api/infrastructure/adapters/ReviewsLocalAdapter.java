package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import bo.umss.market.umss_market_api.application.ports.ReviewsPort;
import bo.umss.market.umss_market_api.application.dto.ReviewRequest;
import bo.umss.market.umss_market_api.application.dto.ReviewResponse;
import bo.umss.market.umss_market_api.application.dto.ReviewSummaryResponse;
import bo.umss.market.umss_market_api.application.usecases.ProductReviewsUseCase;
import bo.umss.market.umss_market_api.application.usecases.SummarizeProductReviewsUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "e3.reviews",
    name = "mode",
    havingValue = "local",
    matchIfMissing = true
)
public class ReviewsLocalAdapter implements ReviewsPort {

    private final ProductReviewsUseCase reviews;
    private final SummarizeProductReviewsUseCase summary;

    @Override
    public ReviewResponse create(
        UUID publicationId,
        UUID buyerId,
        ReviewRequest request
    ) {
        return reviews.create(publicationId, buyerId, request);
    }

    @Override
    public List<ReviewResponse> list(UUID publicationId, int limit) {
        return reviews.list(publicationId, limit);
    }

    @Override
    public ReviewSummaryResponse summarize(UUID publicationId) {
        return summary.execute(publicationId);
    }
}