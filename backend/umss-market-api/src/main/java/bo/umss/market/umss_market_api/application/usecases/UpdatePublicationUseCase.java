package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.UpdatePublicationRequest;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdatePublicationUseCase {

    private final PublicationRepositoryPort publicationRepository;

    @Transactional
    public Publication execute(
            UUID id,
            UpdatePublicationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        if (request.getStock() != null && request.getStock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        // Bloquear la fila antes de leer su stock evita sobrescribir reservas
        // concurrentes al guardar otros campos (precio, nombre, etc.).
        Publication publication =
                publicationRepository.findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Publicación no encontrada"));

        if (request.getNombre() != null) {
            publication.setNombre(
                    request.getNombre());
        }

        if (request.getDescripcion() != null) {
            publication.setDescripcion(
                    request.getDescripcion());
        }

        if (request.getPrecio() != null) {
            publication.setPrecio(
                    request.getPrecio());
        }

        if (request.getTipo() != null) {
            publication.setTipo(
                    request.getTipo());
        }

        if (request.getStock() != null) {
            publication.setStock(
                    request.getStock());
        }

        if (request.getModalidadCobro() != null) {
            publication.setModalidadCobro(
                    request.getModalidadCobro());
        }

        publication.setUpdatedAt(
                LocalDateTime.now());

        return publicationRepository.save(
                publication);
    }
}