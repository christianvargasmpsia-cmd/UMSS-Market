package bo.umss.market.umss_market_api.infrastructure.adapters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import bo.umss.market.umss_market_api.application.ports.ReviewsPort;
import bo.umss.market.umss_market_api.application.dto.ReviewRequest;
import bo.umss.market.umss_market_api.application.dto.ReviewResponse;
import bo.umss.market.umss_market_api.application.dto.ReviewSummaryResponse;

@Component
@ConditionalOnProperty(
    prefix = "e3.reviews",
    name = "mode",
    havingValue = "http"
)
public class ReviewsHttpAdapter implements ReviewsPort {

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration timeout;
    private final String internalToken;

    public record CreateCommand(
        UUID buyerId,
        ReviewRequest review
    ) {}

    public ReviewsHttpAdapter(
        ObjectMapper mapper,
        @Value("${e3.reviews.base-url:http://localhost:8081}")
        String baseUrl,
        @Value("${e3.reviews.timeout-ms:800}")
        long timeoutMs, @Value("${E3_INTERNAL_TOKEN:}") String internalToken
    ) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException(
                "El timeout debe ser positivo"
            );
        }

        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeout = Duration.ofMillis(timeoutMs);

        if (internalToken == null || internalToken.length() < 32) {
            throw new IllegalArgumentException(
                "E3_INTERNAL_TOKEN debe tener al menos 32 caracteres"
            );
        }

        this.internalToken = internalToken;

        this.client = HttpClient.newBuilder()
            .connectTimeout(
                Duration.ofMillis(Math.min(timeoutMs, 2000))
            )
            .build();
    }

    @Override
    public ReviewResponse create(
        UUID publicationId,
        UUID buyerId,
        ReviewRequest request
    ) {
        return send(
            "POST",
            "/internal/e3/reviews/" + publicationId,
            new CreateCommand(buyerId, request),
            mapper.constructType(ReviewResponse.class)
        );
    }

    @Override
    public List<ReviewResponse> list(
        UUID publicationId,
        int limit
    ) {
        return send(
            "GET",
            "/internal/e3/reviews/" + publicationId
                + "?limit=" + limit,
            null,
            mapper.getTypeFactory()
                .constructCollectionType(
                    List.class,
                    ReviewResponse.class
                )
        );
    }

    @Override
    public ReviewSummaryResponse summarize(
        UUID publicationId
    ) {
        return send(
            "GET",
            "/internal/e3/reviews/" + publicationId
                + "/summary",
            null,
            mapper.constructType(
                ReviewSummaryResponse.class
            )
        );
    }

    private <T> T send(
        String method,
        String path,
        Object payload,
        JavaType responseType
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(timeout)
            .header("X-E3-Internal-Token", internalToken);

        if ("POST".equals(method)) {
            String json;

            try {
                json = mapper.writeValueAsString(payload);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(
                    "No se pudo serializar la solicitud",
                    ex
                );
            }

            builder.header(
                "Content-Type",
                "application/json"
            );

            builder.POST(
                HttpRequest.BodyPublishers.ofString(json)
            );
        } else {
            builder.GET();
        }

        try {
            HttpResponse<String> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            if (status >= 400 && status < 500) {
                throw new ResponseStatusException(
                    HttpStatusCode.valueOf(status),
                    "Solicitud rechazada por el servicio de resenas"
                );
            }

            if (status >= 500) {
                throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Servicio de resenas no disponible"
                );
            }

            if (status < 200 || status >= 300) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta HTTP inesperada"
                );
            }

            try {
                return mapper.readValue(
                    response.body(),
                    responseType
                );
            } catch (JsonProcessingException ex) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta JSON invalida del servicio",
                    ex
                );
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Solicitud interrumpida",
                ex
            );

        } catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo conectar con el servicio de resenas",
                ex
            );
        }
    }
}