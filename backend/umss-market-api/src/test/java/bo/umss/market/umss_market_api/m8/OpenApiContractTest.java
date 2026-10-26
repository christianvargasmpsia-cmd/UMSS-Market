package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.file.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void exportsGeneratedContractAndChecksRecommendationsSecurityAndLimits() throws Exception {
        JsonNode spec=mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        for(String entry:new String[]{"/api/recommendations|get", "/api/interactions|post",
                "/api/interactions|get", "/api/interactions/{id}|get", "/api/interactions/{id}|delete"}) {
            String[] parts=entry.split("\\|");
            JsonNode operation=spec.path("paths").path(parts[0]).path(parts[1]);
            assertFalse(operation.isMissingNode(),entry);
            assertTrue(operation.path("security").toString().contains("bearerAuth"),entry);
        }
        JsonNode operation=spec.path("paths").path("/api/recommendations").path("get");
        JsonNode limit=operation.path("parameters").get(0).path("schema");
        assertEquals(1,limit.path("minimum").asInt());
        assertEquals(50,limit.path("maximum").asInt());
        assertEquals(10,limit.path("default").asInt());
        for(String code:new String[]{"200","400","401","403"}) assertTrue(operation.path("responses").has(code));
        assertEquals("bearer",spec.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText());
        assertEquals("#/components/schemas/RecommendationItem",spec.path("components").path("schemas")
                .path("RecommendationsResponse").path("properties").path("items").path("items").path("$ref").asText());
        JsonNode item=spec.path("components").path("schemas").path("RecommendationItem").path("properties");
        for(String field:new String[]{"id","name","price","stock","storeId","affinity"}) assertTrue(item.has(field),field);
        assertFalse(item.has("quantity"),"Do not collide with checkout's nested Item schema");
        Path output=Path.of("target/openapi.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted(spec))+"\n");
    }

    private JsonNode sorted(JsonNode node) {
        if(node.isObject()) {
            ObjectNode result=mapper.createObjectNode();
            java.util.TreeSet<String> names=new java.util.TreeSet<>();
            node.fieldNames().forEachRemaining(names::add);
            names.forEach(name->result.set(name,sorted(node.get(name))));
            return result;
        }
        if(node.isArray()) {
            var result=mapper.createArrayNode();
            node.forEach(value->result.add(sorted(value)));
            return result;
        }
        return node;
    }
}
