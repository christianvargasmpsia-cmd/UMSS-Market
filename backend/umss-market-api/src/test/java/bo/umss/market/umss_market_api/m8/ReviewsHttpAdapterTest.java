package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.ReviewsHttpAdapter;

class ReviewsHttpAdapterTest {

    private static final String TEST_TOKEN =
        "e3-test-token-only-not-for-production-123456789";

    private HttpServer server;
    private ObjectMapper mapper;
    private ReviewsHttpAdapter adapter;

    @BeforeEach
    void setup() throws IOException {
        mapper = new ObjectMapper().findAndRegisterModules();

        server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0
        );

        server.start();

        adapter = new ReviewsHttpAdapter(
            mapper,
            "http://127.0.0.1:" + server.getAddress().getPort(),
            2000,
            TEST_TOKEN
        );
    }

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void respond(
        HttpExchange exchange,
        int status,
        String json
    ) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type",
            "application/json"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Test
    void listUsesCorrectRouteAndParsesResponse() throws Exception {
        UUID publicationId = UUID.randomUUID();

        ReviewResponse expected = new ReviewResponse(
            UUID.randomUUID(),
            publicationId,
            5,
            "Excelente producto",
            Instant.parse("2026-11-02T12:00:00Z")
        );

        AtomicReference<String> request = new AtomicReference<>();
        AtomicReference<String> receivedToken = new AtomicReference<>();

        server.createContext("/internal/e3/reviews/", exchange -> {
            request.set(
                exchange.getRequestMethod()
                    + " "
                    + exchange.getRequestURI()
            );

            receivedToken.set(
                exchange.getRequestHeaders()
                    .getFirst("X-E3-Internal-Token")
            );

            respond(
                exchange,
                200,
                mapper.writeValueAsString(List.of(expected))
            );
        });

        var actual = adapter.list(publicationId, 20);

        assertEquals(List.of(expected), actual);

        assertEquals(
            "GET /internal/e3/reviews/"
                + publicationId
                + "?limit=20",
            request.get()
        );

        assertEquals(TEST_TOKEN, receivedToken.get());
    }

    @Test
    void createSendsBuyerAndReview() throws Exception {
        UUID publicationId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();

        ReviewRequest review =
            new ReviewRequest(5, "Muy bueno");

        ReviewResponse expected = new ReviewResponse(
            UUID.randomUUID(),
            publicationId,
            5,
            "Muy bueno",
            Instant.parse("2026-11-02T12:00:00Z")
        );

        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<JsonNode> received = new AtomicReference<>();

        server.createContext("/internal/e3/reviews/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());

            received.set(
                mapper.readTree(
                    exchange.getRequestBody().readAllBytes()
                )
            );

            respond(
                exchange,
                201,
                mapper.writeValueAsString(expected)
            );
        });

        var actual = adapter.create(
            publicationId,
            buyerId,
            review
        );

        assertEquals(expected, actual);
        assertEquals("POST", method.get());

        assertEquals(
            "/internal/e3/reviews/" + publicationId,
            path.get()
        );

        assertEquals(
            buyerId.toString(),
            received.get().path("buyerId").asText()
        );

        assertEquals(
            5,
            received.get().path("review").path("rating").asInt()
        );

        assertEquals(
            "Muy bueno",
            received.get().path("review").path("comment").asText()
        );
    }

    @Test
    void summaryParsesResponse() throws Exception {
        UUID publicationId = UUID.randomUUID();

        ReviewSummaryResponse expected =
            new ReviewSummaryResponse(
                publicationId,
                0,
                0,
                10,
                "NO_REVIEWS",
                "Sin resenas",
                List.of(),
                List.of()
            );

        AtomicReference<String> path = new AtomicReference<>();

        server.createContext("/internal/e3/reviews/", exchange -> {
            path.set(exchange.getRequestURI().getPath());

            respond(
                exchange,
                200,
                mapper.writeValueAsString(expected)
            );
        });

        var actual = adapter.summarize(publicationId);

        assertEquals(expected, actual);

        assertEquals(
            "/internal/e3/reviews/"
                + publicationId
                + "/summary",
            path.get()
        );
    }

    @Test
    void propagatesNotFound() {
        server.createContext("/internal/e3/reviews/", exchange ->
            respond(exchange, 404, "{\"message\":\"No existe\"}")
        );

        var error = assertThrows(
            ResponseStatusException.class,
            () -> adapter.list(UUID.randomUUID(), 20)
        );

        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void mapsServerFailureToServiceUnavailable() {
        server.createContext("/internal/e3/reviews/", exchange ->
            respond(exchange, 500, "{\"message\":\"Error\"}")
        );

        var error = assertThrows(
            ResponseStatusException.class,
            () -> adapter.list(UUID.randomUUID(), 20)
        );

        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void rejectsInvalidJson() {
        server.createContext("/internal/e3/reviews/", exchange ->
            respond(exchange, 200, "{invalid-json")
        );

        var error = assertThrows(
            ResponseStatusException.class,
            () -> adapter.list(UUID.randomUUID(), 20)
        );

        assertEquals(502, error.getStatusCode().value());
    }

    @Test
    void rejectsMissingInternalToken() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ReviewsHttpAdapter(
                mapper,
                "http://127.0.0.1:8081",
                2000,
                ""
            )
        );
    }
}
