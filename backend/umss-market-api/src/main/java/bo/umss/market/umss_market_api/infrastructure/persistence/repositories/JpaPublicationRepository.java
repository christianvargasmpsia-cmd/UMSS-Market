package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;

public interface JpaPublicationRepository
        extends JpaRepository<PublicationEntity, UUID>, JpaSpecificationExecutor<PublicationEntity> {

    // Consulta de lectura sin bloqueo pesimista: compatible con @Transactional(readOnly = true).
    // Nueva funcionalidad Demo 2: solo publicaciones activas, productos fisicos y stock configurado.
    @Query("""
            SELECT p FROM PublicationEntity p
             WHERE p.storeId = :storeId AND p.tipo = :type AND p.activa = true
               AND p.stock IS NOT NULL AND p.stock >= 0 AND p.stock <= :threshold
            """)
    Page<PublicationEntity> findLowStock(@Param("storeId") UUID storeId,
            @Param("type") PublicationType type, @Param("threshold") int threshold, Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM PublicationEntity p
             WHERE p.storeId = :storeId AND p.tipo = :type AND p.activa = true
               AND p.stock IS NOT NULL AND p.stock >= 0 AND p.stock <= :threshold
            """)
    long countLowStock(@Param("storeId") UUID storeId,
            @Param("type") PublicationType type, @Param("threshold") int threshold);

    @Query("""
            SELECT COUNT(p) FROM PublicationEntity p
             WHERE p.storeId = :storeId AND p.tipo = :type AND p.activa = true AND p.stock = 0
            """)
    long countOutOfStock(@Param("storeId") UUID storeId, @Param("type") PublicationType type);

    @Query("SELECT p FROM PublicationEntity p WHERE p.id = :id")
    Optional<PublicationEntity> findByIdForUpdate(@Param("id") UUID id);

    // PostgreSQL: UPDATE con predicado de stock mantiene un row lock hasta commit.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PublicationEntity p
               SET p.stock = p.stock - :quantity
             WHERE p.id = :id
               AND p.activa = true
               AND p.stock IS NOT NULL
               AND p.stock >= :quantity
            """)
    int reserveStock(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PublicationEntity p
               SET p.stock = p.stock + :quantity
             WHERE p.id = :id
               AND p.stock IS NOT NULL
            """)
    int returnStock(@Param("id") UUID id, @Param("quantity") int quantity);

    @Query("""
            SELECT p
            FROM PublicationEntity p
            WHERE p.activa = true

              AND (
                    :storeId IS NULL
                    OR p.storeId = :storeId
                  )

              AND (
                    :tipo IS NULL
                    OR p.tipo = :tipo
                  )

              AND (
                    :precioMin IS NULL
                    OR p.precio >= :precioMin
                  )

              AND (
                    :precioMax IS NULL
                    OR p.precio <= :precioMax
                  )

              AND (
                    COALESCE(:textoBusqueda, '') = ''
                    OR LOWER(p.nombre)
                        LIKE LOWER(CONCAT('%', :textoBusqueda, '%'))
                    OR LOWER(p.descripcion)
                        LIKE LOWER(CONCAT('%', :textoBusqueda, '%'))
                  )

            ORDER BY p.createdAt DESC
            """)
    List<PublicationEntity> findByFilters(
            @Param("textoBusqueda") String textoBusqueda,
            @Param("tipo") PublicationType tipo,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            @Param("storeId") UUID storeId);
}
