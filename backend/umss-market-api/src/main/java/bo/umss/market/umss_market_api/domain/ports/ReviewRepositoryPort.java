package bo.umss.market.umss_market_api.domain.ports;
import java.util.*;
import bo.umss.market.umss_market_api.domain.model.ProductReview;
public interface ReviewRepositoryPort {
    ProductReview save(ProductReview review);
    List<ProductReview> findLatestByPublicationId(UUID publicationId, int limit);
}
