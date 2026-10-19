package bo.umss.market.umss_market_api.infrastructure.adapters;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import bo.umss.market.umss_market_api.domain.ports.DuplicateEmbeddingPort;
/** Uses the existing Ollama/nomic embedding infrastructure with a bounded batch request. */
@Component
public class DuplicateEmbeddingAdapter implements DuplicateEmbeddingPort {
    private final URI endpoint;
    private final String model;
    private final long timeout;
    private final HttpClient client;
    private final Semaphore slot=new Semaphore(1);
    private final ObjectMapper mapper=new ObjectMapper();
    public DuplicateEmbeddingAdapter(@Value("${duplicates.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${duplicates.ollama.model:nomic-embed-text}") String model,
            @Value("${duplicates.ollama.timeout-ms:45000}") long timeout) {
        if(timeout<1) throw new IllegalArgumentException("Timeout debe ser positivo");
        this.endpoint=URI.create(baseUrl.replaceAll("/+$","")+"/api/embed");this.model=model;this.timeout=timeout;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.min(timeout,2000))).build();
    }
    @Override public List<List<Double>> embed(List<String> texts) {
        if(!slot.tryAcquire()) throw new IllegalStateException("Embeddings ocupados");
        CompletableFuture<HttpResponse<String>> pending=null;
        try {
            String body=mapper.writeValueAsString(Map.of("model",model,"input",texts,"truncate",false,
                    "keep_alive","5m","options",Map.of("num_gpu",0)));
            var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(timeout))
                    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            pending=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
            var response=pending.get(timeout,TimeUnit.MILLISECONDS);
            if(response.statusCode()!=200) throw new IllegalStateException("Error HTTP de embeddings");
            var root=mapper.readTree(response.body());
            var vectors=root.path("embeddings");
            if(!vectors.isArray() || vectors.size()!=texts.size()) throw new IllegalStateException("Lote invalido");
            var result=new ArrayList<List<Double>>();
            for(var vector:vectors) {
                if(!vector.isArray() || vector.isEmpty()) throw new IllegalStateException("Vector invalido");
                var values=new ArrayList<Double>();
                for(var value:vector) {
                    if(!value.isNumber() || !Double.isFinite(value.doubleValue())) throw new IllegalStateException("Valor invalido");
                    values.add(value.doubleValue());
                }
                result.add(List.copyOf(values));
            }
            return List.copyOf(result);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();throw new IllegalStateException("Embeddings interrumpidos",ex);
        } catch(IOException | ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Embeddings no disponibles",ex);
        } finally {
            if(pending!=null) pending.cancel(true);
            slot.release();
        }
    }
}
