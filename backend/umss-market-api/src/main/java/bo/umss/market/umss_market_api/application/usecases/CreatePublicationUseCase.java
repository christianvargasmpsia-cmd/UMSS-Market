package bo.umss.market.umss_market_api.application.usecases;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;


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
            CreatePublicationRequest request) {

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() ->
                        new RuntimeException("Tienda no encontrada"));

        if (!store.getStatus().name().equals("ACTIVE")) {
            throw new RuntimeException(
                    "La tienda no está activa");
        }

        if (request.getTipo() == PublicationType.PRODUCTO) {

            if (request.getStock() == null ||
                    request.getStock() < 1) {

                throw new RuntimeException(
                        "Los productos deben tener stock mayor a cero");
            }
        }

        if (request.getTipo() == PublicationType.SERVICIO) {

            if (request.getModalidadCobro() == null) {

                throw new RuntimeException(
                        "Los servicios requieren modalidad de cobro");
            }
        }

        // La indexacion semantica se ejecuta por separado del alta.
        Publication publication = Publication.builder()
                .id(UUID.randomUUID())
                .storeId(request.getStoreId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .tipo(request.getTipo())
                .stock(request.getStock())
                .modalidadCobro(request.getModalidadCobro())
                .activa(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .embedding(null)
                .build();

        Publication saved =
                publicationRepository.save(publication);

        return CreatePublicationResponse.builder()
                .success(true)
                .message("Publicación creada correctamente")
                .publicationId(saved.getId())
                .build();
    }
}