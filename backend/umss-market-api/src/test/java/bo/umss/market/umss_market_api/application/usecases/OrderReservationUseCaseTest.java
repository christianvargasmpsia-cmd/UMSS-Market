package bo.umss.market.umss_market_api.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import bo.umss.market.umss_market_api.application.dto.CreateOrderRequest;
import bo.umss.market.umss_market_api.application.dto.CreateOrderItemRequest;
import bo.umss.market.umss_market_api.application.dto.UpdateOrderStatusRequest;
import bo.umss.market.umss_market_api.application.services.InventoryReservationService;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;

class OrderReservationUseCaseTest {
    @Test
    void createOrderStoresReservationMarkerAndServerCalculatedTotal() {
        var orders = mock(OrderRepositoryPort.class);
        var items = mock(OrderItemRepositoryPort.class);
        var publications = mock(PublicationRepositoryPort.class);
        var inventory = mock(InventoryReservationService.class);
        UUID id = UUID.randomUUID();
        when(publications.findById(id)).thenReturn(Optional.of(
                Publication.builder().id(id).activa(true).tipo(PublicationType.PRODUCTO)
                        .stock(3).nombre("Libro").precio(new BigDecimal("20.00")).build()));
        when(inventory.reserve(any(Publication.class), eq(2))).thenReturn(true);
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setPublicationId(id);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("0.01")); // cliente no decide precio
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(UUID.randomUUID());
        request.setItems(List.of(item));
        var useCase = new CreateOrderUseCase(orders, items, publications, inventory);
        assertTrue(useCase.execute(request).isSuccess());
        ArgumentCaptor<OrderItem> captured = ArgumentCaptor.forClass(OrderItem.class);
        verify(items).save(captured.capture());
        assertEquals(true, captured.getValue().getStockReserved());
        assertEquals(new BigDecimal("40.00"), captured.getValue().getSubtotal());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(orderCaptor.capture());
        assertEquals(new BigDecimal("40.00"), orderCaptor.getValue().getTotal());
    }

    @Test
    void cancelReleasesOnlyMarkedItemsAndCannotReopenOrder() {
        var orders = mock(OrderRepositoryPort.class);
        var items = mock(OrderItemRepositoryPort.class);
        var inventory = mock(InventoryReservationService.class);
        UUID orderId = UUID.randomUUID(), pubId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status(OrderStatus.PENDIENTE).build();
        OrderItem reserved = OrderItem.builder().id(UUID.randomUUID()).orderId(orderId)
                .publicationId(pubId).quantity(2).stockReserved(true).build();
        OrderItem legacy = OrderItem.builder().id(UUID.randomUUID()).orderId(orderId)
                .publicationId(pubId).quantity(9).stockReserved(false).build();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(items.findByOrderId(orderId)).thenReturn(List.of(reserved, legacy));
        when(orders.save(any(Order.class))).thenAnswer(x -> x.getArgument(0));
        var useCase = new UpdateOrderStatusUseCase(orders, items, inventory);
        var request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CANCELADO);
        assertEquals(OrderStatus.CANCELADO, useCase.execute(orderId, request).getStatus());
        assertEquals(false, reserved.getStockReserved());
        verify(inventory, times(1)).release(pubId, 2, true);
        useCase.execute(orderId, request); // idempotencia
        verify(inventory, times(1)).release(pubId, 2, true);
        request.setStatus(OrderStatus.CONFIRMADO);
        assertThrows(
    bo.umss.market.umss_market_api.domain.exceptions.InvalidOrderStateException.class,
    () -> useCase.execute(orderId, request)
);
    }
}
