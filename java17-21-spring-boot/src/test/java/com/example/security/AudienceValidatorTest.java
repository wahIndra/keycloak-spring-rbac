package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("leads-api");

    @Test
    void acceptsTokenForThisApi() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .audience(Arrays.asList("leads-api", "account")).build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenForAnotherApi() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .audience(Arrays.asList("orders-api")).build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithoutAudience() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").claim("sub", "x").build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }
}
