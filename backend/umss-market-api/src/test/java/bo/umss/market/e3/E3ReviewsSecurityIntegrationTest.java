package bo.umss.market.e3;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = E3ReviewsServiceApplication.class,
    properties = {
        "E3_INTERNAL_TOKEN=e3-test-token-only-not-for-production-123456789",
        "spring.datasource.url=jdbc:h2:mem:e3_service_security;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@AutoConfigureMockMvc
class E3ReviewsSecurityIntegrationTest {

    private static final String TOKEN =
        "e3-test-token-only-not-for-production-123456789";

    @Autowired
    private MockMvc mvc;

    private String url() {
        return "/internal/e3/reviews/" + UUID.randomUUID();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mvc.perform(get(url()))
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsIncorrectToken() throws Exception {
        mvc.perform(get(url())
                .header("X-E3-Internal-Token", "token-incorrecto"))
            .andExpect(status().isForbidden());
    }

    @Test
    void validTokenReachesReviewsController() throws Exception {
        mvc.perform(get(url())
                .header("X-E3-Internal-Token", TOKEN))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPostWithoutToken() throws Exception {
        String json = """
            {
              "buyerId": "00000000-0000-0000-0000-000000000001",
              "review": {
                "rating": 5,
                "comment": "Excelente"
              }
            }
            """;

        mvc.perform(post(url())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isForbidden());
    }
}
