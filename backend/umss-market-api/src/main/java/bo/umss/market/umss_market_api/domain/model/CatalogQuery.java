package bo.umss.market.umss_market_api.domain.model;
import java.math.BigDecimal;
import java.util.UUID;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
public record CatalogQuery(
        String texto, PublicationType tipo, BigDecimal precioMin, BigDecimal precioMax,
        UUID tiendaId, String categoria, Boolean disponible, int pagina, int limite) { }
