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
import bo.umss.market.umss_market_api.infrastructure.monitoring.AIMonitoringService;

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
    private final AIMonitoringService aiMonitoringService;

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
        this.aiMonitoringService = null;
    }

    @Autowired
    public AIServiceImpl(
            AIProviderPort provider,
            SearchCatalogUseCase searchCatalogUseCase,
            AIMonitoringService aiMonitoringService) {

        this.provider = provider;
        this.searchCatalogUseCase = searchCatalogUseCase;
        this.aiMonitoringService = aiMonitoringService;
    }

    /*
     * ================================================================
     * CONTROL EXCLUSIVO PARA RED TEAMING
     * ================================================================
     *
     * Por defecto las mitigaciones permanecen ACTIVAS.
     *
     * Para una prueba de baseline controlada:
     *
     * -Dredteam.disableMitigations=true
     *
     * Este interruptor solo se utiliza durante las pruebas Red Team.
     */
    private boolean redTeamMitigationsDisabled() {
        return Boolean.getBoolean("redteam.disableMitigations");
    }

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

    @Override
    public String chat(String message) {

        long startTime = System.nanoTime();

        String texto = message == null ? "" : message.trim();

        UUID currentUserId = getCurrentUserId();

        /*
         * ============================================================
         * 1. VALIDACIÓN DE LONGITUD
         * ============================================================
         *
         * Mitigación RT-001.
         *
         * En ejecución normal:
         *     límite ACTIVO
         *
         * En baseline Red Team:
         *     límite DESACTIVADO
         */

        if (!redTeamMitigationsDisabled()
                && ChatInputPolicy.exceedsLimit(message)) {

            String respuesta =
                    "AI_INPUT_TOO_LONG: La consulta supera el limite de "
                            + ChatInputPolicy.MAX_MESSAGE_LENGTH
                            + " caracteres UTF-16.";

            recordMonitoring(
                    currentUserId,
                    texto,
                    respuesta,
                    "INPUT_POLICY",
                    "NO_TOOL",
                    "BLOCK_INPUT_TOO_LONG",
                    startTime
            );

            return respuesta;
        }

        /*
         * ============================================================
         * 2. CONSULTA VACÍA
         * ============================================================
         */

        if (texto.isBlank()) {

            String respuesta = """
                    No puedo responder una consulta vacía.

                    Puedo ayudarte con:

                    - Buscar productos
                    - Buscar servicios
                    - Consultar detalles de publicaciones
                    - Buscar tiendas
                    - Consultar tu historial
                    - Generar recomendaciones
                    """;

            recordMonitoring(
                    currentUserId,
                    texto,
                    respuesta,
                    "INPUT_POLICY",
                    "NO_TOOL",
                    "BLOCK_EMPTY_INPUT",
                    startTime
            );

            return respuesta;
        }

        /*
         * ============================================================
         * 3. IA DESHABILITADA
         * ============================================================
         */

        if (!iaEnabled) {

            if (isCatalogSearchRequest(texto)) {

                String respuesta =
                        executeCatalogSearch(
                                texto,
                                "KEYWORD",
                                true
                        );

                recordMonitoring(
                        currentUserId,
                        texto,
                        respuesta,
                        "KEYWORD",
                        "SEARCH_CATALOG",
                        "ALLOW",
                        startTime
                );

                return respuesta;
            }

            String respuesta = """
                    Estado IA: DESHABILITADA

                    La IA está deshabilitada.

                    Puedo ayudarte con:

                    - Buscar publicaciones
                    - Buscar productos
                    - Buscar servicios
                    """;

            recordMonitoring(
                    currentUserId,
                    texto,
                    respuesta,
                    "IA_DISABLED",
                    "NO_TOOL",
                    "ALLOW",
                    startTime
            );

            return respuesta;
        }

        /*
         * ============================================================
         * 4. ROUTER DE IA
         * ============================================================
         */

        ToolDecision decision = provider.selectTool(texto);

        if (decision == null || decision.getTool() == null) {

            String respuesta = """
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

            recordMonitoring(
                    currentUserId,
                    texto,
                    respuesta,
                    "ROUTER",
                    "NO_TOOL",
                    "ALLOW",
                    startTime
            );

            return respuesta;
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

        /*
         * ============================================================
         * 5. EJECUCIÓN DE HERRAMIENTAS
         * ============================================================
         */

        switch (tool) {

            case "SEARCH_CATALOG": {

                String respuesta =
                        executeSemanticCatalogSearch(
                                texto,
                                "RAG_SEMANTICO_CATALOGO"
                        );

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "RAG_SEMANTICO_CATALOGO",
                        "SEARCH_CATALOG",
                        "ALLOW",
                        startTime
                );
            }

            case "PUBLICATION_DETAIL": {

                if (publicationDetailUseCase == null) {

                    String respuesta = """
                            El módulo de detalle de publicaciones
                            no está disponible actualmente.
                            """;

                    return monitoredResponse(
                            currentUserId,
                            texto,
                            respuesta,
                            "PUBLICATION_DETAIL",
                            "PUBLICATION_DETAIL",
                            "ALLOW",
                            startTime
                    );
                }

                UUID publicationId =
                        extractPublicationId(decision);

                if (publicationId != null) {

                    System.out.println(
                            "RAG #2 -> UUID encontrado: "
                                    + publicationId
                    );

                    String respuesta =
                            publicationDetailUseCase
                                    .executeWithContext(
                                            publicationId,
                                            texto
                                    );

                    return monitoredResponse(
                            currentUserId,
                            texto,
                            respuesta,
                            "PUBLICATION_DETAIL_UUID",
                            "PUBLICATION_DETAIL",
                            "ALLOW",
                            startTime
                    );
                }

                System.out.println(
                        "RAG #2 -> búsqueda semántica"
                );

                String respuesta =
                        publicationDetailUseCase
                                .executeSemanticSearch(texto);

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "PUBLICATION_DETAIL_SEMANTICO",
                        "PUBLICATION_DETAIL",
                        "ALLOW",
                        startTime
                );
            }

            case "SEARCH_STORES": {

                if (searchStoresUseCase == null) {

                    String respuesta = """
                            El módulo de búsqueda de tiendas
                            no está disponible actualmente.
                            """;

                    return monitoredResponse(
                            currentUserId,
                            texto,
                            respuesta,
                            "SEARCH_STORES",
                            "SEARCH_STORES",
                            "ALLOW",
                            startTime
                    );
                }

                String respuesta =
                        searchStoresUseCase
                                .executeSemanticSearch(texto);

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "RAG_SEMANTICO_TIENDAS",
                        "SEARCH_STORES",
                        "ALLOW",
                        startTime
                );
            }

            case "USER_INTERACTIONS": {

                if (userInteractionsUseCase == null) {

                    String respuesta = """
                            El módulo de historial de usuario
                            no está disponible actualmente.
                            """;

                    return monitoredResponse(
                            currentUserId,
                            texto,
                            respuesta,
                            "USER_INTERACTIONS",
                            "USER_INTERACTIONS",
                            "ALLOW",
                            startTime
                    );
                }

                if (currentUserId == null) {

                    String respuesta = """
                            Debes iniciar sesión para consultar
                            tu historial de interacciones.
                            """;

                    return monitoredResponse(
                            null,
                            texto,
                            respuesta,
                            "AUTH_REQUIRED",
                            "USER_INTERACTIONS",
                            "BLOCK_UNAUTHENTICATED",
                            startTime
                    );
                }

                System.out.println(
                        "RAG #4 -> Usuario autenticado: "
                                + currentUserId
                );

                String respuesta =
                        userInteractionsUseCase
                                .executeUserHistory(
                                        currentUserId,
                                        texto
                                );

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "RAG_HISTORIAL_USUARIO",
                        "USER_INTERACTIONS",
                        "ALLOW",
                        startTime
                );
            }

            case "RECOMMENDATIONS": {

                if (recommendationsUseCase == null) {

                    String respuesta = """
                            El módulo de recomendaciones
                            no está disponible actualmente.
                            """;

                    return monitoredResponse(
                            currentUserId,
                            texto,
                            respuesta,
                            "RECOMMENDATIONS",
                            "RECOMMENDATIONS",
                            "ALLOW",
                            startTime
                    );
                }

                if (currentUserId == null) {

                    String respuesta = """
                            Debes iniciar sesión para recibir
                            recomendaciones personalizadas.
                            """;

                    return monitoredResponse(
                            null,
                            texto,
                            respuesta,
                            "AUTH_REQUIRED",
                            "RECOMMENDATIONS",
                            "BLOCK_UNAUTHENTICATED",
                            startTime
                    );
                }

                System.out.println(
                        "RAG #5 -> Usuario autenticado: "
                                + currentUserId
                );

                String respuesta =
                        recommendationsUseCase
                                .getRecommendations(
                                        currentUserId,
                                        texto
                                );

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "RAG_RECOMENDACIONES",
                        "RECOMMENDATIONS",
                        "ALLOW",
                        startTime
                );
            }

            case "NO_TOOL": {

                String respuesta = """
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

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "NO_TOOL",
                        "NO_TOOL",
                        "ALLOW",
                        startTime
                );
            }

            default: {

                System.out.println(
                        "Tool desconocida: " + tool
                );

                String respuesta = """
                        No puedo responder esa consulta.

                        Puedo ayudarte con:

                        - Buscar productos
                        - Buscar servicios
                        - Consultar detalles de publicaciones
                        - Buscar tiendas
                        - Consultar tu historial
                        - Generar recomendaciones
                        """;

                return monitoredResponse(
                        currentUserId,
                        texto,
                        respuesta,
                        "UNKNOWN_TOOL",
                        tool,
                        "ALLOW",
                        startTime
                );
            }
        }
    }

    private String monitoredResponse(
            UUID userId,
            String question,
            String response,
            String camino,
            String herramienta,
            String guard,
            long startTime) {

        recordMonitoring(
                userId,
                question,
                response,
                camino,
                herramienta,
                guard,
                startTime
        );

        return response;
    }

    private void recordMonitoring(
            UUID userId,
            String question,
            String response,
            String camino,
            String herramienta,
            String guard,
            long startTime) {

        if (aiMonitoringService == null) {
            return;
        }

        long latencyMs =
                (System.nanoTime() - startTime)
                        / 1_000_000;

        aiMonitoringService.record(
                userId,
                question,
                response,
                camino,
                herramienta,
                guard,
                latencyMs
        );
    }

    private boolean isCatalogSearchRequest(String message) {

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

        respuesta.append(
                "Camino: "
        ).append(camino).append("\n\n");

        if (publicaciones == null
                || publicaciones.isEmpty()) {

            respuesta.append(
                    "No encontré publicaciones "
                            + "con esa búsqueda.\n"
            );

        } else {

            respuesta.append(
                    "Encontré "
            ).append(publicaciones.size())
                    .append(" publicaciones:\n\n");

            for (PublicationSummaryResponse p :
                    publicaciones) {

                respuesta.append("📌 ")
                        .append(p.getNombre())
                        .append("\n");

                respuesta.append("   Precio: Bs. ")
                        .append(p.getPrecio())
                        .append("\n");

                respuesta.append(
                        "   Stock disponible: "
                ).append(p.getStock())
                        .append("\n");

                respuesta.append("   Tienda: ")
                        .append(p.getNombreTienda())
                        .append("\n\n");
            }
        }

        return respuesta.toString();
    }

    private String executeSemanticCatalogSearch(
            String message,
            String camino) {

        List<PublicationSummaryResponse> publicaciones =
                searchCatalogUseCase
                        .executeSemanticSearch(
                                message,
                                5
                        );

        StringBuilder respuesta =
                new StringBuilder();

        respuesta.append(
                "Camino: "
        ).append(camino).append("\n\n");

        if (publicaciones == null
                || publicaciones.isEmpty()) {

            respuesta.append(
                    "No encontré publicaciones "
                            + "relevantes.\n"
            );

        } else {

            respuesta.append(
                    "Encontré "
            ).append(publicaciones.size())
                    .append(
                            " publicaciones relevantes:\n\n"
                    );

            for (PublicationSummaryResponse p :
                    publicaciones) {

                respuesta.append("📌 ")
                        .append(p.getNombre())
                        .append("\n");

                respuesta.append("   Precio: Bs. ")
                        .append(p.getPrecio())
                        .append("\n");

                respuesta.append(
                        "   Stock disponible: "
                ).append(p.getStock())
                        .append("\n");

                respuesta.append("   Tienda: ")
                        .append(p.getNombreTienda())
                        .append("\n\n");
            }
        }

        return respuesta.toString();
    }

    private UUID extractPublicationId(
            ToolDecision decision) {

        if (decision == null) {
            return null;
        }

        String query = decision.getQuery();

        if (query == null || query.isBlank()) {
            return null;
        }

        try {

            return UUID.fromString(
                    query.trim()
            );

        } catch (IllegalArgumentException ignored) {

            return null;
        }
    }

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