package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.UpdatePublicationStatusRequest;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdatePublicationStatusUseCase {

    private final PublicationRepositoryPort publicationRepository;

    @Transactional
    public Publication execute(
            UUID id,
            UpdatePublicationStatusRequest request) {

        if (request == null || request.getActiva() == null) {
            throw new IllegalArgumentException("El estado activa es obligatorio");
        }

        // El cambio de estado también guarda toda la entidad; usar el mismo
        // bloqueo impide recuperar una versión de stock desactualizada.
        Publication publication =
                publicationRepository.findByIdForUpdate(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Publicación no encontrada"));

        publication.setActiva(
                request.getActiva());

        publication.setUpdatedAt(
                LocalDateTime.now());

        return publicationRepository.save(
                publication);
    }
}