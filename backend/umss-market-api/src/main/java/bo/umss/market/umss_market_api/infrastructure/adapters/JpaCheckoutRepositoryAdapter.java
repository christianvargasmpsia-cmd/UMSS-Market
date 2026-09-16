package bo.umss.market.umss_market_api.infrastructure.adapters;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.CheckoutRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.mappers.*;

@Component
@RequiredArgsConstructor
public class JpaCheckoutRepositoryAdapter implements CheckoutRepositoryPort {
    private final EntityManager entityManager;

    public boolean lockUser(UUID userId) {
        return entityManager.find(UserEntity.class, userId, LockModeType.PESSIMISTIC_WRITE) != null;
    }
    public Optional<Publication> lockPublication(UUID id) {
        return Optional.ofNullable(entityManager.find(PublicationEntity.class, id, LockModeType.PESSIMISTIC_WRITE))
                .map(PublicationMapper::toDomain);
    }
    public boolean decreaseStock(UUID id, int quantity) {
        return entityManager.createQuery("""
            UPDATE PublicationEntity p SET p.stock = p.stock - :quantity
            WHERE p.id = :id AND p.stock >= :quantity AND p.activa = true
            """).setParameter("id", id).setParameter("quantity", quantity).executeUpdate() == 1;
    }
    public void insertOrder(Order order) {
        // persist, no merge: un identificador existente nunca debe sobrescribirse.
        entityManager.persist(OrderMapper.toEntity(order));
        entityManager.flush();
    }
}
