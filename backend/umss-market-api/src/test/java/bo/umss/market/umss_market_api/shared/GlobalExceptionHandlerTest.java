package bo.umss.market.umss_market_api.shared;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import bo.umss.market.umss_market_api.domain.exceptions.EmailAlreadyExistsException;
import bo.umss.market.umss_market_api.domain.exceptions.StoreAlreadyExistsException;
import bo.umss.market.umss_market_api.domain.exceptions.UserAlreadyExistsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldHandleUserAlreadyExists() {

        var response =
                handler.handleUserAlreadyExists(
                        new UserAlreadyExistsException());

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldHandleEmailAlreadyExists() {

        var response =
                handler.handleEmailAlreadyExists(
                        new EmailAlreadyExistsException());

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldHandleStoreAlreadyExists() {

        var response =
                handler.handleStoreAlreadyExists(
                        new StoreAlreadyExistsException());

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldHandleGenericException() {

        var response =
                handler.handleGenericException(
                        new RuntimeException("error"));

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void oversizedUploadReturns413() {

        var exception =
                new org.springframework.web.multipart.MaxUploadSizeExceededException(
                        5L * 1024 * 1024);

        var response = handler.handleMaxUploadSize(exception);

        assertEquals(413, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
