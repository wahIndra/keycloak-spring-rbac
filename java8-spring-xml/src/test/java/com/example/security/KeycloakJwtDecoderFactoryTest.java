package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/** Checks every claim rule in KeycloakJwtDecoderFactory.tokenValidator(). */
class KeycloakJwtDecoderFactoryTest {

    private static final String ISSUER = "http://localhost:8080/realms/myrealm";
    private static final String AUDIENCE = "orders-api";

    private final OAuth2TokenValidator<Jwt> validator =
            KeycloakJwtDecoderFactory.tokenValidator(ISSUER, AUDIENCE);

    private static Jwt.Builder validAccessToken() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .audience(Collections.singletonList(AUDIENCE))
                .claim("typ", "Bearer")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
    }

    @Test
    void acceptsValidAccessToken() {
        assertThat(validator.validate(validAccessToken().build()).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        Jwt jwt = validAccessToken().issuer("https://evil.example.com/realms/myrealm").build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenForAnotherApi() {
        Jwt jwt = validAccessToken().audience(Collections.singletonList("some-other-api")).build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsIdToken() {
        Jwt jwt = validAccessToken().claim("typ", "ID").build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithoutType() {
        Jwt jwt = validAccessToken().claims(c -> c.remove("typ")).build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minusSeconds(3600);
        Jwt jwt = validAccessToken().issuedAt(past).expiresAt(past.plusSeconds(300)).build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithoutExpiry() {
        Jwt jwt = validAccessToken().claims(c -> c.remove(JwtClaimNames.EXP)).build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}
