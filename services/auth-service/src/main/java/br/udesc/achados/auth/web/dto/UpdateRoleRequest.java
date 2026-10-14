package br.udesc.achados.auth.web.dto;

import br.udesc.achados.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "O papel é obrigatório")
        Role role
) {
}
