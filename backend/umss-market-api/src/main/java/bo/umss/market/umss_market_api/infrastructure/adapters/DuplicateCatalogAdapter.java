package bo.umss.market.umss_market_api.infrastructure.adapters;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.domain.ports.DuplicateCatalogPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;
@Component @RequiredArgsConstructor
public class DuplicateCatalogAdapter implements DuplicateCatalogPort {
    private final EntityManager em;
    @Override @Transactional(readOnly=true)
    public List<Candidate> findByStore(UUID storeId) {
        return em.createQuery("select p from PublicationEntity p where p.storeId = :storeId order by p.id",PublicationEntity.class)
                .setParameter("storeId",storeId).getResultList().stream()
                .map(p->new Candidate(p.getId(),p.getNombre(),p.getDescripcion())).toList();
    }
}
