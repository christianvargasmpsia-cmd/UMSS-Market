package bo.umss.market.umss_market_api.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import bo.umss.market.umss_market_api.domain.model.CatalogFilter;
import bo.umss.market.umss_market_api.domain.model.Publication;

public interface PublicationRepositoryPort {

    Publication save(Publication publication);

    List<Publication> findAll();

    Optional<Publication> findById(UUID id);

    /** Lectura bloqueada para ediciones que deban respetar reservas concurrentes. */
    Optional<Publication> findByIdForUpdate(UUID id);

    List<Publication> findByFilters(CatalogFilter filter);

    // Actualizacion condicional atomica, indispensable contra la sobreventa.
    boolean reserveIfAvailable(UUID id, int quantity);

    // Restituye inventario reservado; no aplica a servicios sin stock.
    void returnReservedStock(UUID id, int quantity);

    void deleteById(UUID id);
}