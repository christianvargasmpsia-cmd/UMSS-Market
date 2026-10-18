package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.util.UUID;
import bo.umss.market.umss_market_api.domain.model.Store;
public record TiendaGraphql(UUID id, String nombre, String descripcion, String categoria) {
    public static TiendaGraphql from(Store s) {
        return new TiendaGraphql(s.getId(), s.getNombre(), s.getDescripcion(), s.getCategoria());
    }
}
