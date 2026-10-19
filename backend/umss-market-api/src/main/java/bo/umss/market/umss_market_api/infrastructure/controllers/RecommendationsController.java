package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import bo.umss.market.umss_market_api.application.dto.RecommendationsResponse;
import bo.umss.market.umss_market_api.application.usecases.GetRecommendationsUseCase;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationsController {
    private final GetRecommendationsUseCase recommendations;
    @GetMapping
    public RecommendationsResponse get(@AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendations.execute(userId, limit);
    }
}
