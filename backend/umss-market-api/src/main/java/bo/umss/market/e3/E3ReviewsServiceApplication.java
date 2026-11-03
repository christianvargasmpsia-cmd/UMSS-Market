package bo.umss.market.e3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import bo.umss.market.umss_market_api.application.usecases.ProductReviewsUseCase;
import bo.umss.market.umss_market_api.application.usecases.SummarizeProductReviewsUseCase;
import bo.umss.market.umss_market_api.infrastructure.adapters.JpaPublicationRepositoryAdapter;
import bo.umss.market.umss_market_api.infrastructure.adapters.JpaReviewRepositoryAdapter;
import bo.umss.market.umss_market_api.infrastructure.adapters.JpaUserRepositoryAdapter;
import bo.umss.market.umss_market_api.infrastructure.adapters.OllamaReviewSummaryAdapter;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ProductReviewEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaReviewRepository;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = ProductReviewEntity.class)
@EnableJpaRepositories(basePackageClasses = JpaReviewRepository.class)
@Import({
    ProductReviewsUseCase.class,
    SummarizeProductReviewsUseCase.class,
    JpaPublicationRepositoryAdapter.class,
    JpaReviewRepositoryAdapter.class,
    JpaUserRepositoryAdapter.class,
    OllamaReviewSummaryAdapter.class,
    E3InternalReviewsController.class
})
public class E3ReviewsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            E3ReviewsServiceApplication.class,
            args
        );
    }

    @Bean
    public SecurityFilterChain e3Security(
        HttpSecurity http,
        @Value("${E3_INTERNAL_TOKEN:}") String sharedToken
    ) throws Exception {

        if (sharedToken == null || sharedToken.length() < 32) {
            throw new IllegalStateException(
                "E3_INTERNAL_TOKEN debe tener al menos 32 caracteres"
            );
        }

        return http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/e3/reviews/**")
                .access((authentication, context) -> {
                    String provided = context.getRequest()
                        .getHeader("X-E3-Internal-Token");

                    boolean valid = provided != null
                        && MessageDigest.isEqual(
                            sharedToken.getBytes(StandardCharsets.UTF_8),
                            provided.getBytes(StandardCharsets.UTF_8)
                        );

                    return new AuthorizationDecision(valid);
                })
                .anyRequest().denyAll()
            )
            .build();
    }
}
