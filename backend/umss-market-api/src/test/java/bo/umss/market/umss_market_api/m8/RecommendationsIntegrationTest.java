package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import bo.umss.market.umss_market_api.domain.enums.InteractionType;
import bo.umss.market.umss_market_api.domain.ports.AIProviderPort;
import bo.umss.market.umss_market_api.domain.ports.RecommendationTextPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.*;
import bo.umss.market.umss_market_api.infrastructure.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendationsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JpaPublicationRepository publications;
    @Autowired JpaInteractionRepository interactions;
    @Autowired JwtService jwt;
    @MockitoBean AIProviderPort ai;
    @MockitoBean RecommendationTextPort explanation;
    UUID buyer = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    String auth(UUID user) { return "Bearer " + jwt.generateToken(user,"buyer@example.invalid","COMPRADOR"); }
    PublicationEntity product(String name, boolean active, int stock) {
        return publications.saveAndFlush(PublicationEntity.builder().id(UUID.randomUUID())
                .storeId(UUID.randomUUID()).nombre(name).precio(BigDecimal.TEN).activa(active).stock(stock).build());
    }
    InteractionEntity history(UUID user, PublicationEntity product) {
        return interactions.saveAndFlush(InteractionEntity.builder().userId(user).publicationId(product.getId())
                .type(InteractionType.VIEW).build());
    }
    @Test void persistsDistinctInterestsAndReturnsOnlyAuthenticatedBuyersRecommendations() throws Exception {
        var book=product("libro algebra",true,5); var ball=product("balon futbol",true,5);
        var book2=product("libro calculo",true,5); var ball2=product("balon deporte",true,5);
        product("libro agotado",true,0); product("libro inactivo",false,9);
        history(buyer,book); history(other,ball);
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)).param("limit","1")
                .param("userId",other.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalized").value(true))
                .andExpect(jsonPath("$.items[0].id").value(book2.getId().toString()));
        mvc.perform(get("/api/recommendations").header("Authorization",auth(other)).param("limit","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(ball2.getId().toString()));
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(3));
    }
    @Test void generalRecommendationsRemainAvailableDuringAiOutage() throws Exception {
        when(explanation.explain(anyList(),anyBoolean())).thenThrow(new IllegalStateException("Ollama offline"));
        when(ai.generateEmbedding(anyString())).thenThrow(new IllegalStateException("Embeddings offline"));
        var available=product("Libro",true,3);
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalized").value(false))
                .andExpect(jsonPath("$.items[0].id").value(available.getId().toString()))
                .andExpect(jsonPath("$.aiStatus").value("FALLBACK"))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
        verify(explanation).explain(java.util.List.of("Libro"),false);
        verifyNoInteractions(ai);
    }
    @Test void returnsGeneratedExplanationAndSkipsAiForEmptyCatalog() throws Exception {
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiStatus").value("NOT_REQUESTED"));
        verifyNoInteractions(explanation);
        product("Libro",true,3);
        when(explanation.explain(anyList(),anyBoolean())).thenReturn("Opciones disponibles para explorar.");
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiStatus").value("GENERATED"))
                .andExpect(jsonPath("$.explanation").value("Opciones disponibles para explorar."));
    }
    @Test void rejectsAnonymousInvalidTokenAndInvalidLimits() throws Exception {
        mvc.perform(get("/api/recommendations")).andExpect(status().isForbidden());
        mvc.perform(get("/api/recommendations").header("Authorization","Bearer invalid"))
                .andExpect(status().isUnauthorized());
        for(String limit:new String[]{"0","51","abc"}) mvc.perform(get("/api/recommendations")
                .header("Authorization",auth(buyer)).param("limit",limit)).andExpect(status().isBadRequest());
    }
    @Test void cannotReadOrDeleteAnotherBuyersPersistedInteraction() throws Exception {
        var entry=history(other,product("Libro",true,5));
        String url="/api/interactions/"+entry.getId();
        mvc.perform(get(url)).andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization",auth(buyer))).andExpect(status().isNotFound());
        mvc.perform(delete(url).header("Authorization",auth(buyer))).andExpect(status().isNotFound());
        assertTrue(interactions.existsById(entry.getId()));
        mvc.perform(get(url).header("Authorization",auth(other))).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(other.toString()));
        mvc.perform(get("/api/interactions").header("Authorization",auth(buyer))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete(url).header("Authorization",auth(other))).andExpect(status().isNoContent());
        mvc.perform(get(url).header("Authorization",auth(other))).andExpect(status().isNotFound());
        mvc.perform(delete(url).header("Authorization",auth(other))).andExpect(status().isNotFound());
    }
    @Test void createsHistoryForJwtOwnerEvenIfPayloadClaimsAnotherUser() throws Exception {
        var p=product("Libro",true,5);
        String body="{\"publicationId\":\""+p.getId()+"\",\"type\":\"FAVORITE\",\"userId\":\""+other+"\"}";
        mvc.perform(post("/api/interactions").header("Authorization",auth(buyer))
                .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(buyer.toString()));
        assertEquals(1,interactions.findByUserId(buyer).size());
        assertTrue(interactions.findByUserId(other).isEmpty());
    }
    @Test void jwtUsesConfiguredKeyAndParsesIdentity() {
        String token=jwt.generateToken(buyer,"buyer@example.invalid","COMPRADOR");
        assertEquals(buyer,jwt.extractUserId(token));
        assertEquals("buyer@example.invalid",jwt.extractEmail(token));
        assertTrue(jwt.isTokenValid(token));
        assertFalse(jwt.isTokenValid("invalid"));
    }

    @Test void postingInteractionChangesRecommendationsAndOtherBuyerRemainsGeneral() throws Exception {
        var source=product("tecnologia computadora",true,4);
        var related=product("tecnologia portatil",true,4);
        product("libro poesia",true,4);
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalized").value(false));
        mvc.perform(post("/api/interactions").header("Authorization",auth(buyer))
                .contentType("application/json").content("{\"publicationId\":\""+source.getId()+"\",\"type\":\"FAVORITE\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)).param("limit","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalized").value(true))
                .andExpect(jsonPath("$.items[0].id").value(related.getId().toString()));
        mvc.perform(get("/api/recommendations").header("Authorization",auth(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.personalized").value(false));
    }

    @Test void requestedLimitsAreAppliedAfterAvailabilityFiltering() throws Exception {
        for(int i=0;i<12;i++) product("Disponible "+i,true,5);
        product("Inactivo",false,5); product("Agotado",true,0);
        for(int limit:new int[]{1,5,10}) {
            mvc.perform(get("/api/recommendations").header("Authorization",auth(buyer)).param("limit",""+limit))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(limit));
        }
    }
}
