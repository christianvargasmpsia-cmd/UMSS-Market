package bo.umss.market.umss_market_api.infrastructure.adapters;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import bo.umss.market.umss_market_api.domain.model.ProductReview;
import bo.umss.market.umss_market_api.domain.ports.ReviewRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ProductReviewEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaReviewRepository;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class JpaReviewRepositoryAdapter implements ReviewRepositoryPort {
    private final JpaReviewRepository repository;
    @Override public ProductReview save(ProductReview r) {
        return toDomain(repository.saveAndFlush(ProductReviewEntity.builder().id(r.id())
                .publicationId(r.publicationId()).buyerId(r.buyerId()).rating(r.rating())
                .comment(r.comment()).createdAt(r.createdAt()).build()));
    }
    @Override public List<ProductReview> findLatestByPublicationId(UUID id, int limit) {
        return repository.findByPublicationIdOrderByCreatedAtDescIdDesc(id,PageRequest.of(0,limit))
                .stream().map(JpaReviewRepositoryAdapter::toDomain).toList();
    }
    private static ProductReview toDomain(ProductReviewEntity r) {
        return new ProductReview(r.getId(),r.getPublicationId(),r.getBuyerId(),r.getRating(),r.getComment(),r.getCreatedAt());
    }
}
