package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.enums.Role;
import bo.umss.market.umss_market_api.domain.ports.*;
import bo.umss.market.umss_market_api.domain.ports.DuplicateCatalogPort.Candidate;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.application.usecases.DetectDuplicatePublicationsUseCase;
class DuplicateDetectionUnitTest {
    StoreRepositoryPort stores=mock(StoreRepositoryPort.class);
    UserRepositoryPort users=mock(UserRepositoryPort.class);
    PublicationRepositoryPort publications=mock(PublicationRepositoryPort.class);
    DuplicateCatalogPort catalog=mock(DuplicateCatalogPort.class);
    DuplicateEmbeddingPort ai=mock(DuplicateEmbeddingPort.class);
    UUID user=UUID.randomUUID(),store=UUID.randomUUID();
    DuplicateCheckRequest request=new DuplicateCheckRequest("Laptop","Equipo para estudiar",null);
    DetectDuplicatePublicationsUseCase service(double threshold,int limit) {
        return new DetectDuplicatePublicationsUseCase(stores,users,publications,catalog,ai,threshold,limit);
    }
    @BeforeEach void setup() {
        when(users.findById(user)).thenReturn(Optional.of(User.builder().id(user).role(Role.EMPRENDEDOR).build()));
        when(stores.findById(store)).thenReturn(Optional.of(Store.builder().id(store).userId(user).build()));
    }
    Candidate candidate(String title,String desc) {return new Candidate(UUID.randomUUID(),title,desc);}
    void status(int code,Runnable action) {assertEquals(code,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value());}
    @Test void ranksIdenticalSimilarAndDifferentTextsAndAppliesInclusiveThresholdAndLimit() {
        var exact=candidate(request.title(),request.description());var similar=candidate("Notebook","Computadora para clases");
        var different=candidate("Pan","Pan artesanal");
        when(catalog.findByStore(store)).thenReturn(List.of(different,similar,exact));
        when(ai.embed(anyList())).thenReturn(List.of(List.of(1d,0d),List.of(0d,1d),List.of(0.8,0.6)));
        var result=service(.8,5).execute(user,store,request);
        assertEquals(List.of(exact.id(),similar.id()),result.matches().stream().map(DuplicateCheckResponse.Match::publicationId).toList());
        assertEquals(1,result.matches().get(0).similarity());assertEquals(.8,result.matches().get(1).similarity(),1e-10);
        assertEquals(.8,result.threshold());assertEquals(5,result.maxResults());assertEquals(store,result.storeId());
        assertEquals("EVALUATED",result.status());assertTrue(result.message().contains("revision humana"));
        assertEquals(1,service(.8,1).execute(user,store,request).matches().size());
        assertEquals(1,service(1,5).execute(user,store,request).matches().size());
        verifyNoInteractions(publications);verify(catalog,times(3)).findByStore(store);
    }
    @Test void stableTiesExclusionEmptyShopAndNoAutomaticChanges() {
        UUID a=UUID.fromString("00000000-0000-0000-0000-000000000001"),b=UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(catalog.findByStore(store)).thenReturn(List.of(new Candidate(b,"Laptop",request.description()),new Candidate(a,"Laptop",request.description())));
        when(ai.embed(anyList())).thenReturn(List.of(List.of(1d,0d)));
        assertEquals(a,service(.9,5).execute(user,store,request).matches().getFirst().publicationId());
        when(publications.findById(a)).thenReturn(Optional.of(Publication.builder().id(a).storeId(store).build()));
        var update=new DuplicateCheckRequest(request.title(),request.description(),a);
        assertEquals(List.of(b),service(.9,5).execute(user,store,update).matches().stream().map(DuplicateCheckResponse.Match::publicationId).toList());
        when(catalog.findByStore(store)).thenReturn(List.of(new Candidate(a,"Laptop",request.description())));
        clearInvocations(ai);
        assertTrue(service(.9,5).execute(user,store,update).matches().isEmpty());
        when(catalog.findByStore(store)).thenReturn(List.of());
        assertTrue(service(.9,5).execute(user,store,request).matches().isEmpty());verifyNoInteractions(ai);
        verify(publications,never()).save(any());verify(publications,never()).deleteById(any());
    }
    @Test void enforcesIdentityRoleOwnershipAndUpdateOwnershipBeforeEmbedding() {
        var service=service(.9,5);
        status(401,()->service.execute(null,store,request));status(401,()->service.execute(UUID.randomUUID(),store,request));
        when(users.findById(user)).thenReturn(Optional.of(User.builder().role(Role.COMPRADOR).build()));
        status(403,()->service.execute(user,store,request));setup();
        status(404,()->service.execute(user,UUID.randomUUID(),request));
        when(stores.findById(store)).thenReturn(Optional.of(Store.builder().userId(UUID.randomUUID()).build()));
        status(403,()->service.execute(user,store,request));setup();
        UUID id=UUID.randomUUID();var update=new DuplicateCheckRequest("Laptop","Equipo",id);
        status(404,()->service.execute(user,store,update));
        when(publications.findById(id)).thenReturn(Optional.of(Publication.builder().storeId(UUID.randomUUID()).build()));
        status(403,()->service.execute(user,store,update));verifyNoInteractions(catalog,ai);
    }
    @Test void validatesRequestAndExternalConfiguration() {
        for(var bad:Arrays.asList(null,new DuplicateCheckRequest(null,"x",null),new DuplicateCheckRequest(" ","x",null),
                new DuplicateCheckRequest("x".repeat(201),"x",null),new DuplicateCheckRequest("x",null,null),
                new DuplicateCheckRequest("x"," ",null),new DuplicateCheckRequest("x","x".repeat(2001),null)))
            assertThrows(IllegalArgumentException.class,()->service(.9,5).execute(user,store,bad));
        for(double value:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-.1,1.1}) assertThrows(IllegalArgumentException.class,()->service(value,5));
        assertThrows(IllegalArgumentException.class,()->service(.9,0));assertThrows(IllegalArgumentException.class,()->service(.9,51));
        assertTrue(service(0,50).execute(user,store,new DuplicateCheckRequest("x".repeat(200),"x".repeat(2000),null)).matches().isEmpty());
    }
    @Test void invalidEmbeddingNeverMeansNoDuplicates() {
        when(catalog.findByStore(store)).thenReturn(List.of(candidate("Otro",null)));
        var invalid=new ArrayList<List<List<Double>>>();invalid.add(null);invalid.add(List.of());
        invalid.add(Arrays.asList(null,List.of(1d)));invalid.add(List.of(List.of(),List.of(1d)));
        invalid.add(List.of(List.of(0d),List.of(1d)));invalid.add(List.of(Arrays.asList((Double)null),List.of(1d)));
        invalid.add(List.of(List.of(Double.NaN),List.of(1d)));invalid.add(List.of(List.of(1d),List.of(1d,0d)));
        for(var batch:invalid) {when(ai.embed(anyList())).thenReturn(batch);status(503,()->service(.9,5).execute(user,store,request));}
        when(ai.embed(anyList())).thenThrow(new IllegalStateException("offline"));
        status(503,()->service(.9,5).execute(user,store,request));
    }
    @Test void largeVectorsAreStableAndNegativeSimilarityIsExcluded() {
        when(catalog.findByStore(store)).thenReturn(List.of(candidate(null,"Descripcion")));
        when(ai.embed(anyList())).thenReturn(List.of(List.of(Double.MAX_VALUE,Double.MAX_VALUE),List.of(-Double.MAX_VALUE,-Double.MAX_VALUE)));
        assertTrue(service(0,5).execute(user,store,request).matches().isEmpty());
        when(ai.embed(anyList())).thenReturn(List.of(List.of(Double.MAX_VALUE,Double.MAX_VALUE),List.of(Double.MAX_VALUE,Double.MAX_VALUE)));
        assertEquals(1,service(.9,5).execute(user,store,request).matches().getFirst().similarity(),1e-12);
    }
}
