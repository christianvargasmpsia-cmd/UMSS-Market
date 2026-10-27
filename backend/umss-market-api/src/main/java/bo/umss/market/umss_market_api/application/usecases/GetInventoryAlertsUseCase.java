package bo.umss.market.umss_market_api.application.usecases;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import bo.umss.market.umss_market_api.application.dto.InventoryAlertPageResponse;
import bo.umss.market.umss_market_api.application.dto.InventoryAlertSummaryResponse;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.ports.InventoryAlertsPort;
import bo.umss.market.umss_market_api.domain.ports.StoreRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetInventoryAlertsUseCase {
    private final InventoryAlertsPort inventory;
    private final StoreRepositoryPort stores;
    private final UserRepositoryPort users;

    private void authorize(UUID userId, UUID storeId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacion requerida");
        if (storeId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tienda requerida");
        var user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no disponible"));
        if (user.getRole() != Role.EMPRENDEDOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere emprendedor");
        }
        var store = stores.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tienda no encontrada"));
        if (!userId.equals(store.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede consultar el inventario de otra tienda");
        }
    }

    private void validate(int threshold) {
        if (threshold < 0 || threshold > 1000000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "threshold debe estar entre 0 y 1000000");
        }
    }

    @Transactional(readOnly = true)
    public InventoryAlertPageResponse list(UUID userId, UUID storeId, int threshold, int page, int size) {
        authorize(userId, storeId);
        validate(threshold);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0 y size entre 1 y 100");
        }
        var result = inventory.findLowStock(storeId, threshold, page, size);
        var items = result.items().stream()
                .map(p -> new InventoryAlertPageResponse.InventoryAlertItem(
                        p.publicationId(), p.nombre(), p.stock(), p.stock() == 0 ? "AGOTADO" : "STOCK_BAJO"))
                .toList();
        return new InventoryAlertPageResponse(storeId, threshold, page, size,
                result.totalElements(), result.totalPages(), items);
    }

    @Transactional(readOnly = true)
    public InventoryAlertSummaryResponse summary(UUID userId, UUID storeId, int threshold) {
        authorize(userId, storeId);
        validate(threshold);
        var result = inventory.summarize(storeId, threshold);
        return new InventoryAlertSummaryResponse(storeId, threshold,
                result.productosCriticos(), result.agotados(), result.stockBajo());
    }
}
