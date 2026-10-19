package bo.umss.market.umss_market_api.infrastructure.controllers;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.DetectDuplicatePublicationsUseCase;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/stores/{storeId}/publications/duplicates")
public class DuplicatePublicationsController {
    private final DetectDuplicatePublicationsUseCase detection;
    @PostMapping public DuplicateCheckResponse detect(@PathVariable UUID storeId,
            @AuthenticationPrincipal UUID userId,@RequestBody DuplicateCheckRequest request) {
        return detection.execute(userId,storeId,request);
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalidBody() {return Map.of("message","Solicitud de duplicados invalida");}
}
