package bo.umss.market.e3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.ProductReviewsUseCase;
import bo.umss.market.umss_market_api.application.usecases.SummarizeProductReviewsUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/e3/reviews/{publicationId}")
public class E3InternalReviewsController {

    private final ProductReviewsUseCase reviews;
    private final SummarizeProductReviewsUseCase summary;

    public record CreateCommand(
        UUID buyerId,
        ReviewRequest review
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
        @PathVariable UUID publicationId,
        @RequestBody CreateCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                "Solicitud invalida"
            );
        }

        return reviews.create(
            publicationId,
            command.buyerId(),
            command.review()
        );
    }

    @GetMapping
    public List<ReviewResponse> list(
        @PathVariable UUID publicationId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return reviews.list(publicationId, limit);
    }

    @GetMapping("/summary")
    public ReviewSummaryResponse summarize(
        @PathVariable UUID publicationId
    ) {
        return summary.execute(publicationId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest() {
        return Map.of(
            "message",
            "Solicitud de resena invalida"
        );
    }
}
