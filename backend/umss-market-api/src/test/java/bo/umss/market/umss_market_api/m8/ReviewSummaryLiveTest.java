package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.OllamaReviewSummaryAdapter;
import bo.umss.market.umss_market_api.domain.ports.ReviewSummaryPort;
@Tag("ollama")
class ReviewSummaryLiveTest {
    @Test void generatesBoundedSummaryForFiveMixedReviews() {
        var texts=List.of("El producto llego con rayones", "El producto es facil de usar",
                "Tiene buen acabado y cumple con lo descrito", "La calidad del producto es buena",
                "El precio es elevado y la entrega fue lenta", "El producto llego roto",
                "El empaque era insuficiente y la entrega se retraso", "El producto tiene excelente calidad",
                "Los materiales son resistentes y la entrega fue puntual");
        int[] ratings={2,4,4,3,3,1,1,5,5};
        var evidence=new java.util.ArrayList<ReviewSummaryPort.Evidence>();
        for(int i=0;i<texts.size();i++) evidence.add(new ReviewSummaryPort.Evidence(i,texts.get(i),ratings[i]));
        var result=new OllamaReviewSummaryAdapter("http://localhost:11434","qwen2.5:1.5b",45000).select(evidence);
        System.out.println("Qwen five reviews: "+result);
        assertFalse(result.positive().isEmpty());assertFalse(result.negative().isEmpty());
        var used=new java.util.HashSet<Integer>();
        for(int index:result.positive()) {assertTrue(index>=0 && index<texts.size());assertTrue(used.add(index));}
        for(int index:result.negative()) {assertTrue(index>=0 && index<texts.size());assertTrue(used.add(index));}
        var product=java.util.UUID.randomUUID();
        var products=org.mockito.Mockito.mock(bo.umss.market.umss_market_api.application.usecases.ProductReviewsUseCase.class);
        var repository=org.mockito.Mockito.mock(bo.umss.market.umss_market_api.domain.ports.ReviewRepositoryPort.class);
        var reviews=new java.util.ArrayList<bo.umss.market.umss_market_api.domain.model.ProductReview>();
        for(int[] range:List.of(new int[]{0,1},new int[]{1,3},new int[]{3,5},new int[]{5,7},new int[]{7,9})) {
            reviews.add(new bo.umss.market.umss_market_api.domain.model.ProductReview(java.util.UUID.randomUUID(),
                    product,java.util.UUID.randomUUID(),ratings[range[0]],
                    String.join(". ",texts.subList(range[0],range[1])),java.time.Instant.now()));
        }
        org.mockito.Mockito.when(repository.findLatestByPublicationId(product,10)).thenReturn(reviews);
        var summary=new bo.umss.market.umss_market_api.application.usecases.SummarizeProductReviewsUseCase(
                products,repository,ignored->result).execute(product);
        assertEquals("GENERATED",summary.status());assertEquals(5,summary.reviewCount());
        assertTrue(summary.positiveAspects().size()<=3);assertTrue(summary.negativeAspects().size()<=3);
    }
    @Test void classifiesRealReviewFragmentsWithQwen() {
        var adapter=new OllamaReviewSummaryAdapter("http://localhost:11434","qwen2.5:1.5b",45000);
        long start=System.nanoTime();
        var result=adapter.select(List.of(new ReviewSummaryPort.Evidence(0,"La calidad es excelente",5),
                new ReviewSummaryPort.Evidence(1,"El producto llego roto y no funciona",1)));
        assertEquals(List.of(0),result.positive());assertEquals(List.of(1),result.negative());
        System.out.println("Qwen reviews real: "+(System.nanoTime()-start)/1_000_000+" ms; "+result);
    }
}
