package bo.umss.market.umss_market_api.application.usecases;

import bo.umss.market.umss_market_api.application.services.*;
import bo.umss.market.umss_market_api.domain.ports.AIProviderPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatInputPolicyTest {
    @Test void boundaries() {
        assertFalse(ChatInputPolicy.exceedsLimit(null));
        assertFalse(ChatInputPolicy.exceedsLimit(""));
        assertFalse(ChatInputPolicy.exceedsLimit("x".repeat(4096)));
        assertTrue(ChatInputPolicy.exceedsLimit("x".repeat(4097)));
        assertTrue(ChatInputPolicy.exceedsLimit(" ".repeat(4097)));
        assertFalse(ChatInputPolicy.exceedsLimit("😀".repeat(2048)));
        assertTrue(ChatInputPolicy.exceedsLimit("😀".repeat(2049)));
    }

    @Test void rejectsBeforeProviderOrCatalogEvenWhenAiDisabled() {
        for (boolean enabled : new boolean[]{true,false}) {
            var provider = mock(AIProviderPort.class);
            var catalog = mock(SearchCatalogUseCase.class);
            var service = new AIServiceImpl(provider,catalog);
            ReflectionTestUtils.setField(service,"iaEnabled",enabled);
            for (String input : new String[]{"buscar "+"x".repeat(4097)," ".repeat(4097)}) {
                assertTrue(service.chat(input).contains("AI_INPUT_TOO_LONG"));
                verifyNoInteractions(provider,catalog);
            }
        }
    }

    @Test void acceptsBoundaryAndHandlesNullWithoutModel() {
        var provider = mock(AIProviderPort.class);
        var catalog = mock(SearchCatalogUseCase.class);
        var service = new AIServiceImpl(provider,catalog);
        ReflectionTestUtils.setField(service,"iaEnabled",true);
        service.chat(null);
        verifyNoInteractions(provider,catalog);
        String allowed = "x".repeat(4096);
        service.chat(allowed);
        verify(provider).selectTool(allowed);
    }
}
