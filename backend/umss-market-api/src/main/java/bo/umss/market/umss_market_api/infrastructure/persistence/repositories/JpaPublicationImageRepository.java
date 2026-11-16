package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationImageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPublicationImageRepository extends JpaRepository<PublicationImageEntity, UUID> {
    List<PublicationImageEntity> findByPublicationIdOrderByCreatedAtAsc(UUID publicationId);
}
