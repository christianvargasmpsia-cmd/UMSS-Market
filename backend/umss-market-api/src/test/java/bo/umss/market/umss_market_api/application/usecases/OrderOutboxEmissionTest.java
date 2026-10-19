package bo.umss.market.umss_market_api.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.services.InventoryReservationService;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.enums.OrderEventType;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.*;
import bo.umss.market.umss_market_api.infrastructure.outbox.*;

class OrderOutboxEmissionTest {
    @Test void createEmitsOrderCreated() {
        var orders = mock(OrderRepositoryPort.class);
        var items = mock(OrderItemRepositoryPort.class);
        var publications = mock(PublicationRepositoryPort.class);
        var reservations = mock(InventoryReservationService.class);
        var writer = mock(OrderOutboxPort.class);
        UUID pubId = UUID.randomUUID();
        when(publications.findById(pubId)).thenReturn(Optional.of(
                Publication.builder().id(pubId).nombre("Libro").precio(BigDecimal.TEN)
                        .stock(4).tipo(PublicationType.PRODUCTO).activa(true).build()));
        when(reservations.reserve(any(Publication.class), eq(1))).thenReturn(true);
        var request = new CreateOrderRequest();
        request.setUserId(UUID.randomUUID());
        var item = new CreateOrderItemRequest();
        item.setPublicationId(pubId); item.setQuantity(1);
        request.setItems(List.of(item));
        var useCase = new CreateOrderUseCase(orders, items, publications, reservations, writer);
        var response = useCase.execute(request);
        assertTrue(response.isSuccess());
        verify(writer, times(1)).record(response.getOrderId(), OrderEventType.ORDER_CREATED, OrderStatus.PENDIENTE);
    }

    @Test void statusChangeEmitsEventButNoOpDoesNot() {
        var orders = mock(OrderRepositoryPort.class);
        var items = mock(OrderItemRepositoryPort.class);
        var inventory = mock(InventoryReservationService.class);
        var writer = mock(OrderOutboxPort.class);
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.PENDIENTE).build();
        when(orders.findByIdForUpdate(id)).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMADO);
        var useCase = new UpdateOrderStatusUseCase(orders, items, inventory, writer);
        assertEquals(OrderStatus.CONFIRMADO, useCase.execute(id, request).getStatus());
        useCase.execute(id, request);
        verify(writer, times(1)).record(id, OrderEventType.ORDER_STATUS_CHANGED, OrderStatus.CONFIRMADO);
    }
}
