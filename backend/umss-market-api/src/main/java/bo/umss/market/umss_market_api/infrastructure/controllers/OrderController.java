package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.application.dto.CreateOrderRequest;
import bo.umss.market.umss_market_api.application.dto.CreateOrderResponse;
import bo.umss.market.umss_market_api.application.dto.OrderResponse;
import bo.umss.market.umss_market_api.application.dto.UpdateOrderRequest;
import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.application.usecases.CreateOrderItemUseCase;
import bo.umss.market.umss_market_api.application.usecases.CreateOrderUseCase;
import bo.umss.market.umss_market_api.application.usecases.DeleteOrderItemUseCase;
import bo.umss.market.umss_market_api.application.usecases.DeleteOrderUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetOrderByIdUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetOrderItemsUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetOrdersUseCase;
import bo.umss.market.umss_market_api.application.usecases.UpdateOrderItemUseCase;
import bo.umss.market.umss_market_api.application.usecases.UpdateOrderStatusUseCase;
import bo.umss.market.umss_market_api.application.usecases.UpdateOrderUseCase;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    private final GetOrdersUseCase getOrdersUseCase;

    private final GetOrderByIdUseCase getOrderByIdUseCase;

    private final UpdateOrderUseCase updateOrderUseCase;

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    private final DeleteOrderUseCase deleteOrderUseCase;

    private final GetOrderItemsUseCase getOrderItemsUseCase;

    private final CreateOrderItemUseCase createOrderItemUseCase;

    private final UpdateOrderItemUseCase updateOrderItemUseCase;

    private final DeleteOrderItemUseCase deleteOrderItemUseCase;


    // ============================================================
    // CREAR PEDIDO
    // ============================================================

    /**
     * Crear un nuevo pedido.
     *
     * El pedido se crea inicialmente con estado PENDIENTE.
     * El total se calcula utilizando los precios reales
     * de las publicaciones almacenadas en la base de datos.
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {

        CreateOrderResponse response =
                createOrderUseCase.execute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // OBTENER PEDIDOS DEL USUARIO
    // ============================================================

    /**
     * Obtener los pedidos de un usuario.
     *
     * GET /api/orders
     *
     * El userId se recibe mediante el request body debido
     * a que el caso de uso actual trabaja directamente
     * con el identificador del usuario.
     *
     * Para mantener el endpoint REST simple, se obtiene
     * mediante un parámetro opcional.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @org.springframework.web.bind.annotation.RequestParam UUID userId) {

        List<OrderResponse> orders =
                getOrdersUseCase.execute(userId);

        return ResponseEntity.ok(orders);
    }


    // ============================================================
    // OBTENER PEDIDO POR ID
    // ============================================================

    /**
     * Obtener un pedido por ID incluyendo sus productos.
     *
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id) {

        OrderResponse response =
                getOrderByIdUseCase.execute(id);

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // ACTUALIZAR PEDIDO
    // ============================================================

    /**
     * Actualizar los datos del pedido.
     *
     * PUT /api/orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable UUID id,
            @RequestBody UpdateOrderRequest request) {

        Order order =
                updateOrderUseCase.execute(
                        id,
                        request);

        return ResponseEntity.ok(order);
    }


    // ============================================================
    // CAMBIAR ESTADO DEL PEDIDO
    // ============================================================

    /**
     * Cambiar el estado del pedido.
     *
     * PATCH /api/orders/{id}/status
     *
     * Estados disponibles:
     *
     * PENDIENTE
     * CONFIRMADO
     * CANCELADO
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody UpdateOrderStatusRequest request) {

        Order order =
                updateOrderStatusUseCase.execute(
                        id,
                        request);

        return ResponseEntity.ok(order);
    }


    // ============================================================
    // ELIMINAR PEDIDO
    // ============================================================

    /**
     * Eliminar un pedido y sus items.
     *
     * DELETE /api/orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable UUID id) {

        deleteOrderUseCase.execute(id);

        return ResponseEntity
                .noContent()
                .build();
    }


    // ============================================================
    // OBTENER ITEMS DEL PEDIDO
    // ============================================================

    /**
     * Obtener todos los productos pertenecientes
     * a un pedido.
     *
     * GET /api/orders/{orderId}/items
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(
            @PathVariable UUID orderId) {

        List<OrderItem> items =
                getOrderItemsUseCase.execute(orderId);

        return ResponseEntity.ok(items);
    }


    // ============================================================
    // AGREGAR ITEM AL PEDIDO
    // ============================================================

    /**
     * Agregar un producto a un pedido existente.
     *
     * POST /api/orders/{orderId}/items
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItem> createOrderItem(
            @PathVariable UUID orderId,
            @RequestBody CreateOrderItemRequest request) {

        OrderItem item =
                createOrderItemUseCase.execute(
                        orderId,
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(item);
    }


    // ============================================================
    // ACTUALIZAR ITEM
    // ============================================================

    /**
     * Actualizar la cantidad de un producto
     * dentro del pedido.
     *
     * PUT /api/orders/{orderId}/items/{itemId}
     */
    @PutMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderItem> updateOrderItem(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @RequestBody CreateOrderItemRequest request) {

        OrderItem item =
                updateOrderItemUseCase.execute(
                        orderId,
                        itemId,
                        request);

        return ResponseEntity.ok(item);
    }


    // ============================================================
    // ELIMINAR ITEM
    // ============================================================

    /**
     * Eliminar un producto del pedido.
     *
     * DELETE /api/orders/{orderId}/items/{itemId}
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<Void> deleteOrderItem(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId) {

        deleteOrderItemUseCase.execute(
                orderId,
                itemId);

        return ResponseEntity
                .noContent()
                .build();
    }
}