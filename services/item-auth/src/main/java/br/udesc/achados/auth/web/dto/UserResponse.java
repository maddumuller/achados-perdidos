package br.udesc.achados.auth.web.dto;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nome,
        String email,
        Role role,
        Boolean ativo,
        Instant criadoEm
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole(),
                user.isAtivo(),
                user.getCriadoEm()
        );
    }
}
