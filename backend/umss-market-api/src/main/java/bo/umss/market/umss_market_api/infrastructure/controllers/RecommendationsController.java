package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import bo.umss.market.umss_market_api.application.dto.RecommendationsResponse;
import bo.umss.market.umss_market_api.application.usecases.GetRecommendationsUseCase;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Recomendaciones", description = "Recomendaciones propias del comprador autenticado")
public class RecommendationsController {
    private final GetRecommendationsUseCase recommendations;
    @GetMapping
    @Operation(summary = "Recomendar publicaciones al comprador autenticado",
            description = "Ordena por afinidad con las últimas 30 interacciones propias. Excluye publicaciones "
                    + "vistas en ese historial, inactivas y sin stock. Sin historial útil devuelve catálogo general. "
                    + "Ollama solo explica la selección; su falla no altera el ranking ni impide HTTP 200.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "Recomendaciones personalizadas o generales; items puede estar vacío"),
                @ApiResponse(responseCode = "400", description = "limit no entero o fuera de 1..50", content = @Content),
                @ApiResponse(responseCode = "401", description = "JWT inválido o vencido", content = @Content),
                @ApiResponse(responseCode = "403", description = "Usuario no autenticado", content = @Content)
            })
    public RecommendationsResponse get(@AuthenticationPrincipal UUID userId,
            @Parameter(description = "Máximo de publicaciones devueltas; puede haber menos candidatos",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "10"))
            @RequestParam(defaultValue = "10") int limit) {
        return recommendations.execute(userId, limit);
    }
}
