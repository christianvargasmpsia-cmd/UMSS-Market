package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import bo.umss.market.umss_market_api.infrastructure.outbox.OutboxStatus;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query("select e.id from OutboxEventEntity e " +
           "where e.status in :statuses and e.availableAt <= :now order by e.createdAt asc, e.id asc")
    List<UUID> findDueIds(@Param("statuses") List<OutboxStatus> statuses,
                          @Param("now") LocalDateTime now, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEventEntity e where e.id = :id")
    Optional<OutboxEventEntity> findByIdForUpdate(@Param("id") UUID id);
}
