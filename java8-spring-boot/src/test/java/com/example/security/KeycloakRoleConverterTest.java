package com.example.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

    @Test
    void mapsClientRolesToUppercaseRoleAuthorities() {
        Map<String, Object> leadsApi = new HashMap<String, Object>();
        leadsApi.put("roles", Arrays.asList("leads_read"));
        Map<String, Object> resourceAccess = new HashMap<String, Object>();
        resourceAccess.put("leads-api", leadsApi);

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("resource_access", resourceAccess)
                .build();

        Collection<GrantedAuthority> authorities = new KeycloakRoleConverter("leads-api", false).convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_LEADS_READ");
    }

    @Test
    void ignoresRolesOfOtherClients() {
        Map<String, Object> ordersApi = new HashMap<String, Object>();
        ordersApi.put("roles", Arrays.asList("ORDERS_WRITE"));
        Map<String, Object> resourceAccess = new HashMap<String, Object>();
        resourceAccess.put("orders-api", ordersApi);

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("resource_access", resourceAccess)
                .build();

        assertThat(new KeycloakRoleConverter("leads-api", false).convert(jwt)).isEmpty();
    }

    @Test
    void readsRealmRolesOnlyWhenEnabled() {
        Map<String, Object> realmAccess = new HashMap<String, Object>();
        realmAccess.put("roles", Arrays.asList("legacy-role"));

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("realm_access", realmAccess)
                .build();

        assertThat(new KeycloakRoleConverter("leads-api", false).convert(jwt)).isEmpty();
        assertThat(new KeycloakRoleConverter("leads-api", true).convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_LEGACY-ROLE");
    }
}
