package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.application.dto.OrderResponse;
import bo.umss.market.umss_market_api.application.usecases.GetOrdersUseCase;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/** UC-009: historial de pedidos propio, opcionalmente filtrado por estado. */
@RestController
@RequestMapping("/api/orders/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MyOrdersController {

    private final GetOrdersUseCase getOrders;

    @GetMapping
    @Operation(summary = "Consultar mis pedidos", description = "Solo pedidos asociados al usuario del JWT; estado opcional")
    public ResponseEntity<List<OrderResponse>> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) OrderStatus status) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
        }
        return ResponseEntity.ok(getOrders.execute(userId, status));
    }
}
