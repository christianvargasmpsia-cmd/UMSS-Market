package bo.umss.market.umss_market_api.m8;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Reuses all HTTP/JWT scenarios on an explicitly isolated PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "DEMO_POSTGRES_URL", matches = "jdbc:postgresql://[^/]+/umss_recommendations_test")
class RecommendationsPostgresTest extends RecommendationsIntegrationTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("DEMO_POSTGRES_URL"));
        properties.add("spring.datasource.username", () -> System.getenv("DEMO_POSTGRES_USER"));
        properties.add("spring.datasource.password", () -> System.getenv("DEMO_POSTGRES_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
