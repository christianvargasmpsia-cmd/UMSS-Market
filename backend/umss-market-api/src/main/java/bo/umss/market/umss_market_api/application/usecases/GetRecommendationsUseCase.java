package bo.umss.market.umss_market_api.application.usecases;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import bo.umss.market.umss_market_api.application.dto.RecommendationsResponse;
import bo.umss.market.umss_market_api.application.dto.RecommendationsResponse.Item;
import bo.umss.market.umss_market_api.domain.model.Publication;
import bo.umss.market.umss_market_api.domain.enums.InteractionType;
import bo.umss.market.umss_market_api.domain.ports.InteractionRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.PublicationRepositoryPort;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.domain.ports.RecommendationTextPort;

/** Content-based ranking with optional, failure-tolerant Ollama explanation. */
@Service
@RequiredArgsConstructor
public class GetRecommendationsUseCase {
    private final InteractionRepositoryPort interactionRepository;
    private final PublicationRepositoryPort publicationRepository;
    private final RecommendationTextPort recommendationText;
    private static final Set<String> STOP_WORDS = Set.of(
            "de", "la", "el", "los", "las", "un", "una", "para", "con", "del", "por", "en", "y");

    public RecommendationsResponse execute(UUID userId, int limit) {
        Objects.requireNonNull(userId, "Usuario autenticado requerido");
        if (limit < 1 || limit > 50) throw new IllegalArgumentException("limit debe estar entre 1 y 50");
        List<Publication> catalog = publicationRepository.findAll();
        Map<UUID, Publication> byId = catalog.stream().collect(Collectors.toMap(Publication::getId, p -> p));
        Map<String, Double> interests = new HashMap<>();
        Map<UUID, Double> stores = new HashMap<>();
        Set<UUID> seen = new HashSet<>();
        for (var interaction : interactionRepository.findRecentByUserId(userId, 30)) {
            // Defense in depth: never incorporate another buyer's history.
            if (!userId.equals(interaction.getUserId())) continue;
            Publication source = byId.get(interaction.getPublicationId());
            if (source == null) continue;
            double weight = weight(interaction.getType());
            tokens(source).forEach(token -> interests.merge(token, weight, Double::sum));
            stores.merge(source.getStoreId(), weight, Double::sum);
            seen.add(source.getId());
        }
        List<Item> items = catalog.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActiva()))
                .filter(p -> p.getStock() != null && p.getStock() > 0)
                .filter(p -> !seen.contains(p.getId()))
                .map(p -> new Item(p.getId(), p.getNombre(), p.getPrecio(), p.getStock(),
                        p.getStoreId(), score(p, interests, stores)))
                .sorted(Comparator.comparingDouble(Item::affinity).reversed().thenComparing(Item::id))
                .limit(limit).toList();
        boolean personalized = !seen.isEmpty();
        if (items.isEmpty()) return new RecommendationsResponse(personalized, items,
                "No hay publicaciones disponibles para recomendar.", "NOT_REQUESTED");
        String fallback = personalized ? "Productos ordenados segun tus interacciones recientes."
                : "Publicaciones disponibles del catalogo general; aun no hay historial util.";
        try {
            String explanation = recommendationText.explain(items.stream().limit(3)
                    .map(p -> Objects.toString(p.name(), "Producto")).toList(), personalized);
            if (explanation == null || explanation.isBlank() || explanation.length() > 1500) {
                throw new IllegalStateException("Explicacion vacia o demasiado extensa");
            }
            return new RecommendationsResponse(personalized, items, explanation.strip(), "GENERATED");
        } catch (RuntimeException ex) {
            return new RecommendationsResponse(personalized, items,
                    fallback + " La explicacion con IA no esta disponible en este momento.", "FALLBACK");
        }
    }

    /** Preserves the chat contract; query text cannot override the authenticated identity. */
    public String getRecommendations(UUID userId, String userQuery) {
        var response = execute(userId, 10);
        if (response.items().isEmpty()) return "No hay publicaciones disponibles para recomendar.";
        String heading = response.personalized() ? "Recomendaciones segun tus interacciones:" : "Recomendaciones generales:";
        return heading + "\n" + response.explanation() + "\n" + response.items().stream()
                .map(p -> p.name() + " | Bs. " + p.price() + " | ID: " + p.id())
                .collect(Collectors.joining("\n"));
    }

    private static double weight(InteractionType type) {
        if (type == null) return 1;
        return switch (type) {
            case VIEW -> 1;
            case LIKE, COMMENT -> 2;
            case FAVORITE -> 3;
            case PURCHASE -> 4;
        };
    }

    private static Set<String> tokens(Publication p) {
        String text = Objects.toString(p.getNombre(), "") + " " + Objects.toString(p.getDescripcion(), "");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() > 1).filter(t -> !STOP_WORDS.contains(t)).collect(Collectors.toSet());
    }

    private static double score(Publication p, Map<String, Double> interests, Map<UUID, Double> stores) {
        Set<String> terms = tokens(p);
        double content = terms.stream().mapToDouble(t -> interests.getOrDefault(t, 0.0)).sum()
                / Math.max(1, terms.size());
        return content + 0.25 * stores.getOrDefault(p.getStoreId(), 0.0);
    }
}
