package bo.umss.market.e3;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.*;

@SpringBootTest(
    classes = E3ReviewsServiceApplication.class,
    properties = {
        "E3_INTERNAL_TOKEN=e3-test-token-only-not-for-production-123456789",
        "spring.datasource.url=jdbc:h2:mem:e3_business;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@AutoConfigureMockMvc
@Transactional
class E3ReviewsBusinessIntegrationTest {

    private static final String TOKEN =
        "e3-test-token-only-not-for-production-123456789";

    @Autowired MockMvc mvc;
    @Autowired JpaUserRepository users;
    @Autowired JpaPublicationRepository publications;
    @Autowired JpaReviewRepository reviews;

    private UUID buyerId;
    private UUID publicationId;

    private String url() {
        return "/internal/e3/reviews/" + publicationId;
    }

    @BeforeEach
    void setup() {
        buyerId = UUID.randomUUID();
        publicationId = UUID.randomUUID();

        users.saveAndFlush(
            UserEntity.builder()
                .id(buyerId)
                .ru(buyerId.toString())
                .nombre("Comprador E3")
                .email(buyerId + "@example.invalid")
                .passwordHash(UUID.randomUUID().toString())
                .build()
        );

        publications.saveAndFlush(
            PublicationEntity.builder()
                .id(publicationId)
                .storeId(UUID.randomUUID())
                .nombre("Producto E3")
                .precio(BigDecimal.TEN)
                .activa(true)
                .stock(4)
                .build()
        );
    }

    @Test
    void createsAndListsReviewsWithValidToken() throws Exception {
        String body = """
            {
              "buyerId": "%s",
              "review": {
                "rating": 5,
                "comment": "Excelente calidad"
              }
            }
            """.formatted(buyerId);

        mvc.perform(post(url())
                .header("X-E3-Internal-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.publicationId")
                .value(publicationId.toString()))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.comment")
                .value("Excelente calidad"))
            .andExpect(jsonPath("$.buyerId").doesNotExist());

        assertEquals(1, reviews.count());

        mvc.perform(get(url())
                .header("X-E3-Internal-Token", TOKEN)
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].rating").value(5))
            .andExpect(jsonPath("$[0].comment")
                .value("Excelente calidad"));
    }

    @Test
    void returnsNoReviewsSummaryWithoutCallingOllama() throws Exception {
        mvc.perform(get(url() + "/summary")
                .header("X-E3-Internal-Token", TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_REVIEWS"))
            .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mvc.perform(get(url())
                .header("X-E3-Internal-Token", TOKEN)
                .param("limit", "51"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownPublication() throws Exception {
        mvc.perform(get(
                    "/internal/e3/reviews/" + UUID.randomUUID()
                )
                .header("X-E3-Internal-Token", TOKEN))
            .andExpect(status().isNotFound());
    }
}
