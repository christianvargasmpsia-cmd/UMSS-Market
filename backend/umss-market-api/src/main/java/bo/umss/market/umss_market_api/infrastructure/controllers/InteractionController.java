package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.umss.market.umss_market_api.application.dto.CreateInteractionRequest;
import bo.umss.market.umss_market_api.application.dto.InteractionResponse;
import bo.umss.market.umss_market_api.application.usecases.CreateInteractionUseCase;
import bo.umss.market.umss_market_api.application.usecases.DeleteInteractionUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetInteractionByIdUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetUserInteractionsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Interacciones", description = "Historial privado de interacciones del usuario autenticado")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class InteractionController {

    private final CreateInteractionUseCase createInteractionUseCase;

    private final GetUserInteractionsUseCase getUserInteractionsUseCase;

    private final GetInteractionByIdUseCase getInteractionByIdUseCase;

    private final DeleteInteractionUseCase deleteInteractionUseCase;

    /**
     * Crear una interacción para el usuario autenticado.
     */
    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar una interacción propia",
            description = "Asigna el usuario del JWT. Tipos: VIEW, LIKE, COMMENT, FAVORITE y PURCHASE. "
                    + "El tipo PURCHASE es una señal declarada, no verifica una compra efectiva.")
    public ResponseEntity<InteractionResponse> create(
            @Valid @RequestBody CreateInteractionRequest request,
            @AuthenticationPrincipal UUID userId) {

        InteractionResponse response =
                createInteractionUseCase.execute(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Obtener todas las interacciones
     * del usuario autenticado.
     */
    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Listar las interacciones del usuario autenticado",
            description = "Devuelve únicamente el historial propio identificado por el principal JWT.")
    public ResponseEntity<List<InteractionResponse>> findAll(
            @AuthenticationPrincipal UUID userId) {

        List<InteractionResponse> response =
                getUserInteractionsUseCase.execute(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener una interacción por ID.
     */
    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar una interacción propia",
            description = "Una interacción ajena o inexistente devuelve 404.")
    public ResponseEntity<InteractionResponse> findById(
            @PathVariable UUID id, @AuthenticationPrincipal UUID userId) {

        InteractionResponse response =
                getInteractionByIdUseCase.execute(userId, id);

        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar una interacción.
     */
    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Eliminar una interacción propia",
            description = "Una interacción ajena o inexistente devuelve 404; eliminación exitosa devuelve 204.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, @AuthenticationPrincipal UUID userId) {

        deleteInteractionUseCase.execute(userId, id);

        return ResponseEntity
                .noContent()
                .build();
    }

}
