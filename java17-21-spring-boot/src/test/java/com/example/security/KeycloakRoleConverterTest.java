package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

    @Test
    void mapsClientRolesToUppercaseRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("resource_access", Map.of("leads-api", Map.of("roles", List.of("leads_read"))))
                .build();

        Collection<GrantedAuthority> authorities = new KeycloakRoleConverter("leads-api", false).convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_LEADS_READ");
    }

    @Test
    void ignoresRolesOfOtherClients() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("resource_access", Map.of("orders-api", Map.of("roles", List.of("ORDERS_WRITE"))))
                .build();

        assertThat(new KeycloakRoleConverter("leads-api", false).convert(jwt)).isEmpty();
    }

    @Test
    void readsRealmRolesOnlyWhenEnabled() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("realm_access", Map.of("roles", List.of("legacy-role")))
                .build();

        assertThat(new KeycloakRoleConverter("leads-api", false).convert(jwt)).isEmpty();
        assertThat(new KeycloakRoleConverter("leads-api", true).convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_LEGACY-ROLE");
    }
}
