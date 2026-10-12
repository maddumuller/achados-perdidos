package br.edu.udesc.gateway.domain;

import java.util.List;

public record AuthenticatedUser(String id, List<String> roles) {

    public boolean hasRole(String requiredRole) {
        if (requiredRole == null || requiredRole.isBlank()) {
            return true;
        }
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        String normalizedRequired = normalizeRole(requiredRole);
        return roles.stream()
                .map(this::normalizeRole)
                .anyMatch(r -> r.equalsIgnoreCase(normalizedRequired));
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String trimmed = role.trim();
        if (trimmed.toUpperCase().startsWith("ROLE_")) {
            return trimmed.substring(5);
        }
        return trimmed;
    }
}
