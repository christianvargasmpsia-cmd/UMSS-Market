package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.application.usecases.UpdateOrderStatusUseCase;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.ports.*;

/** UC-002: pedidos recibidos por una tienda; nunca expone items de otras tiendas. */
@RestController
@RequestMapping("/api/stores/{storeId}/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StoreReceivedOrdersController {
    private final StoreRepositoryPort stores;
    private final UserRepositoryPort users;
    private final OrderRepositoryPort orders;
    private final OrderItemRepositoryPort items;
    private final PublicationRepositoryPort publications;
    private final UpdateOrderStatusUseCase changeStatus;

    private void authorize(UUID storeId, UUID userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT requerido");
        var user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inexistente"));
        if (user.getRole() != Role.EMPRENDEDOR) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo emprendedores");
        var store = stores.findById(storeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tienda inexistente"));
        if (!userId.equals(store.getUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tienda ajena");
    }

    public record ReceivedItem(UUID publicationId, String productName, Integer quantity,
                               java.math.BigDecimal unitPrice, java.math.BigDecimal subtotal) {}
    public record ReceivedOrder(UUID orderId, UUID buyerId, OrderStatus status,
                                java.time.LocalDateTime createdAt, List<ReceivedItem> items,
                                java.math.BigDecimal storeSubtotal) {}

    private List<ReceivedItem> ownedItems(UUID orderId, UUID storeId) {
        List<ReceivedItem> result = new ArrayList<>();
        for (OrderItem item : items.findByOrderId(orderId)) {
            Publication p = publications.findById(item.getPublicationId()).orElse(null);
            if (p != null && storeId.equals(p.getStoreId()))
                result.add(new ReceivedItem(p.getId(), p.getNombre(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()));
        }
        return result;
    }

    private ReceivedOrder view(Order order, UUID storeId) {
        var own = ownedItems(order.getId(), storeId);
        if (own.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no pertenece a esta tienda");
        var subtotal = own.stream().map(ReceivedItem::subtotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return new ReceivedOrder(order.getId(), order.getUserId(), order.getStatus(), order.getCreatedAt(), own, subtotal);
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Listar pedidos recibidos de mi tienda", description = "Solo items de la tienda autenticada; estado opcional")
    public List<ReceivedOrder> list(@PathVariable UUID storeId, @AuthenticationPrincipal UUID userId,
                                    @RequestParam(required = false) OrderStatus status) {
        authorize(storeId, userId);
        List<ReceivedOrder> result = new ArrayList<>();
        // MVP: recorrido en memoria; para volumen alto reemplazar por consulta SQL con JOIN y paginacion.
        for (Order order : orders.findAll()) {
            if (status != null && order.getStatus() != status) continue;
            var own = ownedItems(order.getId(), storeId);
            if (!own.isEmpty()) {
                var subtotal = own.stream().map(ReceivedItem::subtotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                result.add(new ReceivedOrder(order.getId(), order.getUserId(), order.getStatus(), order.getCreatedAt(), own, subtotal));
            }
        }
        return result;
    }

    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Consultar detalle de pedido recibido de mi tienda")
    public ReceivedOrder detail(@PathVariable UUID storeId, @PathVariable UUID orderId,
                                @AuthenticationPrincipal UUID userId) {
        authorize(storeId, userId);
        Order order = orders.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido inexistente"));
        return view(order, storeId);
    }

    @PatchMapping("/{orderId}/status")
    @Transactional
    @Operation(summary = "Actualizar estado de pedido de mi tienda")
    public ReceivedOrder status(@PathVariable UUID storeId, @PathVariable UUID orderId,
                                @AuthenticationPrincipal UUID userId, @RequestBody UpdateOrderStatusRequest request) {
        authorize(storeId, userId);
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido inexistente"));
        List<OrderItem> all = items.findByOrderId(orderId);
        if (all.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido sin items");
        // El estado es global al pedido: bloquear cambios cuando mezcla varias tiendas.
        for (OrderItem item : all) {
            Publication p = publications.findById(item.getPublicationId()).orElse(null);
            if (p == null || !storeId.equals(p.getStoreId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido multitienda: no se puede modificar estado global desde una sola tienda");
        }
        if (request == null || request.getStatus() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado requerido");
        try {
            return view(changeStatus.execute(orderId, request), storeId);
        } catch (bo.umss.market.umss_market_api.domain.exceptions.InvalidOrderStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }
}
