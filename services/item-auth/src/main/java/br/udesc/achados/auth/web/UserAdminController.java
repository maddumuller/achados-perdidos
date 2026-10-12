package br.udesc.achados.auth.web;

import br.udesc.achados.auth.service.AuthorizationGuard;
import br.udesc.achados.auth.service.UserAdminService;
import br.udesc.achados.auth.web.dto.UpdateRoleRequest;
import br.udesc.achados.auth.web.dto.UpdateStatusRequest;
import br.udesc.achados.auth.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public Page<UserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute("roles") List<String> roles
    ) {
        AuthorizationGuard.requireAdmin(roles);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return userAdminService.listUsers(pageable);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @RequestAttribute("userId") UUID adminId,
            @RequestAttribute("roles") List<String> roles
    ) {
        AuthorizationGuard.requireAdmin(roles);
        return userAdminService.updateRole(id, request.role(), adminId);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestAttribute("userId") UUID adminId,
            @RequestAttribute("roles") List<String> roles
    ) {
        AuthorizationGuard.requireAdmin(roles);
        return userAdminService.updateStatus(id, request.ativo(), adminId);
    }
}
