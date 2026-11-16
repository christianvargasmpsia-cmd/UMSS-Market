package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.SimulatedCheckoutUseCase;
import bo.umss.market.umss_market_api.domain.exceptions.CheckoutException;

@RestController
@RequestMapping("/api/orders/checkout/simulated")
@RequiredArgsConstructor
public class SimulatedCheckoutController {
    private final SimulatedCheckoutUseCase checkout;

    @PostMapping
    public OrderResponse purchase(@AuthenticationPrincipal java.util.UUID authenticatedUserId,
                                  @RequestBody SimulatedCheckoutRequest request) {
        if (authenticatedUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
        }
        if (request == null) {
            return checkout.execute(null);
        }
        var trustedRequest = new SimulatedCheckoutRequest(request.requestId(), authenticatedUserId,
                request.items(), request.delivery());
        return checkout.execute(trustedRequest);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, Object>> rejected(CheckoutException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("success", false, "message", ex.getMessage()));
    }
}
