package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.OllamaRecommendationAdapter;

/** Optional real-model smoke test; no application database or customer data. */
@Tag("ollama")
class OllamaRecommendationLiveTest {
    @Test void generatesExplanationWithInstalledQwen() {
        var adapter=new OllamaRecommendationAdapter("http://localhost:11434","qwen2.5:1.5b",20000);
        long start=System.nanoTime();
        String text=adapter.explain(List.of("Libro de algebra", "Libro de calculo"),true);
        assertFalse(text.isBlank());
        assertTrue(text.length()<=1500);
        System.out.println("Qwen real: " + ((System.nanoTime()-start)/1_000_000) + " ms; " + text);
    }
}
