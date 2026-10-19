package bo.umss.market.umss_market_api.domain.ports;
import java.util.List;
public interface DuplicateEmbeddingPort {
    List<List<Double>> embed(List<String> texts);
}
