package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public OrderResponse purchase(@RequestBody SimulatedCheckoutRequest request) {
        return checkout.execute(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, Object>> rejected(CheckoutException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("success", false, "message", ex.getMessage()));
    }
}
