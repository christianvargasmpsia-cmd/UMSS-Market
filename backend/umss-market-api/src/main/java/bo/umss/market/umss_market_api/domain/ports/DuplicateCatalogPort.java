package bo.umss.market.umss_market_api.domain.ports;
import java.util.*;
public interface DuplicateCatalogPort {
    record Candidate(UUID id, String title, String description) {}
    List<Candidate> findByStore(UUID storeId);
}
