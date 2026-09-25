package bo.umss.market.umss_market_api.application.services;

import bo.umss.market.umss_market_api.application.dto.CatalogFilterRequest;
import bo.umss.market.umss_market_api.application.dto.PublicationSummaryResponse;
import bo.umss.market.umss_market_api.application.dto.ToolDecision;
import bo.umss.market.umss_market_api.application.usecases.GetPublicationDetailSemanticUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetRecommendationsUseCase;
import bo.umss.market.umss_market_api.application.usecases.GetUserInteractionsSemanticUseCase;
import bo.umss.market.umss_market_api.application.usecases.SearchCatalogUseCase;
import bo.umss.market.umss_market_api.application.usecases.SearchStoresBySemanticUseCase;
import bo.umss.market.umss_market_api.domain.ports.AIProviderPort;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AIServiceImpl implements AIService {

    @Value("${ia.enabled:${IA_HABILITADA:false}}")
    private boolean iaEnabled;

    private final AIProviderPort provider;
    private final SearchCatalogUseCase searchCatalogUseCase;

    @Autowired(required = false)
    private GetPublicationDetailSemanticUseCase publicationDetailUseCase;

    @Autowired(required = false)
    private SearchStoresBySemanticUseCase searchStoresUseCase;

    @Autowired(required = false)
    private GetUserInteractionsSemanticUseCase userInteractionsUseCase;

    @Autowired(required = false)
    private GetRecommendationsUseCase recommendationsUseCase;

    public AIServiceImpl(
            AIProviderPort provider,
            SearchCatalogUseCase searchCatalogUseCase) {

        this.provider = provider;
        this.searchCatalogUseCase = searchCatalogUseCase;
    }

    // ============================================================
    // GENERATE
    // ============================================================

    @Override
    public String generate(String prompt) {

        if (!iaEnabled) {
            return """
                    La IA está deshabilitada.

                    Solo puedo ayudarte con búsquedas
                    del catálogo.
                    """;
        }

        return provider.generate(prompt);
    }

    // ============================================================
    // CHAT PRINCIPAL
    // ============================================================

    @Override
    public String chat(String message) {

        if (ChatInputPolicy.exceedsLimit(message)) {
            return "AI_INPUT_TOO_LONG: La consulta supera el limite de "
                    + ChatInputPolicy.MAX_MESSAGE_LENGTH + " caracteres UTF-16.";
        }

        String texto = message == null
                ? ""
                : message.trim();

        // ========================================================
        // MENSAJE VACÍO
        // ========================================================

        if (texto.isBlank()) {

            return """
                    No puedo responder una consulta vacía.

                    Puedo ayudarte con:

                    - Buscar productos
                    - Buscar servicios
                    - Consultar detalles de publicaciones
                    - Buscar tiendas
                    - Consultar tu historial
                    - Generar recomendaciones
                    """;
        }

        // ========================================================
        // IA DESHABILITADA
        // ========================================================

        if (!iaEnabled) {

            if (isCatalogSearchRequest(texto)) {

                return executeCatalogSearch(
                        texto,
                        "KEYWORD",
                        true
                );
            }

            return """
                    Estado IA: DESHABILITADA

                    La IA está deshabilitada.

                    Puedo ayudarte con:

                    - Buscar publicaciones
                    - Buscar productos
                    - Buscar servicios
                    """;
        }

        // ========================================================
        // IA HABILITADA
        // ========================================================

        ToolDecision decision = provider.selectTool(texto);

        if (decision == null || decision.getTool() == null) {

            return """
                    No pude determinar qué herramienta
                    utilizar para tu consulta.

                    Puedo ayudarte con:

                    - Buscar productos
                    - Buscar servicios
                    - Consultar detalles de publicaciones
                    - Buscar tiendas
                    - Consultar tu historial
                    - Generar recomendaciones
                    """;
        }

        String tool = decision.getTool()
                .trim()
                .toUpperCase(Locale.ROOT);

        System.out.println();
        System.out.println("=================================");
        System.out.println("AI TOOL ROUTER");
        System.out.println("=================================");
        System.out.println("Pregunta : " + texto);
        System.out.println("Tool     : " + tool);
        System.out.println("Query    : " + decision.getQuery());
        System.out.println("=================================");

        // ========================================================
        // ROUTER
        // ========================================================

        switch (tool) {

            // ====================================================
            // RAG #1
            // SEARCH CATALOG
            // ====================================================

            case "SEARCH_CATALOG":

                return executeSemanticCatalogSearch(
                        texto,
                        "RAG_SEMANTICO_CATALOGO"
                );

            // ====================================================
            // RAG #2
            // PUBLICATION DETAIL
            // ====================================================

            case "PUBLICATION_DETAIL":

                if (publicationDetailUseCase == null) {

                    return """
                            El módulo de detalle de publicaciones
                            no está disponible actualmente.
                            """;
                }

                UUID publicationId =
                        extractPublicationId(decision);

                // ------------------------------------------------
                // UUID DIRECTO
                // ------------------------------------------------

                if (publicationId != null) {

                    System.out.println(
                            "RAG #2 -> UUID encontrado: "
                                    + publicationId
                    );

                    return publicationDetailUseCase
                            .executeWithContext(
                                    publicationId,
                                    texto
                            );
                }

                // ------------------------------------------------
                // BÚSQUEDA SEMÁNTICA
                // ------------------------------------------------

                System.out.println(
                        "RAG #2 -> búsqueda semántica"
                );

                return publicationDetailUseCase
                        .executeSemanticSearch(texto);

            // ====================================================
            // RAG #3
            // SEARCH STORES
            // ====================================================

            case "SEARCH_STORES":

                if (searchStoresUseCase == null) {

                    return """
                            El módulo de búsqueda de tiendas
                            no está disponible actualmente.
                            """;
                }

                return searchStoresUseCase
                        .executeSemanticSearch(texto);

            // ====================================================
            // RAG #4
            // USER INTERACTIONS
            // ====================================================

            case "USER_INTERACTIONS":

                if (userInteractionsUseCase == null) {

                    return """
                            El módulo de historial de usuario
                            no está disponible actualmente.
                            """;
                }

                UUID currentUserId =
                        getCurrentUserId();

                if (currentUserId == null) {

                    return """
                            Debes iniciar sesión para consultar
                            tu historial de interacciones.
                            """;
                }

                System.out.println(
                        "RAG #4 -> Usuario autenticado: "
                                + currentUserId
                );

                return userInteractionsUseCase
                        .executeUserHistory(
                                currentUserId,
                                texto
                        );

            // ====================================================
            // RAG #5
            // RECOMMENDATIONS
            // ====================================================

            case "RECOMMENDATIONS":

                if (recommendationsUseCase == null) {

                    return """
                            El módulo de recomendaciones
                            no está disponible actualmente.
                            """;
                }

                UUID recommendationUserId =
                        getCurrentUserId();

                if (recommendationUserId == null) {

                    return """
                            Debes iniciar sesión para recibir
                            recomendaciones personalizadas.
                            """;
                }

                System.out.println(
                        "RAG #5 -> Usuario autenticado: "
                                + recommendationUserId
                );

                return recommendationsUseCase
                        .getRecommendations(
                                recommendationUserId,
                                texto
                        );

            // ====================================================
            // NO TOOL
            // ====================================================

            case "NO_TOOL":

                return """
                        No encontré una herramienta de UMSS Market
                        que corresponda a tu consulta.

                        Puedo ayudarte con:

                        - Buscar productos
                        - Buscar servicios
                        - Consultar detalles de publicaciones
                        - Buscar tiendas
                        - Consultar tu historial
                        - Generar recomendaciones
                        """;

            // ====================================================
            // TOOL DESCONOCIDA
            // ====================================================

            default:

                System.out.println(
                        "Tool desconocida: "
                                + tool
                );

                return """
                        No puedo responder esa consulta.

                        Puedo ayudarte con:

                        - Buscar productos
                        - Buscar servicios
                        - Consultar detalles de publicaciones
                        - Buscar tiendas
                        - Consultar tu historial
                        - Generar recomendaciones
                        """;
        }
    }

    // ============================================================
    // DETECTAR BÚSQUEDA DE CATÁLOGO
    // ============================================================

    private boolean isCatalogSearchRequest(
            String message) {

        String texto =
                message.toLowerCase(Locale.ROOT);

        return texto.contains("buscar")
                || texto.contains("busco")
                || texto.contains("producto")
                || texto.contains("productos")
                || texto.contains("publicación")
                || texto.contains("publicaciones")
                || texto.contains("catalogo")
                || texto.contains("catálogo")
                || texto.contains("servicio")
                || texto.contains("servicios");
    }

    // ============================================================
    // BÚSQUEDA KEYWORD
    // ============================================================

    private String executeCatalogSearch(
            String message,
            String camino,
            boolean showDisabledBanner) {

        CatalogFilterRequest request =
                new CatalogFilterRequest();

        String busqueda = message
                .replaceAll("(?i)buscar", "")
                .replaceAll("(?i)busco", "")
                .replaceAll("(?i)producto", "")
                .replaceAll("(?i)productos", "")
                .replaceAll("(?i)publicación", "")
                .replaceAll("(?i)publicaciones", "")
                .replaceAll("(?i)catálogo", "")
                .replaceAll("(?i)catalogo", "")
                .trim();

        request.setTexto(busqueda);

        List<PublicationSummaryResponse> publicaciones =
                searchCatalogUseCase.execute(request);

        StringBuilder respuesta =
                new StringBuilder();

        if (showDisabledBanner) {

            respuesta.append(
                    "Estado IA: DESHABILITADA\n\n"
            );
        }

        respuesta.append("Camino: ")
                .append(camino)
                .append("\n\n");

        if (publicaciones == null
                || publicaciones.isEmpty()) {

            respuesta.append(
                    "No encontré publicaciones con esa búsqueda.\n"
            );

        } else {

            respuesta.append(
                    "Encontré "
            )
            .append(publicaciones.size())
            .append(" publicaciones:\n\n");

            for (PublicationSummaryResponse p
                    : publicaciones) {

                respuesta.append("📌 ")
                        .append(p.getNombre())
                        .append("\n");

                respuesta.append(
                        "   Precio: Bs. "
                )
                .append(p.getPrecio())
                .append("\n");

                respuesta.append(
                        "   Stock disponible: "
                )
                .append(p.getStock())
                .append("\n");

                respuesta.append(
                        "   Tienda: "
                )
                .append(p.getNombreTienda())
                .append("\n\n");
            }
        }

        return respuesta.toString();
    }

    // ============================================================
    // RAG #1
    // BÚSQUEDA SEMÁNTICA DE CATÁLOGO
    // ============================================================

    private String executeSemanticCatalogSearch(
            String message,
            String camino) {

        List<PublicationSummaryResponse> publicaciones =
                searchCatalogUseCase.executeSemanticSearch(
                        message,
                        5
                );

        StringBuilder respuesta =
                new StringBuilder();

        respuesta.append("Camino: ")
                .append(camino)
                .append("\n\n");

        if (publicaciones == null
                || publicaciones.isEmpty()) {

            respuesta.append(
                    "No encontré publicaciones relevantes.\n"
            );

        } else {

            respuesta.append(
                    "Encontré "
            )
            .append(publicaciones.size())
            .append(" publicaciones relevantes:\n\n");

            for (PublicationSummaryResponse p
                    : publicaciones) {

                respuesta.append("📌 ")
                        .append(p.getNombre())
                        .append("\n");

                respuesta.append(
                        "   Precio: Bs. "
                )
                .append(p.getPrecio())
                .append("\n");

                respuesta.append(
                        "   Stock disponible: "
                )
                .append(p.getStock())
                .append("\n");

                respuesta.append(
                        "   Tienda: "
                )
                .append(p.getNombreTienda())
                .append("\n\n");
            }
        }

        return respuesta.toString();
    }

    // ============================================================
    // EXTRAER UUID DE PUBLICACIÓN
    // ============================================================

    private UUID extractPublicationId(
            ToolDecision decision) {

        if (decision == null) {
            return null;
        }

        String query =
                decision.getQuery();

        if (query == null
                || query.isBlank()) {

            return null;
        }

        String cleanQuery =
                query.trim();

        try {

            return UUID.fromString(cleanQuery);

        } catch (IllegalArgumentException ignored) {

            return null;
        }
    }

    // ============================================================
    // OBTENER USUARIO AUTENTICADO
    // ============================================================

    private UUID getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return null;
        }

        if (!authentication.isAuthenticated()) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof UUID) {
            return (UUID) principal;
        }

        if (principal instanceof String) {

            try {

                return UUID.fromString(
                        (String) principal
                );

            } catch (IllegalArgumentException ignored) {

                return null;
            }
        }

        return null;
    }
}
