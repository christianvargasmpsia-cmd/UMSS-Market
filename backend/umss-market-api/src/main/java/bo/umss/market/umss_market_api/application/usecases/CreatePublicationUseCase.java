
package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.application.dto.CreatePublicationRequest;
import bo.umss.market.umss_market_api.application.dto.CreatePublicationResponse;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.StoreRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatePublicationUseCase {

    private final PublicationRepositoryPort publicationRepository;
    private final StoreRepositoryPort storeRepository;

    public CreatePublicationResponse execute(
            CreatePublicationRequest request,
            UUID authenticatedUserId) {

        if (authenticatedUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Tienda no encontrada"));

        // Verificar que la tienda pertenezca al usuario autenticado.
        if (!authenticatedUserId.equals(store.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para crear publicaciones en esta tienda");
        }

        if (store.getStatus() == null
                || !store.getStatus().name().equals("ACTIVE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La tienda no está activa");
        }

        if (request.getTipo() == PublicationType.PRODUCTO) {
            if (request.getStock() == null || request.getStock() < 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Los productos deben tener stock mayor a cero");
            }
        }

        if (request.getTipo() == PublicationType.SERVICIO) {
            if (request.getModalidadCobro() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Los servicios requieren modalidad de cobro");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Publication publication = Publication.builder()
                .id(UUID.randomUUID())
                .storeId(store.getId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .tipo(request.getTipo())
                .stock(request.getStock())
                .modalidadCobro(request.getModalidadCobro())
                .activa(true)
                .createdAt(now)
                .updatedAt(now)
                .embedding(null)
                .build();

        Publication saved = publicationRepository.save(publication);

        return CreatePublicationResponse.builder()
                .success(true)
                .message("Publicación creada correctamente")
                .publicationId(saved.getId())
                .build();
    }
}
