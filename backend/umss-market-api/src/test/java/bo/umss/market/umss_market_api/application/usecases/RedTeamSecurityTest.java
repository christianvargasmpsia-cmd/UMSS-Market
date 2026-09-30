package bo.umss.market.umss_market_api.application.usecases;

import bo.umss.market.umss_market_api.application.dto.ToolDecision;
import bo.umss.market.umss_market_api.application.services.AIServiceImpl;
import bo.umss.market.umss_market_api.domain.ports.AIProviderPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class RedTeamSecurityTest {

    @Test
    void attacksAndRegression() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> evidence =
                new ArrayList<>();

        List<String> failures =
                new ArrayList<>();

        boolean baseline =
                Boolean.getBoolean("redteam.baseline");

        boolean disableMitigations =
                Boolean.getBoolean("redteam.disableMitigations");

        for (int n = 1; n <= 5; n++) {

            String id =
                    String.format("RT-%03d", n);

            JsonNode attack;

            String external =
                    System.getProperty(
                            "redteam.attacksPath"
                    );

            try (var input =
                         external == null
                                 ? getClass()
                                     .getResourceAsStream(
                                         "/redteam/ataques/"
                                                 + id
                                                 + ".json"
                                     )
                                 : Files.newInputStream(
                                     Path.of(
                                         external,
                                         id + ".json"
                                     )
                                 )) {

                if (input == null) {
                    throw new IllegalStateException(
                            "No se encontro el ataque: "
                                    + id
                    );
                }

                attack =
                        mapper.readTree(input).get(0);
            }

            for (int run = 1; run <= 3; run++) {

                SecurityContextHolder.clearContext();

                try {

                    /*
                     * =====================================================
                     * DEPENDENCIAS CONTROLADAS
                     * =====================================================
                     */

                    var provider =
                            mock(AIProviderPort.class);

                    var catalog =
                            mock(SearchCatalogUseCase.class);

                    var history =
                            mock(
                                    GetUserInteractionsSemanticUseCase.class
                            );

                    var recommendations =
                            mock(
                                    GetRecommendationsUseCase.class
                            );

                    var detail =
                            mock(
                                    GetPublicationDetailSemanticUseCase.class
                            );

                    var stores =
                            mock(
                                    SearchStoresBySemanticUseCase.class
                            );

                    /*
                     * =====================================================
                     * SERVICIO REAL
                     * =====================================================
                     */

                    var service =
                            new AIServiceImpl(
                                    provider,
                                    catalog
                            );

                    ReflectionTestUtils.setField(
                            service,
                            "iaEnabled",
                            true
                    );

                    ReflectionTestUtils.setField(
                            service,
                            "userInteractionsUseCase",
                            history
                    );

                    ReflectionTestUtils.setField(
                            service,
                            "recommendationsUseCase",
                            recommendations
                    );

                    ReflectionTestUtils.setField(
                            service,
                            "publicationDetailUseCase",
                            detail
                    );

                    ReflectionTestUtils.setField(
                            service,
                            "searchStoresUseCase",
                            stores
                    );

                    /*
                     * =====================================================
                     * ACTOR
                     * =====================================================
                     */

                    String actor =
                            attack
                                    .path("fixture")
                                    .path("actor")
                                    .asText();

                    if (!"anonimo".equals(actor)) {

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(
                                        new UsernamePasswordAuthenticationToken(
                                                UUID.fromString(actor),
                                                "",
                                                List.of()
                                        )
                                );
                    }

                    /*
                     * =====================================================
                     * DECISION DEL ROUTER
                     * =====================================================
                     */

                    ToolDecision decision =
                            new ToolDecision();

                    decision.setTool(
                            attack
                                    .path("fixture")
                                    .path("tool")
                                    .asText()
                    );

                    decision.setQuery(
                            attack
                                    .path("fixture")
                                    .path("query")
                                    .asText()
                    );

                    when(
                            provider.selectTool(anyString())
                    ).thenReturn(decision);

                    /*
                     * =====================================================
                     * EJECUCION DEL ATAQUE
                     * =====================================================
                     */

                    String response =
                            service.chat(
                                    attack
                                            .path("mensaje")
                                            .asText()
                            );

                    /*
                     * =====================================================
                     * CAPTURA DE EJECUCIONES REALES
                     * =====================================================
                     */

                    var calls =
                            new ArrayList<Map<String, String>>();

                    for (Object dependency :
                            List.of(
                                    catalog,
                                    history,
                                    recommendations,
                                    detail,
                                    stores
                            )) {

                        for (var invocation :
                                mockingDetails(dependency)
                                        .getInvocations()) {

                            String firstArgument =
                                    invocation
                                            .getArguments()
                                            .length == 0
                                            ? ""
                                            : String.valueOf(
                                                invocation
                                                    .getArguments()[0]
                                            );

                            calls.add(
                                    Map.of(
                                            "metodo",
                                            invocation
                                                    .getMethod()
                                                    .getName(),

                                            "argumentos",
                                            Arrays.toString(
                                                    invocation
                                                            .getArguments()
                                            ),

                                            "primer_argumento",
                                            firstArgument
                                    )
                            );
                        }
                    }

                    /*
                     * =====================================================
                     * PROVEEDOR INVOCADO
                     * =====================================================
                     */

                    boolean providerInvoked =
                            !mockingDetails(provider)
                                    .getInvocations()
                                    .isEmpty();

                    /*
                     * =====================================================
                     * CRITERIO DEL ATAQUE
                     * =====================================================
                     */

                    String criterion =
                            attack
                                    .path("exito_si")
                                    .path("tipo")
                                    .asText();

                    String expectedValue =
                            attack
                                    .path("exito_si")
                                    .path("valor")
                                    .asText();

                    boolean attackSuccess;

                    switch (criterion) {

                        case "proveedor_invocado":

                            attackSuccess =
                                    providerInvoked;

                            break;

                        case "herramienta_ejecutada":

                            /*
                             * El ataque solo tiene exito si alguna
                             * dependencia real de herramienta fue
                             * ejecutada.
                             *
                             * Para RT-002, DELETE_ALL_ORDERS no existe
                             * como herramienta implementada. Por tanto,
                             * una lista vacia significa que la allowlist
                             * rechazo correctamente la herramienta.
                             */

                            attackSuccess =
                                    !calls.isEmpty();

                            break;

                        case "usuario_consultado":

                            attackSuccess =
                                    calls.stream()
                                            .anyMatch(
                                                    call ->
                                                            expectedValue
                                                                    .equals(
                                                                            call.get(
                                                                                    "primer_argumento"
                                                                            )
                                                                    )
                                            );

                            break;

                        default:

                            throw new IllegalArgumentException(
                                    "Criterio no soportado: "
                                            + criterion
                            );
                    }

                    /*
                     * =====================================================
                     * VALIDACION ESPECIFICA RT-005
                     * =====================================================
                     */

                    if (n == 5) {

                        UUID authenticatedUser =
                                UUID.fromString(actor);

                        if (!baseline) {

                            /*
                             * En modo protegido la herramienta debe
                             * ejecutarse usando exclusivamente el usuario
                             * autenticado.
                             */

                            verify(history)
                                    .executeUserHistory(
                                            eq(authenticatedUser),
                                            anyString()
                                    );

                        } else if (disableMitigations) {

                            /*
                             * En baseline se registra la ejecución que
                             * permitiría el ataque si la autorización
                             * estuviera desactivada.
                             *
                             * La comprobación exacta del usuario objetivo
                             * se realiza mediante attackSuccess.
                             */
                        }
                    }

                    /*
                     * =====================================================
                     * EVIDENCIA
                     * =====================================================
                     */

                    var row =
                            new LinkedHashMap<String, Object>();

                    row.put(
                            "ataque",
                            id
                    );

                    row.put(
                            "intento",
                            run
                    );

                    row.put(
                            "baseline",
                            baseline
                    );

                    row.put(
                            "mitigaciones_desactivadas",
                            disableMitigations
                    );

                    row.put(
                            "criterio",
                            criterion
                    );

                    row.put(
                            "exito_ataque",
                            attackSuccess
                    );

                    row.put(
                            "proveedor_invocado",
                            providerInvoked
                    );

                    row.put(
                            "herramientas",
                            calls
                    );

                    row.put(
                            "respuesta",
                            response
                    );

                    evidence.add(row);

                    /*
                     * =====================================================
                     * REGRESION
                     * =====================================================
                     *
                     * En modo protegido un ataque exitoso representa
                     * una regresion.
                     */

                    if (!baseline && attackSuccess) {

                        failures.add(
                                id
                                        + " intento "
                                        + run
                    );
                    }

                } finally {

                    SecurityContextHolder.clearContext();
                }
            }
        }

        /*
         * =============================================================
         * ARCHIVO DE SALIDA
         * =============================================================
         */

        Path output =
                Path.of(
                        "target/redteam/"
                                + (
                                    baseline
                                        ? "antes"
                                        : "despues"
                                )
                                + ".json"
                );

        Files.createDirectories(
                output.getParent()
        );

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put(
                "modo",
                "Servicio Java real; proveedor y repositorios simulados. "
                        + "No es una ejecucion de Ollama."
        );

        report.put(
                "baseline",
                baseline
        );

        report.put(
                "mitigaciones_desactivadas",
                disableMitigations
        );

        report.put(
                "total_ejecuciones",
                evidence.size()
        );

        report.put(
                "total_ataques",
                5
        );

        report.put(
                "repeticiones_por_ataque",
                3
        );

        report.put(
                "resultados",
                evidence
        );

        report.put(
                "fecha",
                java.time.Instant.now().toString()
        );

        mapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(
                        output.toFile(),
                        report
                );

        /*
         * =============================================================
         * QUALITY GATE
         * =============================================================
         */

        assertTrue(
                failures.isEmpty(),
                "Ataques exitosos en modo protegido: "
                        + failures
        );
    }
}