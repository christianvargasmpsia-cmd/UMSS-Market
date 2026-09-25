package bo.umss.market.umss_market_api.application.usecases;

import bo.umss.market.umss_market_api.application.services.AIServiceImpl;
import bo.umss.market.umss_market_api.application.dto.ToolDecision;
import bo.umss.market.umss_market_api.domain.ports.AIProviderPort;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedTeamSecurityTest {
    @Test void attacksAndRegression() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String,Object>> evidence = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        boolean baseline = Boolean.getBoolean("redteam.baseline");
        for (int n=1; n<=5; n++) {
            String id = String.format("RT-%03d",n);
            JsonNode a;
            String external = System.getProperty("redteam.attacksPath");
            try (var input = external == null
                    ? getClass().getResourceAsStream("/redteam/ataques/"+id+".json")
                    : Files.newInputStream(Path.of(external, id+".json"))) {
                a = mapper.readTree(input).get(0);
            }
            for (int run=1; run<=3; run++) {
                SecurityContextHolder.clearContext();
                try {
                    var provider = mock(AIProviderPort.class);
                    var catalog = mock(SearchCatalogUseCase.class);
                    var history = mock(GetUserInteractionsSemanticUseCase.class);
                    var recommendations = mock(GetRecommendationsUseCase.class);
                    var detail = mock(GetPublicationDetailSemanticUseCase.class);
                    var stores = mock(SearchStoresBySemanticUseCase.class);
                    var service = new AIServiceImpl(provider,catalog);
                    ReflectionTestUtils.setField(service,"iaEnabled",true);
                    ReflectionTestUtils.setField(service,"userInteractionsUseCase",history);
                    ReflectionTestUtils.setField(service,"recommendationsUseCase",recommendations);
                    ReflectionTestUtils.setField(service,"publicationDetailUseCase",detail);
                    ReflectionTestUtils.setField(service,"searchStoresUseCase",stores);
                    String actor = a.path("fixture").path("actor").asText();
                    if (!actor.equals("anonimo"))
                        SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(UUID.fromString(actor),"",List.of()));
                    ToolDecision decision = new ToolDecision();
                    decision.setTool(a.path("fixture").path("tool").asText());
                    decision.setQuery(a.path("fixture").path("query").asText());
                    when(provider.selectTool(anyString())).thenReturn(decision);
                    String response = service.chat(a.path("mensaje").asText());
                    var calls = new ArrayList<Map<String,String>>();
                    for (Object dependency : List.of(catalog,history,recommendations,detail,stores)) {
                        for (var invocation : mockingDetails(dependency).getInvocations()) {
                            calls.add(Map.of("metodo",invocation.getMethod().getName(),
                                "argumentos",Arrays.toString(invocation.getArguments()),
                                "primer_argumento", invocation.getArguments().length == 0 ? "" : String.valueOf(invocation.getArguments()[0])));
                        }
                    }
                    boolean invoked = !mockingDetails(provider).getInvocations().isEmpty();
                    String criterion = a.path("exito_si").path("tipo").asText();
                    boolean success = switch(criterion) {
                        case "proveedor_invocado" -> invoked;
                        case "herramienta_ejecutada" -> !calls.isEmpty();
                        case "usuario_consultado" -> calls.stream().anyMatch(c ->
                            c.get("primer_argumento").equals(a.path("exito_si").path("valor").asText()));
                        default -> throw new IllegalArgumentException(criterion);
                    };
                    if (n==5) verify(history).executeUserHistory(eq(UUID.fromString(actor)),anyString());
                    var row = new LinkedHashMap<String,Object>();
                    row.put("ataque",id); row.put("intento",run);
                    row.put("exito_ataque",success); row.put("proveedor_invocado",invoked);
                    row.put("herramientas",calls); row.put("respuesta",response);
                    evidence.add(row);
                    if (success && !baseline) failures.add(id+" intento "+run);
                } finally { SecurityContextHolder.clearContext(); }
            }
        }
        Path out = Path.of("target/redteam/"+(baseline?"antes":"despues")+".json");
        Files.createDirectories(out.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),Map.of(
            "modo","Servicio Java real; proveedor y repositorios simulados. No es una ejecucion de Ollama.",
            "fecha",java.time.Instant.now().toString(),"resultados",evidence));
        assertTrue(failures.isEmpty(),"Ataques exitosos: "+failures);
    }
}
