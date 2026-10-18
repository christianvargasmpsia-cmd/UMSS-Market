package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.enums.StoreStatus;
import bo.umss.market.umss_market_api.domain.model.CatalogPage;
import bo.umss.market.umss_market_api.domain.model.CatalogQuery;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.model.Store;
import bo.umss.market.umss_market_api.domain.ports.CatalogQueryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.StoreEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.mappers.PublicationMapper;
import bo.umss.market.umss_market_api.infrastructure.persistence.mappers.StoreMapper;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationRepository;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaStoreRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

/** Lecturas publicas paginadas. No modifica REST ni la logica de escritura. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaCatalogQueryAdapter implements CatalogQueryPort {

    private final JpaPublicationRepository publicaciones;
    private final JpaStoreRepository tiendas;

    @Override
    public CatalogPage<Publication> buscar(CatalogQuery f) {
        Specification<PublicationEntity> specification = (p, q, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.isTrue(p.get("activa")));

            // Solo publicaciones de tiendas activas. La categoria REAL esta en Store.
            Subquery<UUID> activeStores = q.subquery(UUID.class);
            Root<StoreEntity> s = activeStores.from(StoreEntity.class);
            List<Predicate> allowedStores = new ArrayList<>();
            allowedStores.add(cb.equal(s.get("status"), StoreStatus.ACTIVE));
            if (f.categoria() != null && !f.categoria().isBlank()) {
                allowedStores.add(cb.equal(cb.lower(s.get("categoria")), f.categoria().toLowerCase(java.util.Locale.ROOT)));
            }
            activeStores.select(s.get("id"))
                    .where(allowedStores.toArray(Predicate[]::new));
            conditions.add(p.get("storeId").in(activeStores));

            if (f.tiendaId() != null) conditions.add(cb.equal(p.get("storeId"), f.tiendaId()));
            if (f.tipo() != null) conditions.add(cb.equal(p.get("tipo"), f.tipo()));
            if (f.precioMin() != null) conditions.add(cb.greaterThanOrEqualTo(p.get("precio"), f.precioMin()));
            if (f.precioMax() != null) conditions.add(cb.lessThanOrEqualTo(p.get("precio"), f.precioMax()));
            if (f.texto() != null && !f.texto().isBlank()) {
                // Escapamos % y _ para que el usuario busque texto literal.
                String pattern = "%" + f.texto().toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(p.get("nombre")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(p.<String>get("descripcion"), "")), pattern, '\\')));
            }
            if (f.disponible() != null) {
                Predicate inStock = cb.and(cb.equal(p.get("tipo"), PublicationType.PRODUCTO),
                        cb.greaterThan(p.get("stock"), 0));
                Predicate service = cb.equal(p.get("tipo"), PublicationType.SERVICIO);
                conditions.add(f.disponible() ? cb.or(inStock, service)
                        : cb.and(cb.equal(p.get("tipo"), PublicationType.PRODUCTO),
                                cb.or(cb.isNull(p.get("stock")), cb.lessThanOrEqualTo(p.get("stock"), 0))));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        Page<PublicationEntity> page = publicaciones.findAll(specification,
                PageRequest.of(f.pagina() - 1, f.limite(), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new CatalogPage<>(page.getContent().stream().map(PublicationMapper::toDomain).toList(),
                page.getTotalElements(), f.pagina(), f.limite(), page.getTotalPages());
    }

    @Override
    public Optional<Publication> detallePublico(UUID id) {
        return publicaciones.findById(id).filter(p -> Boolean.TRUE.equals(p.getActiva()))
                .filter(p -> tiendas.findById(p.getStoreId())
                        .filter(s -> s.getStatus() == StoreStatus.ACTIVE).isPresent())
                .map(PublicationMapper::toDomain);
    }

    @Override
    public CatalogPage<Store> tiendas(String categoria, int pagina, int limite) {
        Specification<StoreEntity> filter = (s, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(s.get("status"), StoreStatus.ACTIVE));
            if (categoria != null && !categoria.isBlank()) {
                preds.add(cb.equal(cb.lower(s.get("categoria")), categoria.toLowerCase(java.util.Locale.ROOT)));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Page<StoreEntity> result = tiendas.findAll(filter,
                PageRequest.of(pagina - 1, limite, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));
        return new CatalogPage<>(result.getContent().stream().map(StoreMapper::toDomain).toList(),
                result.getTotalElements(), pagina, limite, result.getTotalPages());
    }

    @Override public List<String> categorias() { return tiendas.findActiveCategories(StoreStatus.ACTIVE); }

    @Override
    public List<Store> tiendasPorIds(Set<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return tiendas.findAllById(ids).stream()
                .filter(t -> t.getStatus() == StoreStatus.ACTIVE)
                .map(StoreMapper::toDomain).toList();
    }
}
