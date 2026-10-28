package bo.umss.market.umss_market_api.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import bo.umss.market.umss_market_api.application.dto.CreatePublicationRequest;
import bo.umss.market.umss_market_api.domain.enums.*;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.ports.*;
import bo.umss.market.umss_market_api.infrastructure.controllers.PublicationController;

class CreatePublicationUseCaseTest {
    private final PublicationRepositoryPort publications = mock(PublicationRepositoryPort.class);
    private final StoreRepositoryPort stores = mock(StoreRepositoryPort.class);
    private final CreatePublicationUseCase useCase = new CreatePublicationUseCase(publications, stores);
    private CreatePublicationRequest request;

    @BeforeEach void setup() {
        request = new CreatePublicationRequest();
        request.setStoreId(UUID.randomUUID());
        request.setNombre("Laptop Lenovo ThinkPad");
        request.setDescripcion("Laptop para estudio");
        request.setPrecio(new BigDecimal("4200"));
        request.setTipo(PublicationType.PRODUCTO);
        request.setStock(8);
        request.setModalidadCobro(PaymentMode.COMPLETO);
        when(stores.findById(request.getStoreId())).thenReturn(Optional.of(
            Store.builder().id(request.getStoreId()).status(StoreStatus.ACTIVE).build()));
        when(publications.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void createsProductWithoutAnyAiProvider() {
        var response = useCase.execute(request);
        assertTrue(response.isSuccess());
        verify(publications).save(argThat(p -> p.getId().equals(response.getPublicationId())
            && p.getEmbedding() == null && p.getStock() == 8 && p.getActiva()
            && p.getNombre().equals(request.getNombre()) && p.getDescripcion().equals(request.getDescripcion())
            && p.getPrecio().equals(request.getPrecio()) && p.getStoreId().equals(request.getStoreId())
            && p.getTipo() == PublicationType.PRODUCTO && p.getModalidadCobro() == PaymentMode.COMPLETO
            && p.getCreatedAt() != null && p.getUpdatedAt() != null));
    }

    @Test void createsServiceWithoutEmbedding() {
        request.setTipo(PublicationType.SERVICIO);
        request.setStock(null);
        assertTrue(useCase.execute(request).isSuccess());
        verify(publications).save(argThat(p -> p.getEmbedding() == null && p.getStock() == null));
    }

    @Test void rejectsMissingStore() {
        when(stores.findById(request.getStoreId())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(request));
        verifyNoInteractions(publications);
    }

    @Test void rejectsInactiveStore() {
        when(stores.findById(request.getStoreId())).thenReturn(Optional.of(
            Store.builder().status(StoreStatus.INACTIVE).build()));
        assertThrows(RuntimeException.class, () -> useCase.execute(request));
        verifyNoInteractions(publications);
    }

    @Test void rejectsInvalidStock() {
        for (Integer stock : new Integer[]{null, 0, -1}) {
            request.setStock(stock);
            assertThrows(RuntimeException.class, () -> useCase.execute(request));
        }
        verifyNoInteractions(publications);
    }

    @Test void rejectsServiceWithoutPaymentMode() {
        request.setTipo(PublicationType.SERVICIO);
        request.setModalidadCobro(null);
        assertThrows(RuntimeException.class, () -> useCase.execute(request));
        verifyNoInteractions(publications);
    }

    @Test void doesNotHidePersistenceFailures() {
        when(publications.save(any())).thenThrow(new IllegalStateException("Database unavailable"));
        assertThrows(IllegalStateException.class, () -> useCase.execute(request));
    }

    @Test void httpCreationIntegratesControllerAndUseCaseWithoutOllama() throws Exception {
        var controller = new PublicationController(useCase, mock(SearchCatalogUseCase.class),
            mock(GetPublicationByIdUseCase.class), mock(UpdatePublicationUseCase.class),
            mock(UpdatePublicationStatusUseCase.class), mock(DeletePublicationUseCase.class));
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/api/publications")
            .contentType("application/json").content("""
                {"storeId":"%s","nombre":"Laptop Lenovo ThinkPad",
                 "descripcion":"Laptop para estudio","precio":4200,"tipo":"PRODUCTO",
                 "stock":8,"modalidadCobro":"COMPLETO"}
                """.formatted(request.getStoreId())))
            .andExpect(status().isCreated()).andExpect(jsonPath("success").value(true))
            .andExpect(jsonPath("publicationId").isNotEmpty());
        verify(publications).save(argThat(p -> p.getEmbedding() == null && p.getStock() == 8));
    }
}
