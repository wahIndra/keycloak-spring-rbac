package com.example.security;

import java.util.Objects;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the JwtDecoder used by the API. Every token must pass ALL of these checks:
 *  - signature: RS256 only, key taken from Keycloak's JWKS endpoint
 *  - exp / nbf: not expired, not used too early (60 seconds clock skew)
 *  - exp present: tokens without an expiry are rejected
 *  - iss: created by our realm
 *  - aud: meant for this API
 *  - typ: a Keycloak access token ("Bearer"), not an ID token or refresh token
 *
 * Written in Java 8 syntax on purpose, so the same class works on Java 8 to 21.
 */
public final class KeycloakJwtDecoderFactory {

    private KeycloakJwtDecoderFactory() { }

    /**
     * @param issuerUri must match the token's "iss" EXACTLY (the public realm URL)
     * @param jwkSetUri may be empty. If empty, it is built from issuerUri.
     *                  Set it to the internal URL if the API reaches Keycloak over an internal network.
     * @param audience  this API's client name; must be in "aud"
     */
    public static JwtDecoder create(String issuerUri, String jwkSetUri, String audience) {
        requireText(issuerUri, "issuerUri");
        requireText(audience, "audience");

        String jwks = isBlank(jwkSetUri)
                ? issuerUri + "/protocol/openid-connect/certs"
                : jwkSetUri;

        // Does not call Keycloak at startup.
        // Public keys are fetched when the first token arrives, then cached.
        // An unknown "kid" (after a key rotation) triggers a refresh.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(tokenValidator(issuerUri, audience));
        return decoder;
    }

    /** The claim checks on their own, so they can be unit tested without a signed token. */
    public static OAuth2TokenValidator<Jwt> tokenValidator(String issuerUri, String audience) {
        return new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(issuerUri),                 // exp, nbf, iss
                new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull), // exp must exist
                new AudienceValidator(audience),                                   // aud
                new JwtClaimValidator<Object>("typ", "Bearer"::equals));           // access tokens only
    }

    private static void requireText(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
