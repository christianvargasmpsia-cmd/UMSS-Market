package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.ports.DuplicateEmbeddingPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.*;
import bo.umss.market.umss_market_api.infrastructure.security.JwtService;
@SpringBootTest(properties={"duplicates.threshold=0.8","duplicates.max-results=1"})
@AutoConfigureMockMvc @Transactional
class DuplicateDetectionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JpaUserRepository users;
    @Autowired JpaStoreRepository stores;
    @Autowired JpaPublicationRepository publications;
    @Autowired JwtService jwt;
    @MockitoBean DuplicateEmbeddingPort ai;
    UUID owner,store,otherOwner,otherStore,own,foreign;
    String url(UUID id) {return "/api/stores/"+id+"/publications/duplicates";}
    String auth(UUID id,String role) {return "Bearer "+jwt.generateToken(id,id+"@example.invalid",role);}
    String body="{\"title\":\"Laptop\",\"description\":\"Equipo para estudiar\"}";
    @BeforeEach void setup() {
        owner=UUID.randomUUID();otherOwner=UUID.randomUUID();store=UUID.randomUUID();otherStore=UUID.randomUUID();
        for(UUID id:List.of(owner,otherOwner)) users.saveAndFlush(UserEntity.builder().id(id).ru(id.toString())
                .nombre("Emprendedor").email(id+"@example.invalid").passwordHash(UUID.randomUUID().toString()).role(Role.EMPRENDEDOR).build());
        stores.saveAndFlush(StoreEntity.builder().id(store).userId(owner).nombre("Propia").build());
        stores.saveAndFlush(StoreEntity.builder().id(otherStore).userId(otherOwner).nombre("Ajena").build());
        own=publication(store,"Laptop","Equipo para estudiar",false);foreign=publication(otherStore,"Ajena","Texto privado",true);
        when(ai.embed(anyList())).thenAnswer(call->{List<String> texts=call.getArgument(0);return texts.stream().map(t->List.of(1d,0d)).toList();});
    }
    UUID publication(UUID shop,String title,String desc,boolean active) {
        UUID id=UUID.randomUUID();publications.saveAndFlush(PublicationEntity.builder().id(id).storeId(shop)
                .nombre(title).descripcion(desc).precio(BigDecimal.TEN).stock(0).activa(active).build());return id;
    }
    @Test void persistedShopScopeIncludesInactiveAndNeverModifiesCatalog() throws Exception {
        long count=publications.count();
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EVALUATED"))
                .andExpect(jsonPath("$.threshold").value(.8)).andExpect(jsonPath("$.maxResults").value(1))
                .andExpect(jsonPath("$.matches.length()").value(1)).andExpect(jsonPath("$.matches[0].publicationId").value(own.toString()))
                .andExpect(jsonPath("$.matches[0].title").value("Laptop")).andExpect(jsonPath("$.matches[0].similarity").value(1));
        verify(ai).embed(argThat(texts->texts.size()==1 && !texts.getFirst().contains("privado")));
        assertEquals(count,publications.count());assertFalse(publications.findById(own).orElseThrow().getActiva());
        assertEquals(0,publications.findById(own).orElseThrow().getStock());
    }
    @Test void authorizationRejectsAnonymousBuyerOtherShopAndSpoofedJwtIdentity() throws Exception {
        mvc.perform(post(url(store)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"COMPRADOR")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(url(store)).header("Authorization",auth(otherOwner,"EMPRENDEDOR")).contentType("application/json")
                .content(body.substring(0,body.length()-1)+",\"userId\":\""+owner+"\"}")).andExpect(status().isForbidden());
        var user=users.findById(owner).orElseThrow();user.setRole(Role.COMPRADOR);users.saveAndFlush(user);
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(ai);
    }
    @Test void exclusionAndEmptyShopSkipAiAndForeignExclusionFails() throws Exception {
        String update=body.substring(0,body.length()-1)+",\"excludePublicationId\":\""+own+"\"}";
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.matches").isEmpty());
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(update.replace(own.toString(),foreign.toString())))
                .andExpect(status().isForbidden());
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(update.replace(own.toString(),UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());verifyNoInteractions(ai);
    }
    @Test void errorsAndValidationAreExplicitRatherThanEmptyMatches() throws Exception {
        when(ai.embed(anyList())).thenThrow(new IllegalStateException("timeout"));
        mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(body))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("EMBEDDINGS_UNAVAILABLE")))
                .andExpect(jsonPath("$.matches").doesNotExist());
        for(String bad:List.of("{}","{","{\"title\":[],\"description\":\"text\"}","{\"title\":\"A\",\"description\":\"B\",\"excludePublicationId\":\"bad\"}"))
            mvc.perform(post(url(store)).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(bad)).andExpect(status().isBadRequest());
        mvc.perform(post(url(UUID.randomUUID())).header("Authorization",auth(owner,"EMPRENDEDOR")).contentType("application/json").content(body)).andExpect(status().isNotFound());
    }
}
