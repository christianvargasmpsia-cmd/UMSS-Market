package bo.umss.market.umss_market_api.infrastructure.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import bo.umss.market.umss_market_api.infrastructure.health.HealthDatabaseProbe;

class HealthControllerTest {
    @Test
    void reportsHealthyDatabase() throws Exception {
        assertHealth(true, 200, "UP");
    }

    @Test
    void reportsUnavailableDatabaseWithoutInternalDetails() throws Exception {
        assertHealth(false, 503, "DOWN");
    }

    private void assertHealth(boolean up, int status, String databaseStatus) throws Exception {
        HealthDatabaseProbe database = mock(HealthDatabaseProbe.class);
        when(database.isUp()).thenReturn(up);
        MockMvcBuilders.standaloneSetup(new HealthController(database)).build()
                .perform(get("/api/health"))
                .andExpect(status().is(status))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().json("{\"application\":\"UP\",\"database\":\""
                        + databaseStatus + "\"}", true));
        verify(database).isUp();
    }
}
