package bo.umss.market.umss_market_api.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.application.dto.UpdatePublicationRequest;
import bo.umss.market.umss_market_api.application.dto.UpdatePublicationStatusRequest;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;

class PublicationInventoryLockTest {

    @Test
    void metadataUpdateUsesLockedReadAndPreservesLatestStock() throws Exception {
        var repository = mock(PublicationRepositoryPort.class);
        UUID id = UUID.randomUUID();
        var publication = Publication.builder().id(id).nombre("Cuaderno")
                .precio(BigDecimal.TEN).stock(3).activa(true).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(publication));
        when(repository.save(any(Publication.class))).thenAnswer(call -> call.getArgument(0));

        var request = new UpdatePublicationRequest();
        request.setNombre("Cuaderno A4");
        var result = new UpdatePublicationUseCase(repository).execute(id, request);
        assertEquals("Cuaderno A4", result.getNombre());
        assertEquals(3, result.getStock());
        verify(repository).findByIdForUpdate(id);
        verify(repository, never()).findById(id);
        assertNotNull(UpdatePublicationUseCase.class
                .getMethod("execute", UUID.class, UpdatePublicationRequest.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void publicationStatusUpdateUsesTheSameStockLock() throws Exception {
        var repository = mock(PublicationRepositoryPort.class);
        UUID id = UUID.randomUUID();
        var publication = Publication.builder().id(id).stock(2).activa(true).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(publication));
        when(repository.save(any(Publication.class))).thenAnswer(call -> call.getArgument(0));

        var request = new UpdatePublicationStatusRequest();
        request.setActiva(false);
        var result = new UpdatePublicationStatusUseCase(repository).execute(id, request);
        assertFalse(result.getActiva());
        assertEquals(2, result.getStock());
        verify(repository).findByIdForUpdate(id);
        verify(repository, never()).findById(id);
        assertNotNull(UpdatePublicationStatusUseCase.class
                .getMethod("execute", UUID.class, UpdatePublicationStatusRequest.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void negativeStockIsRejectedBeforeModifyingPublication() {
        var repository = mock(PublicationRepositoryPort.class);
        var request = new UpdatePublicationRequest();
        request.setStock(-1);
        assertThrows(IllegalArgumentException.class,
                () -> new UpdatePublicationUseCase(repository).execute(UUID.randomUUID(), request));
        verifyNoInteractions(repository);
    }
}
