package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bo.umss.market.umss_market_api.application.dto.CatalogFilterRequest;
import bo.umss.market.umss_market_api.application.dto.CreatePublicationRequest;
import bo.umss.market.umss_market_api.application.dto.CreatePublicationResponse;
import bo.umss.market.umss_market_api.application.dto.PublicationSummaryResponse;
import bo.umss.market.umss_market_api.application.dto.UpdatePublicationRequest;
import bo.umss.market.umss_market_api.application.dto.UpdatePublicationStatusRequest;

import bo.umss.market.umss_market_api.application.usecases.CreatePublicationUseCase;
import bo.umss.market.umss_market_api.application.usecases.DeletePublicationUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetPublicationByIdUseCase;
import bo.umss.market.umss_market_api.application.usecases.SearchCatalogUseCase;
import bo.umss.market.umss_market_api.application.usecases.UpdatePublicationStatusUseCase;
import bo.umss.market.umss_market_api.application.usecases.UpdatePublicationUseCase;

import bo.umss.market.umss_market_api.domain.model.Publication;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {

    private final CreatePublicationUseCase createPublicationUseCase;

    private final SearchCatalogUseCase searchCatalogUseCase;

    private final GetPublicationByIdUseCase getPublicationByIdUseCase;

    private final UpdatePublicationUseCase updatePublicationUseCase;

    private final UpdatePublicationStatusUseCase updatePublicationStatusUseCase;

    private final DeletePublicationUseCase deletePublicationUseCase;


    // ============================================================
    // CREAR PUBLICACIÓN
    // ============================================================

    /**
     * Crear una publicación.
     *
     * El alta no requiere Ollama. El embedding se genera por separado mediante reindexacion.
     */
    @PostMapping
    public ResponseEntity<CreatePublicationResponse> createPublication(
            @RequestBody CreatePublicationRequest request) {

        CreatePublicationResponse response =
                createPublicationUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // BÚSQUEDA TRADICIONAL / CATÁLOGO
    // ============================================================

    /**
     * Buscar publicaciones utilizando filtros tradicionales.
     *
     * Parámetros disponibles:
     *
     * ?texto=mouse
     * ?tipo=PRODUCTO
     * ?precioMin=50
     * ?precioMax=100
     * ?storeId=UUID
     *
     * También permite combinar filtros.
     */
    @GetMapping
    public ResponseEntity<List<PublicationSummaryResponse>> search(
            @ModelAttribute CatalogFilterRequest filter) {

        List<PublicationSummaryResponse> publications =
                searchCatalogUseCase.execute(filter);

        return ResponseEntity.ok(publications);
    }


    // ============================================================
    // BÚSQUEDA SEMÁNTICA / RAG
    // ============================================================

    /**
     * Búsqueda semántica de publicaciones.
     *
     * Ejemplo:
     *
     * GET /api/publications/semantic?query=algo%20para%20mover%20el%20cursor
     *
     * También permite indicar la cantidad de resultados:
     *
     * GET /api/publications/semantic?query=algo%20para%20mover%20el%20cursor&topK=5
     *
     * El flujo es:
     *
     * 1. Generar embedding de la consulta.
     * 2. Comparar con los embeddings de las publicaciones.
     * 3. Calcular similitud coseno.
     * 4. Ordenar por relevancia.
     * 5. Devolver los resultados Top-K.
     */
    @GetMapping("/semantic")
    public ResponseEntity<List<PublicationSummaryResponse>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {

        List<PublicationSummaryResponse> publications =
                searchCatalogUseCase.executeSemanticSearch(
                        query,
                        topK);

        return ResponseEntity.ok(publications);
    }


    // ============================================================
    // OBTENER PUBLICACIÓN POR ID
    // ============================================================

    /**
     * Obtener una publicación por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Publication> findById(
            @PathVariable UUID id) {

        Publication publication =
                getPublicationByIdUseCase.execute(id);

        return ResponseEntity.ok(publication);
    }


    // ============================================================
    // ACTUALIZAR PUBLICACIÓN
    // ============================================================

    /**
     * Actualizar completamente una publicación.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Publication> updatePublication(
            @PathVariable UUID id,
            @RequestBody UpdatePublicationRequest request) {

        Publication publication =
                updatePublicationUseCase.execute(
                        id,
                        request);

        return ResponseEntity.ok(publication);
    }


    // ============================================================
    // CAMBIAR ESTADO
    // ============================================================

    /**
     * Activar o desactivar una publicación.
     *
     * PATCH /api/publications/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Publication> updatePublicationStatus(
            @PathVariable UUID id,
            @RequestBody UpdatePublicationStatusRequest request) {

        Publication publication =
                updatePublicationStatusUseCase.execute(
                        id,
                        request);

        return ResponseEntity.ok(publication);
    }


    // ============================================================
    // ELIMINAR PUBLICACIÓN
    // ============================================================

    /**
     * Eliminar una publicación.
     *
     * DELETE /api/publications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublication(
            @PathVariable UUID id) {

        deletePublicationUseCase.execute(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}