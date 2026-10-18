package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.util.UUID;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.Publication;
public record ProductoGraphql(UUID id, UUID tiendaId, String nombre, String descripcion,
                              String precio, PublicationType tipo, Integer stock,
                              boolean disponible) {
    public static ProductoGraphql from(Publication p) {
        return new ProductoGraphql(p.getId(), p.getStoreId(), p.getNombre(), p.getDescripcion(),
                p.getPrecio().toPlainString(), p.getTipo(), p.getStock(),
                p.getTipo() == PublicationType.SERVICIO || (p.getStock() != null && p.getStock() > 0));
    }
}
