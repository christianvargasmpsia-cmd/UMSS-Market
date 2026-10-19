package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.DuplicateEmbeddingAdapter;
import bo.umss.market.umss_market_api.application.usecases.DetectDuplicatePublicationsUseCase;
import bo.umss.market.umss_market_api.application.dto.DuplicateCheckRequest;
import bo.umss.market.umss_market_api.domain.ports.*;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.enums.Role;
@Tag("ollama")
class DuplicateEmbeddingLiveTest {
    @Test void realNomicFindsIdenticalAndSimilarButNotDifferentPublication() {
        UUID owner=UUID.randomUUID(),shop=UUID.randomUUID(),exact=UUID.randomUUID(),similar=UUID.randomUUID(),different=UUID.randomUUID();
        var stores=mock(StoreRepositoryPort.class);var users=mock(UserRepositoryPort.class);
        var publications=mock(PublicationRepositoryPort.class);var catalog=mock(DuplicateCatalogPort.class);
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().role(Role.EMPRENDEDOR).build()));
        when(stores.findById(shop)).thenReturn(Optional.of(Store.builder().userId(owner).build()));
        var request=new DuplicateCheckRequest("Laptop Lenovo ThinkPad","Computadora portatil con 16 GB de RAM y disco SSD de 512 GB",null);
        when(catalog.findByStore(shop)).thenReturn(List.of(new DuplicateCatalogPort.Candidate(exact,request.title(),request.description()),
                new DuplicateCatalogPort.Candidate(similar,"Notebook Lenovo ThinkPad","Portatil con memoria RAM de 16 GB y almacenamiento SSD de 512 GB"),
                new DuplicateCatalogPort.Candidate(different,"Pan integral","Pan artesanal fresco de harina integral")));
        var service=new DetectDuplicatePublicationsUseCase(stores,users,publications,catalog,
                new DuplicateEmbeddingAdapter("http://localhost:11434","nomic-embed-text",45000),.9,5);
        long start=System.nanoTime();var result=service.execute(owner,shop,request);
        System.out.println("Nomic real: "+(System.nanoTime()-start)/1_000_000+" ms; "+result);
        assertEquals(2,result.matches().size());assertEquals(exact,result.matches().getFirst().publicationId());
        assertEquals(1,result.matches().getFirst().similarity());assertEquals(similar,result.matches().get(1).publicationId());
    }
}
