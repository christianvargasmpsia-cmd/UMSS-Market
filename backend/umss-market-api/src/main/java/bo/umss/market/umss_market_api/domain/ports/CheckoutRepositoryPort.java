package bo.umss.market.umss_market_api.domain.ports;
import java.util.Optional;
import java.util.UUID;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.Publication;
public interface CheckoutRepositoryPort {
    boolean lockUser(UUID userId);
    Optional<Publication> lockPublication(UUID publicationId);
    boolean decreaseStock(UUID publicationId, int quantity);
    void insertOrder(Order order);
}
