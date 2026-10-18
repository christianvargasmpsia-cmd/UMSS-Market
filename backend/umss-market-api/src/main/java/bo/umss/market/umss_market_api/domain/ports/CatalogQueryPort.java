package bo.umss.market.umss_market_api.domain.ports;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
public interface CatalogQueryPort {
    CatalogPage<Publication> buscar(CatalogQuery filtro);
    Optional<Publication> detallePublico(UUID id);
    CatalogPage<Store> tiendas(String categoria, int pagina, int limite);
    List<String> categorias();
    List<Store> tiendasPorIds(Set<UUID> ids);
}
