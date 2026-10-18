package bo.umss.market.umss_market_api.infrastructure.graphql;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

/** Limites defensivos para el endpoint GraphQL publico. */
@Configuration
public class GraphqlProtectionConfig {
    @Bean
    public Instrumentation catalogGraphqlInstrumentation() {
        return new ChainedInstrumentation(List.of(
                new MaxQueryDepthInstrumentation(6),
                new MaxQueryComplexityInstrumentation(200)));
    }
}
