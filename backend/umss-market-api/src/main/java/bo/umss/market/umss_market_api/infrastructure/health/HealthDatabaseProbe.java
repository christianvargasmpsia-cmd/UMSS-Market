package bo.umss.market.umss_market_api.infrastructure.health;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class HealthDatabaseProbe {
    private final JdbcTemplate jdbc;

    public HealthDatabaseProbe(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        // Local to this probe; does not alter business query timeouts.
        this.jdbc.setQueryTimeout(5);
    }

    public boolean isUp() {
        try {
            return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
        } catch (DataAccessException exception) {
            return false;
        }
    }
}
