package bo.umss.market.umss_market_api.application.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import bo.umss.market.umss_market_api.domain.exceptions.InsufficientStockException;
import lombok.RequiredArgsConstructor;

/** Operaciones de stock; deben invocarse dentro de una transaccion Spring. */
@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final PublicationRepositoryPort publications;

    /** @return true si se desconto stock, false para servicios sin inventario. */
    public boolean reserve(Publication publication, int quantity) {
        if (publication == null || publication.getId() == null || quantity <= 0) {
            throw new IllegalArgumentException("Publicacion o cantidad invalida");
        }
        if (!Boolean.TRUE.equals(publication.getActiva())) {
            throw new IllegalStateException("La publicacion no esta activa");
        }
        if (publication.getStock() == null) {
            if (publication.getTipo() == PublicationType.SERVICIO) {
                return false; // El servicio no tiene unidades fisicas.
            }
            throw new IllegalStateException("Producto sin stock configurado: " + publication.getId());
        }
        if (!publications.reserveIfAvailable(publication.getId(), quantity)) {
            throw new InsufficientStockException("Stock insuficiente para: " + publication.getNombre());
        }
        return true;
    }

    public void release(UUID publicationId, int quantity, boolean wasReserved) {
        if (wasReserved) {
            publications.returnReservedStock(publicationId, quantity);
        }
    }
}
