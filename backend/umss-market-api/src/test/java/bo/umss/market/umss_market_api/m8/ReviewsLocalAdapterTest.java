package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.ReviewsLocalAdapter;

class ReviewsLocalAdapterTest {

    private final ProductReviewsUseCase reviews =
        mock(ProductReviewsUseCase.class);

    private final SummarizeProductReviewsUseCase summary =
        mock(SummarizeProductReviewsUseCase.class);

    private final ReviewsLocalAdapter adapter =
        new ReviewsLocalAdapter(reviews, summary);

    @Test
    void delegatesCreate() {
        UUID id = UUID.randomUUID();
        UUID buyer = UUID.randomUUID();

        ReviewRequest request =
            new ReviewRequest(5, "Excelente");

        ReviewResponse expected =
            new ReviewResponse(
                UUID.randomUUID(),
                id,
                5,
                "Excelente",
                Instant.now()
            );

        when(reviews.create(id, buyer, request))
            .thenReturn(expected);

        assertSame(
            expected,
            adapter.create(id, buyer, request)
        );

        verify(reviews).create(id, buyer, request);
    }

    @Test
    void delegatesList() {
        UUID id = UUID.randomUUID();

        List<ReviewResponse> expected = List.of();

        when(reviews.list(id, 20))
            .thenReturn(expected);

        assertSame(expected, adapter.list(id, 20));

        verify(reviews).list(id, 20);
    }

    @Test
    void delegatesSummary() {
        UUID id = UUID.randomUUID();

        ReviewSummaryResponse expected =
            new ReviewSummaryResponse(
                id,
                0,
                0,
                10,
                "NO_REVIEWS",
                "Sin resenas",
                List.of(),
                List.of()
            );

        when(summary.execute(id))
            .thenReturn(expected);

        assertSame(expected, adapter.summarize(id));

        verify(summary).execute(id);
    }
}