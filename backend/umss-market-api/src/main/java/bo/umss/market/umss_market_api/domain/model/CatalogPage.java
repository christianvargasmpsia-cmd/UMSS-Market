package bo.umss.market.umss_market_api.domain.model;
import java.util.List;
public record CatalogPage<T>(List<T> items, long total, int pagina, int limite, int paginas) { }
