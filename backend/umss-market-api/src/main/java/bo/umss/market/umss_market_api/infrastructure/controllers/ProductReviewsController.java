package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.*;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/publications/{publicationId}/reviews")
public class ProductReviewsController {
    private final ProductReviewsUseCase reviews;
    private final SummarizeProductReviewsUseCase summary;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable UUID publicationId,@AuthenticationPrincipal UUID userId,@RequestBody ReviewRequest request) {
        return reviews.create(publicationId,userId,request);
    }
    @GetMapping public List<ReviewResponse> list(@PathVariable UUID publicationId,@RequestParam(defaultValue="20") int limit) {
        return reviews.list(publicationId,limit);
    }
    @GetMapping("/summary") public ReviewSummaryResponse summarize(@PathVariable UUID publicationId) {
        return summary.execute(publicationId);
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalidBody() { return Map.of("message","Cuerpo de resena invalido"); }
}
