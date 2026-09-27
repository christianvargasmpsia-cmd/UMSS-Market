package bo.umss.market.umss_market_api.infrastructure.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class AIMonitoringService {

    private final ObjectMapper objectMapper;

    @Value("${ai.monitoring.enabled:true}")
    private boolean enabled;

    @Value("${ai.monitoring.version:m7-v1}")
    private String version;

    @Value("${ai.monitoring.log-path:logs/ai-monitoring.jsonl}")
    private String logPath;

    public AIMonitoringService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void record(
            UUID userId,
            String question,
            String answer,
            String camino,
            String herramienta,
            String guard,
            long latencyMs) {

        if (!enabled) {
            return;
        }

        try {
            AIMonitoringEvent event = new AIMonitoringEvent();

            event.setTs(OffsetDateTime.now());

            /*
             * Se registra únicamente el UUID del usuario autenticado.
             * Nunca se registra JWT, contraseña, API key ni correo.
             */
            event.setUsuario(
                    userId != null
                            ? userId.toString()
                            : "anonymous"
            );

            event.setVersion(version);
            event.setPregunta(question);
            event.setRespuesta(answer);

            /*
             * Actualmente AIServiceImpl no expone las fuentes
             * recuperadas como una lista estructurada.
             */
            event.setFuentes(Collections.emptyList());

            event.setCamino(camino);
            event.setHerramienta(herramienta);
            event.setGuard(guard);
            event.setLatenciaMs(latencyMs);

            /*
             * OllamaResponse todavía no proporciona token counts.
             */
            event.setTokens(null);

            /*
             * El endpoint /api/ai/chat todavía no recibe feedback.
             */
            event.setFeedback(null);

            String json = objectMapper.writeValueAsString(event);

            Path path = Paths.get(logPath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    path,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            /*
             * El monitoreo nunca debe impedir que la IA responda.
             */
            System.err.println(
                    "No se pudo registrar evento de monitoreo IA: "
                            + e.getMessage()
            );
        }
    }
}