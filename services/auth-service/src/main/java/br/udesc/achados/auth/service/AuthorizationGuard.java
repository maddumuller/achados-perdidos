package br.udesc.achados.auth.service;

import br.udesc.achados.auth.exception.ForbiddenException;

import java.util.List;

public final class AuthorizationGuard {

    private AuthorizationGuard() {
    }

    public static void requireAdmin(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ForbiddenException("Acesso restrito a administradores");
        }

        boolean isAdmin = roles.stream().anyMatch(r -> {
            String clean = r.startsWith("ROLE_") ? r.substring(5) : r;
            return clean.equalsIgnoreCase("ADMIN");
        });

        if (!isAdmin) {
            throw new ForbiddenException("Acesso restrito a administradores");
        }
    }
}
