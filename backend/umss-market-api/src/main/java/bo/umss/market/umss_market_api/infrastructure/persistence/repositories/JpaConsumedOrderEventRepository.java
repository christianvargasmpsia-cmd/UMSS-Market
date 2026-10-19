package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ConsumedOrderEventEntity;

public interface JpaConsumedOrderEventRepository extends JpaRepository<ConsumedOrderEventEntity, UUID> {}
