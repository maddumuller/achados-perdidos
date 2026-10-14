package br.udesc.achados.auth.service;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.ConflictException;
import br.udesc.achados.auth.exception.NotFoundException;
import br.udesc.achados.auth.messaging.UsuarioDesativadoEvent;
import br.udesc.achados.auth.repository.UserRepository;
import br.udesc.achados.auth.web.dto.UserResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserAdminService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::fromEntity);
    }

    @Transactional
    public UserResponse updateRole(UUID targetUserId, Role newRole, UUID adminId) {
        if (targetUserId.equals(adminId) && newRole != Role.ADMIN) {
            throw new ConflictException("Um administrador não pode remover o próprio papel de ADMIN");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        user.setRole(newRole);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateStatus(UUID targetUserId, boolean novoAtivo, UUID adminId) {
        if (targetUserId.equals(adminId) && !novoAtivo) {
            throw new ConflictException("Um administrador não pode desativar a si mesmo");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        boolean estavaAtivo = user.isAtivo();
        user.setAtivo(novoAtivo);
        User saved = userRepository.save(user);

        if (estavaAtivo && !novoAtivo) {
            eventPublisher.publishEvent(new UsuarioDesativadoEvent(targetUserId));
        }

        return UserResponse.fromEntity(saved);
    }
}
