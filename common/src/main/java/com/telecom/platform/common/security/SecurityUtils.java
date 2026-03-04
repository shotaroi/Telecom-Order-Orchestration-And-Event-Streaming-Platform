package com.telecom.platform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for extracting claims from OIDC/JWT principal.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<String> getCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomerIdPrincipal cp) {
            return Optional.of(cp.getCustomerId());
        }
        return getClaim("customer_id")
                .or(() -> getClaim("sub")); // fallback to sub for local dev
    }

    public static Optional<String> getClaim(String claim) {
        Jwt jwt = getJwt();
        if (jwt == null) return Optional.empty();
        Object value = jwt.getClaim(claim);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    public static Set<String> getRoles() {
        Jwt jwt = getJwt();
        if (jwt == null) return Collections.emptySet();
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Iterable<?> iterable) {
            return java.util.stream.StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public static boolean hasRole(String role) {
        return getRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(role) || r.equals("ROLE_" + role));
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> userRoles = getRoles();
        for (String role : roles) {
            if (userRoles.stream().anyMatch(r -> r.equalsIgnoreCase(role) || r.equals("ROLE_" + role))) {
                return true;
            }
        }
        return false;
    }

    private static Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }
}
