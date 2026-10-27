package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import bo.umss.market.umss_market_api.application.dto.InventoryAlertPageResponse;
import bo.umss.market.umss_market_api.application.dto.InventoryAlertSummaryResponse;
import bo.umss.market.umss_market_api.application.usecases.GetInventoryAlertsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stores/{storeId}/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InventoryAlertsController {
    private final GetInventoryAlertsUseCase alerts;

    @GetMapping("/low-stock")
    @Operation(operationId = "listLowStock", summary = "Consultar publicaciones activas con stock critico de mi tienda")
    @ApiResponse(responseCode = "200", description = "Listado paginado de productos con stock igual o inferior al umbral")
    @ApiResponse(responseCode = "400", description = "Parametros invalidos")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado a esta tienda")
    @ApiResponse(responseCode = "404", description = "Tienda no encontrada")
    public InventoryAlertPageResponse lowStock(@PathVariable UUID storeId, @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return alerts.list(userId, storeId, threshold, page, size);
    }

    @GetMapping("/summary")
    @Operation(operationId = "summarizeInventoryAlerts", summary = "Resumen de alertas de stock de mi tienda")
    @ApiResponse(responseCode = "200", description = "Cantidad de productos agotados y con stock bajo")
    @ApiResponse(responseCode = "400", description = "Umbral invalido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado a esta tienda")
    @ApiResponse(responseCode = "404", description = "Tienda no encontrada")
    public InventoryAlertSummaryResponse summary(@PathVariable UUID storeId, @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "5") int threshold) {
        return alerts.summary(userId, storeId, threshold);
    }
}
