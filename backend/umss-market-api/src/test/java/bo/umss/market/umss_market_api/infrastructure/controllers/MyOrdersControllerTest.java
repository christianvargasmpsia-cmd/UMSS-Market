package bo.umss.market.umss_market_api.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.application.usecases.GetOrdersUseCase;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;

class MyOrdersControllerTest {

    @Test
    void usesAuthenticatedPrincipalInsteadOfClientSuppliedUserId() {
        var useCase = mock(GetOrdersUseCase.class);
        var controller = new MyOrdersController(useCase);
        UUID principal = UUID.randomUUID();
        when(useCase.execute(principal, OrderStatus.PENDIENTE)).thenReturn(List.of());

        var response = controller.list(principal, OrderStatus.PENDIENTE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(useCase).execute(principal, OrderStatus.PENDIENTE);
    }

    @Test
    void rejectsMissingAuthenticatedPrincipal() {
        var controller = new MyOrdersController(mock(GetOrdersUseCase.class));
        var error = assertThrows(ResponseStatusException.class,
                () -> controller.list(null, null));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }
}
