package bo.umss.market.umss_market_api.application.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.domain.exceptions.InsufficientStockException;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;

class InventoryReservationServiceTest {
    private final PublicationRepositoryPort port = mock(PublicationRepositoryPort.class);
    private final InventoryReservationService service = new InventoryReservationService(port);

    @Test
    void reserveTrackedStockAtomically() {
        UUID id = UUID.randomUUID();
        Publication p = Publication.builder().id(id).nombre("Mochila")
                .activa(true).tipo(PublicationType.PRODUCTO).stock(1).build();
        when(port.reserveIfAvailable(id, 1)).thenReturn(true);
        assertTrue(service.reserve(p, 1));
        verify(port).reserveIfAvailable(id, 1);
    }

    @Test
    void rejectInsufficientStockWithoutChangingAnything() {
        UUID id = UUID.randomUUID();
        Publication p = Publication.builder().id(id).nombre("Mochila")
                .activa(true).tipo(PublicationType.PRODUCTO).stock(1).build();
        when(port.reserveIfAvailable(id, 2)).thenReturn(false);
        assertThrows(InsufficientStockException.class, () -> service.reserve(p, 2));
        verify(port, never()).returnReservedStock(any(), anyInt());
    }

    @Test
    void noInventoryForUnlimitedService() {
        Publication p = Publication.builder().id(UUID.randomUUID()).activa(true)
                .tipo(PublicationType.SERVICIO).stock(null).build();
        assertFalse(service.reserve(p, 7));
        verifyNoInteractions(port);
    }

    @Test
    void rejectNullStockForPhysicalProduct() {
        Publication p = Publication.builder().id(UUID.randomUUID()).activa(true)
                .tipo(PublicationType.PRODUCTO).stock(null).build();
        assertThrows(IllegalStateException.class, () -> service.reserve(p, 1));
    }

    @Test
    void onlyReturnRealReservations() {
        UUID id = UUID.randomUUID();
        service.release(id, 3, false);
        verifyNoInteractions(port);
        service.release(id, 3, true);
        verify(port).returnReservedStock(id, 3);
    }
}
