package bo.umss.market.umss_market_api.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import bo.umss.market.umss_market_api.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * Configuración de seguridad de UMSS Market.
 *
 * Responsabilidades:
 *
 * 1. Configurar la autenticación mediante JWT. 2. Definir permisos según el rol
 * del usuario. 3. Proteger las operaciones de compradores y emprendedores. 4.
 * Configurar CORS para el frontend Angular. 5. Deshabilitar las sesiones HTTP
 * para la autenticación. 6. Responder HTTP 401 cuando falta autenticación. 7.
 * Mantener HTTP 403 para usuarios sin autorización.
 *
 * Nota: La validación de propiedad de publicaciones y tiendas debe realizarse
 * también en la lógica de negocio.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Filtro encargado de procesar el token JWT.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {

        FilterRegistrationBean<JwtAuthenticationFilter> registration
                = new FilterRegistrationBean<>(jwtAuthenticationFilter);

        registration.setEnabled(false);

        return registration;
    }

    /**
     * Codificador de contraseñas.
     *
     * BCrypt permite almacenar contraseñas mediante hashes seguros, en lugar de
     * texto plano.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuración CORS.
     *
     * Permite que el frontend Angular, ejecutándose en http://localhost:4200,
     * consuma la API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Origen autorizado del frontend.
        configuration.setAllowedOrigins(
                List.of("http://localhost:4200")
        );

        // Métodos HTTP permitidos.
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Permitir encabezados como Authorization.
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Permitir credenciales en solicitudes CORS.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source
                = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    /**
     * Configuración principal de Spring Security.
     *
     * Define autenticación, autorización y manejo de sesiones para los
     * endpoints de UMSS Market.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * 1. Deshabilitar CSRF.
                 *
                 * La API utiliza JWT en el encabezado
                 * Authorization para autenticación.
                 */
                .csrf(csrf -> csrf.disable())
                /*
                 * 2. Habilitar la configuración CORS.
                 */
                .cors(cors -> {
                })
                /*
                 * 3. Configurar sesiones sin estado.
                 *
                 * STATELESS evita que Spring Security
                 * utilice sesiones HTTP para mantener
                 * el contexto de autenticación.
                 *
                 * Cada solicitud protegida debe
                 * autenticarse mediante su JWT.
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                /*
                 * 4. Configurar respuesta para usuarios
                 * no autenticados.
                 *
                 * Sin autenticación válida:
                 * HTTP 401 Unauthorized.
                 *
                 * Con autenticación, pero sin permisos:
                 * HTTP 403 Forbidden.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception)
                                -> response.sendError(HttpServletResponse.SC_FORBIDDEN)
                        )
                )
                /*
                 * 5. Reglas de autorización.
                 *
                 * Se conservan los permisos existentes
                 * de UMSS Market.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        /*
                         * Autenticación y registro.
                         *
                         * Endpoints públicos.
                         */
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register/entrepreneur", "/api/auth/register/customer"
                        ).permitAll()
                        /*
                         * Endpoints que requieren
                         * un usuario autenticado.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/admin").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/recommendations",
                                "/api/interactions/**",
                                "/api/orders/me"
                        ).authenticated()
                        /*
                         * Inventario.
                         *
                         * Solo emprendedores.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/stores/*/inventory/**"
                        ).hasRole("EMPRENDEDOR")
                        /*
                         * Reseñas.
                         *
                         * Requieren autenticación.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/publications/*/reviews"
                        ).authenticated()
                        /*
                         * UC-001: Imágenes de publicaciones.
                         *
                         * Solo emprendedores pueden
                         * subir imágenes.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/publications/*/images"
                        ).hasRole("EMPRENDEDOR")
                        /*
                         * UC-001: Eliminación de imágenes.
                         *
                         * Solo emprendedores pueden
                         * solicitar la eliminación.
                         *
                         * El controlador debe comprobar
                         * además que sean propietarios.
                         */
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/publications/*/images/*"
                        ).hasRole("EMPRENDEDOR")
                        /*
                         * Detección de publicaciones
                         * duplicadas.
                         */
                        .requestMatchers(
                                "/api/stores/*/publications/duplicates"
                        ).hasRole("EMPRENDEDOR")
                        /*
                         * Administración de pedidos
                         * de una tienda.
                         */
                        .requestMatchers(
                                "/api/stores/*/orders/**"
                        ).hasRole("EMPRENDEDOR")
                        /*
                         * Mantener el comportamiento
                         * actual de los demás endpoints.
                         *
                         * IMPORTANTE:
                         * permitAll no significa que
                         * todos los endpoints sean seguros.
                         * Las rutas restantes deben
                         * revisarse antes de producción.
                         */
                        // Operaciones de publicaciones.
                        .requestMatchers(HttpMethod.GET, "/api/publications/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/publications/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/publications/*/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/publications").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/publications/**").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/publications/**").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/publications/**").hasRole("EMPRENDEDOR")
                        // Tiendas: consulta pública, administración protegida.
                        .requestMatchers(HttpMethod.GET, "/api/stores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stores/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stores").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/stores/**").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/stores/**").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/stores/**").hasRole("EMPRENDEDOR")
                        // Usuarios y administración.
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Pedidos.
                        .requestMatchers("/api/orders/**").authenticated()
                        // Imágenes.
                        .requestMatchers("/api/publications/*/images").hasRole("EMPRENDEDOR")
                        .requestMatchers("/api/publications/*/images/**").hasRole("EMPRENDEDOR")
                        // Cualquier otra operación requiere JWT.
                                                .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                /*
                 * 6. Registrar el filtro JWT.
                 *
                 * El filtro procesa la autenticación
                 * antes del filtro estándar de usuario
                 * y contraseña de Spring Security.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
