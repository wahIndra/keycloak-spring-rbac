package com.example.security;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Keycloak settings, checked at startup. A missing value stops the app
 * instead of starting with a half-configured security setup (fail closed).
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record KeycloakProps(
        @NotBlank String issuerUri,
        String jwkSetUri,
        @NotBlank String audience,
        @NotBlank String clientId,
        boolean includeRealmRoles,
        List<String> allowedOrigins) {

    public KeycloakProps {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
