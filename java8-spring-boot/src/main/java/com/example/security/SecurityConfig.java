package com.example.security;

import javax.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize
public class SecurityConfig {

    // No defaults for issuer/audience/client-id: a missing value stops the app at startup (fail closed)
    @Value("${app.security.issuer-uri}")                private String issuerUri;
    @Value("${app.security.jwk-set-uri:}")              private String jwkSetUri;
    @Value("${app.security.audience}")                  private String audience;
    @Value("${app.security.client-id}")                 private String clientId;
    @Value("${app.security.include-realm-roles:false}") private boolean includeRealmRoles;

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();
        JsonAccessDeniedHandler deniedHandler = new JsonAccessDeniedHandler();

        http
            .csrf(AbstractHttpConfigurer::disable)  // token-only API: no cookies, so no CSRF risk
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'none'; frame-ancestors 'none'")))
            .authorizeHttpRequests(auth -> auth
                // Same behavior as Spring Security 6: rules apply to every dispatch type
                .shouldFilterAllDispatcherTypes(true)
                // Let Spring's error page render 400/404/500 instead of hiding them behind 401/403
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/leads/**").hasAnyRole("LEADS_READ", "LEADS_OPERATOR")
                .requestMatchers(HttpMethod.POST, "/leads").hasAnyRole("LEADS_WRITE", "LEADS_OPERATOR")
                .requestMatchers(HttpMethod.POST, "/b2b/partners/*/leads").hasRole("B2B_LEADS_SUBMIT")
                .anyRequest().denyAll())            // deny by default
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler));
        return http.build();
    }

    /** Checks signature (RS256), exp, iss, aud, and typ. See KeycloakJwtDecoderFactory. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return KeycloakJwtDecoderFactory.create(issuerUri, jwkSetUri, audience);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Principal name = "sub" (the default). It never changes, unlike a username,
        // so it is the right key for audit logs and data ownership.
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter(clientId, includeRealmRoles));
        return converter;
    }
}
