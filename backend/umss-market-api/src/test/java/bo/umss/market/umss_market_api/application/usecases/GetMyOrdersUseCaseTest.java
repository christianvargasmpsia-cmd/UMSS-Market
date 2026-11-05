package bo.umss.market.umss_market_api.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.ports.OrderItemRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;

class GetMyOrdersUseCaseTest {

    private OrderRepositoryPort orders;
    private OrderItemRepositoryPort items;
    private PublicationRepositoryPort publications;
    private GetOrdersUseCase useCase;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepositoryPort.class);
        items = mock(OrderItemRepositoryPort.class);
        publications = mock(PublicationRepositoryPort.class);
        useCase = new GetOrdersUseCase(orders, items, publications);
    }

    @Test
    void filtersByUserAndStatusInRepository() {
        UUID currentUser = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(orders.findByUserIdAndStatus(currentUser, OrderStatus.CONFIRMADO))
                .thenReturn(List.of(Order.builder()
                        .id(orderId).userId(currentUser).status(OrderStatus.CONFIRMADO).build()));
        when(items.findByOrderId(orderId)).thenReturn(List.of());

        var result = useCase.execute(currentUser, OrderStatus.CONFIRMADO);

        assertEquals(1, result.size());
        assertEquals(currentUser, result.get(0).getUserId());
        assertEquals(OrderStatus.CONFIRMADO, result.get(0).getEstado());
        verify(orders).findByUserIdAndStatus(currentUser, OrderStatus.CONFIRMADO);
        verify(orders, never()).findAll();
        verify(orders, never()).findByStatus(any());
    }

    @Test
    void withoutStatusPreservesExistingUserHistory() {
        UUID currentUser = UUID.randomUUID();
        when(orders.findByUserId(currentUser)).thenReturn(List.of());

        assertTrue(useCase.execute(currentUser, null).isEmpty());
        verify(orders).findByUserId(currentUser);
        verify(orders, never()).findByUserIdAndStatus(any(), any());
    }

    @Test
    void legacyMethodStillWorks() {
        UUID currentUser = UUID.randomUUID();
        when(orders.findByUserId(currentUser)).thenReturn(List.of());
        assertTrue(useCase.execute(currentUser).isEmpty());
        verify(orders).findByUserId(currentUser);
    }
}
