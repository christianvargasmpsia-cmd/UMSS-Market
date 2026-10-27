package bo.umss.market.umss_market_api.infrastructure.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/** Exporta el contrato desde Springdoc; NO crea ejemplos ficticios. */
@SpringBootTest(properties = {"ia.enabled=false", "ai.monitoring.enabled=false"})
@AutoConfigureMockMvc
class OpenApiContractExportTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void exportsCanonicalCodeFirstOpenApi() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode document = mapper.readTree(json);
        assertTrue(document.path("openapi").asText().startsWith("3."), "Se requiere OpenAPI 3.x");
        JsonNode paths = document.path("paths");
        assertTrue(paths.has("/api/orders"), "Contrato: endpoint de pedidos");
        assertTrue(paths.has("/api/stores/{storeId}/inventory/low-stock"), "Contrato: stock bajo");
        assertTrue(paths.has("/api/stores/{storeId}/inventory/summary"), "Contrato: resumen de inventario");
        assertTrue(paths.path("/api/stores/{storeId}/inventory/low-stock").has("get"));
        assertEquals("listLowStock", paths.path("/api/stores/{storeId}/inventory/low-stock")
                .path("get").path("operationId").asText());
        // La clase anidada Item chocaba con otro esquema llamado Item (checkout).
        // Verificar la forma del contrato, no solo la existencia del endpoint.
        String itemRef = paths.path("/api/stores/{storeId}/inventory/low-stock")
                .path("get").path("responses").path("200").path("content")
                .path("*/*").path("schema").path("$ref").asText();
        assertEquals("#/components/schemas/InventoryAlertPageResponse", itemRef);
        JsonNode inventoryItemRef = document.path("components").path("schemas")
                .path("InventoryAlertPageResponse").path("properties")
                .path("items").path("items").path("$ref");
        assertEquals("#/components/schemas/InventoryAlertItem", inventoryItemRef.asText(),
                "El esquema de inventario no debe apuntar al Item de checkout");
        JsonNode itemProperties = document.path("components").path("schemas")
                .path("InventoryAlertItem").path("properties");
        for (String property : new String[] {"publicationId", "nombre", "stock", "estado"}) {
            assertTrue(itemProperties.has(property), "Falta propiedad OpenAPI: " + property);
        }
        Path output = Path.of("target", "generated-openapi.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted(document)) + "\n",
                StandardCharsets.UTF_8);
        System.out.println("OPENAPI_EXPORT=" + output.toAbsolutePath());
    }

    private JsonNode sorted(JsonNode node) {
        if (node.isArray()) {
            var output = mapper.createArrayNode();
            node.forEach(element -> output.add(sorted(element)));
            return output;
        }
        if (node.isObject()) {
            var output = mapper.createObjectNode();
            var names = new ArrayList<String>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            names.forEach(name -> output.set(name, sorted(node.get(name))));
            return output;
        }
        return node;
    }
}
