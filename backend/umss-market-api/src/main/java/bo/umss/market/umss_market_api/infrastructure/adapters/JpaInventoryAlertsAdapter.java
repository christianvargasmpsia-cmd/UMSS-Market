package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.ports.InventoryAlertsPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaPublicationRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaInventoryAlertsAdapter implements InventoryAlertsPort {
    private final JpaPublicationRepository publications;

    @Override
    public StockPage findLowStock(UUID storeId, int threshold, int page, int size) {
        // Orden estable ante createdAt empatado: id es el desempate.
        var paging = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = publications.findLowStock(storeId, PublicationType.PRODUCTO, threshold, paging);
        return new StockPage(result.getContent().stream()
                .map(p -> new StockItem(p.getId(), p.getNombre(), p.getStock(), p.getCreatedAt()))
                .toList(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Override
    public StockSummary summarize(UUID storeId, int threshold) {
        long critical = publications.countLowStock(storeId, PublicationType.PRODUCTO, threshold);
        long empty = publications.countOutOfStock(storeId, PublicationType.PRODUCTO);
        // threshold=0 implica 0 productos con stock bajo positivo.
        return new StockSummary(critical, empty, critical - empty);
    }
}
