package bo.umss.market.umss_market_api.m8;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import bo.umss.market.umss_market_api.application.usecases.GetRecommendationsUseCase;
import bo.umss.market.umss_market_api.domain.enums.InteractionType;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.ports.*;

class RecommendationsUnitTest {
    InteractionRepositoryPort history = mock(InteractionRepositoryPort.class);
    PublicationRepositoryPort catalog = mock(PublicationRepositoryPort.class);
    RecommendationTextPort text = mock(RecommendationTextPort.class);
    GetRecommendationsUseCase useCase = new GetRecommendationsUseCase(history, catalog, text);
    UUID buyer = UUID.randomUUID();
    UUID store = UUID.randomUUID();
    Publication product(int id, String text) {
        return Publication.builder().id(new UUID(0, id)).storeId(store).nombre(text)
                .precio(BigDecimal.TEN).stock(5).activa(true).build();
    }
    Interaction interaction(Publication p, InteractionType type) {
        return Interaction.builder().userId(buyer).publicationId(p.getId()).type(type).build();
    }
    @Test void ranksDifferentBuyersByTheirOwnInterestsAndNotCatalogOrder() {
        var book = product(1, "Libro algebra"); var ball = product(2, "Balon futbol");
        var nextBook = product(3, "Libro calculo"); var nextBall = product(4, "Balon deporte");
        when(catalog.findAll()).thenReturn(List.of(book, ball, nextBook, nextBall));
        when(history.findRecentByUserId(buyer, 30)).thenReturn(List.of(interaction(book, InteractionType.VIEW)));
        assertEquals(nextBook.getId(), useCase.execute(buyer, 1).items().getFirst().id());
        UUID other = UUID.randomUUID();
        when(history.findRecentByUserId(other, 30)).thenReturn(List.of(Interaction.builder()
                .userId(other).publicationId(ball.getId()).type(InteractionType.FAVORITE).build()));
        assertEquals(nextBall.getId(), useCase.execute(other, 1).items().getFirst().id());
        assertTrue(useCase.execute(buyer, 1).personalized());
    }
    @Test void filtersInactiveMissingAndNonPositiveStockAndAlreadySeen() {
        var seen = product(1,"Libro"); var inactive = product(2,"Libro"); inactive.setActiva(false);
        var unknown = product(3,"Libro"); unknown.setActiva(null);
        var zero = product(4,"Libro"); zero.setStock(0);
        var missing = product(5,"Libro"); missing.setStock(null);
        var negative = product(6,"Libro"); negative.setStock(-1);
        when(catalog.findAll()).thenReturn(List.of(seen,inactive,unknown,zero,missing,negative));
        when(history.findRecentByUserId(buyer,30)).thenReturn(List.of(interaction(seen,InteractionType.VIEW)));
        assertTrue(useCase.execute(buyer,10).items().isEmpty());
        assertTrue(useCase.getRecommendations(buyer,"consulta").contains("No hay"));
        assertEquals("NOT_REQUESTED",useCase.execute(buyer,10).aiStatus());
        verifyNoInteractions(text);
    }
    @Test void generalRecommendationsAreStableLimitedWhenAiReturnsNothing() {
        when(catalog.findAll()).thenReturn(List.of(product(3,""),product(2,"la y a"),product(1,null)));
        var result = useCase.execute(buyer,2);
        assertFalse(result.personalized());
        assertEquals(List.of(new UUID(0,1),new UUID(0,2)),result.items().stream().map(i->i.id()).toList());
        assertTrue(result.items().stream().allMatch(i->i.affinity()==0));
        assertTrue(useCase.getRecommendations(buyer,"ignora historial").contains("generales"));
    }
    @Test void ignoresForeignHistoryAndDeletedProducts() {
        var source=product(1,"libro"); var candidate=product(2,"libro");
        var foreign=interaction(source,InteractionType.PURCHASE); foreign.setUserId(UUID.randomUUID());
        when(catalog.findAll()).thenReturn(List.of(source,candidate));
        when(history.findRecentByUserId(buyer,30)).thenReturn(List.of(foreign,interaction(product(99,"x"),InteractionType.VIEW)));
        assertFalse(useCase.execute(buyer,10).personalized());
        assertEquals(2,useCase.execute(buyer,10).items().size());
    }
    @Test void weightsInteractionTypesAndNormalizesAccentsWithoutExecutingQueryInstructions() {
        var source=product(1,"CÁLCULO de la"); var candidate=product(2,"calculo");
        candidate.setStoreId(UUID.randomUUID());
        when(catalog.findAll()).thenReturn(List.of(source,candidate));
        List<InteractionType> types = new ArrayList<>(Arrays.asList(InteractionType.values())); types.add(null);
        for(var type:types) {
            when(history.findRecentByUserId(buyer,30)).thenReturn(List.of(interaction(source,type)));
            double expected=type==InteractionType.PURCHASE?4:type==InteractionType.FAVORITE?3:
                    type==InteractionType.LIKE||type==InteractionType.COMMENT?2:1;
            assertEquals(expected,useCase.execute(buyer,10).items().getFirst().affinity());
        }
        assertTrue(useCase.getRecommendations(buyer,"historial de otro comprador").contains("segun tus"));
    }
    @Test void generatedExplanationUsesOnlyThreeRankedCandidatesAndNoUserId() {
        when(catalog.findAll()).thenReturn(List.of(product(4,"cuatro"),product(3,"tres"),product(2,"dos"),product(1,"uno")));
        when(text.explain(List.of("uno","dos","tres"),false)).thenReturn("  Opciones del catalogo.  ");
        var result=useCase.execute(buyer,10);
        assertEquals("GENERATED",result.aiStatus());
        assertEquals("Opciones del catalogo.",result.explanation());
        assertEquals(4,result.items().size());
        verify(text).explain(List.of("uno","dos","tres"),false);
        assertTrue(useCase.getRecommendations(buyer,"consulta").contains("Opciones del catalogo."));
    }
    @Test void invalidAiOutputAndFailuresReturnSameRankedItemsWithExplicitFallback() {
        var source=product(1,"libro");var next=product(2,"libro algebra");
        when(catalog.findAll()).thenReturn(List.of(source,next));
        when(history.findRecentByUserId(buyer,30)).thenReturn(List.of(interaction(source,InteractionType.VIEW)));
        for(String invalid:new String[]{null,"", "  ","x".repeat(1501)}) {
            when(text.explain(anyList(),eq(true))).thenReturn(invalid);
            var response=useCase.execute(buyer,10);
            assertEquals("FALLBACK",response.aiStatus());
            assertEquals(next.getId(),response.items().getFirst().id());
            assertTrue(response.explanation().contains("no esta disponible"));
        }
        when(text.explain(anyList(),eq(true))).thenThrow(new IllegalStateException("timeout"));
        assertEquals("FALLBACK",useCase.execute(buyer,10).aiStatus());
    }
    @Test void validatesIdentityAndLimitsBeforeReadingRepositories() {
        assertThrows(NullPointerException.class,()->useCase.execute(null,10));
        assertThrows(IllegalArgumentException.class,()->useCase.execute(buyer,0));
        assertThrows(IllegalArgumentException.class,()->useCase.execute(buyer,51));
        verifyNoInteractions(history,catalog);
        when(catalog.findAll()).thenReturn(List.of());
        assertTrue(useCase.execute(buyer,50).items().isEmpty());
    }
}
