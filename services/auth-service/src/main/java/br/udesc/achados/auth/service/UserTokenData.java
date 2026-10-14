package br.udesc.achados.auth.service;

import java.util.List;
import java.util.UUID;

public record UserTokenData(
        UUID userId,
        String email,
        List<String> roles
) {
    public boolean hasRole(String requiredRole) {
        if (requiredRole == null || requiredRole.isBlank()) {
            return true;
        }
        String cleanRequired = requiredRole.startsWith("ROLE_") ? requiredRole.substring(5) : requiredRole;
        return roles != null && roles.stream().anyMatch(r -> {
            String cleanR = r.startsWith("ROLE_") ? r.substring(5) : r;
            return cleanR.equalsIgnoreCase(cleanRequired);
        });
    }
}
