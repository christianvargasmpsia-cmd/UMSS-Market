package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import bo.umss.market.umss_market_api.domain.enums.StoreStatus;

import bo.umss.market.umss_market_api.infrastructure.persistence.entities.StoreEntity;

public interface JpaStoreRepository extends JpaRepository<StoreEntity, UUID>, JpaSpecificationExecutor<StoreEntity> {

    @Query("SELECT DISTINCT s.categoria FROM StoreEntity s WHERE s.status = :status AND s.categoria IS NOT NULL AND TRIM(s.categoria) <> '' ORDER BY s.categoria")
    java.util.List<String> findActiveCategories(@Param("status") StoreStatus status);

    Optional<StoreEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
