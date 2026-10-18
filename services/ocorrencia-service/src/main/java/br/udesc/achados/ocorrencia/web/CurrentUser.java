package br.udesc.achados.ocorrencia.web;

import java.util.List;
import java.util.UUID;

public record CurrentUser(UUID id, List<String> roles) {

    public boolean isAdmin() {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(r -> {
            String clean = r.startsWith("ROLE_") ? r.substring(5) : r;
            return clean.equalsIgnoreCase("ADMIN");
        });
    }

    public String rolesAsHeader() {
        return (roles == null || roles.isEmpty()) ? "USER" : String.join(",", roles);
    }
}
