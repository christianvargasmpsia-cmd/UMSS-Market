package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ProductReviewEntity;
public interface JpaReviewRepository extends JpaRepository<ProductReviewEntity,UUID> {
    List<ProductReviewEntity> findByPublicationIdOrderByCreatedAtDescIdDesc(UUID publicationId, Pageable page);
}
