package com.example.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads roles from the token:
 *  - client roles: resource_access.<clientId>.roles (always)
 *  - realm roles : realm_access.roles (only when includeRealmRoles = true)
 * Output is ROLE_<UPPER_CASE>, e.g. leads_read -> ROLE_LEADS_READ
 *
 * Written in Java 8 syntax on purpose, so the same class works on Java 8 to 21.
 */
public final class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;
    private final boolean includeRealmRoles;

    public KeycloakRoleConverter(String clientId, boolean includeRealmRoles) {
        this.clientId = Objects.requireNonNull(clientId, "clientId is required");
        this.includeRealmRoles = includeRealmRoles;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<String>();

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            roles.addAll(rolesOf(resourceAccess.get(clientId)));
        }
        if (includeRealmRoles) {
            roles.addAll(rolesOf(jwt.getClaimAsMap("realm_access")));
        }

        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> rolesOf(Object container) {
        if (!(container instanceof Map)) {
            return Collections.emptySet();
        }
        Object roles = ((Map<String, Object>) container).get("roles");
        if (!(roles instanceof Collection)) {
            return Collections.emptySet();
        }
        return ((Collection<Object>) roles).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
