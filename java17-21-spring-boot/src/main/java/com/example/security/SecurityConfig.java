package com.example.security;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(KeycloakProps.class)
public class SecurityConfig {

    /** Swagger is only open in the dev profile. */
    @Bean
    @Order(1)
    @Profile("dev")
    SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http, KeycloakProps props) throws Exception {
        var entryPoint = new JsonAuthenticationEntryPoint();
        var deniedHandler = new JsonAccessDeniedHandler();

        // Principal name = "sub" (the default). It never changes, unlike a username,
        // so it is the right key for audit logs and data ownership.
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRoleConverter(props.clientId(), props.includeRealmRoles()));

        return http
            .csrf(AbstractHttpConfigurer::disable)   // token-only API: no cookies, so no CSRF risk
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'none'; frame-ancestors 'none'")))
            .authorizeHttpRequests(auth -> auth
                // Let Spring's error page render 400/404/500 instead of hiding them behind 401/403
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/leads/**").hasAnyRole("LEADS_READ", "LEADS_OPERATOR")
                .requestMatchers(HttpMethod.POST, "/leads").hasAnyRole("LEADS_WRITE", "LEADS_OPERATOR")
                .requestMatchers(HttpMethod.POST, "/b2b/partners/*/leads").hasRole("B2B_LEADS_SUBMIT")
                .requestMatchers(HttpMethod.GET, "/mobile/me").hasRole("MOBILE_ACCESS")
                .anyRequest().denyAll())                     // deny by default
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler))
            .build();
    }

    /** Checks signature (RS256), exp, iss, aud, and typ. See KeycloakJwtDecoderFactory. */
    @Bean
    JwtDecoder jwtDecoder(KeycloakProps props) {
        return KeycloakJwtDecoderFactory.create(props.issuerUri(), props.jwkSetUri(), props.audience());
    }

    /** CORS only for listed origins. Never "*". No cookies (credentials) are allowed. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(KeycloakProps props) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
