
package bo.umss.market.umss_market_api.infrastructure.controllers;
import bo.umss.market.umss_market_api.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthControllerTest {

    private final MockMvc mockMvc =
    MockMvcBuilders.standaloneSetup(
        new HealthController()
    )
    .setControllerAdvice(new GlobalExceptionHandler())
    .build();

    @Test
    void healthReturnsStatusAndService() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value(
                "umss-market-api"));
    }

    @Test
    void healthRejectsPostMethod() throws Exception {
        mockMvc.perform(post("/api/health"))
            .andExpect(status().isMethodNotAllowed());
    }
}
