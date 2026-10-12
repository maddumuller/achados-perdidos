package br.udesc.achados.auth.service;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.ConflictException;
import br.udesc.achados.auth.messaging.UsuarioDesativadoEvent;
import br.udesc.achados.auth.repository.UserRepository;
import br.udesc.achados.auth.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserAdminService userAdminService;

    private UUID adminId;
    private UUID outroUsuarioId;
    private User outroUsuario;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        outroUsuarioId = UUID.randomUUID();
        outroUsuario = new User("João", "joao@email.com", "hash", Role.USER);
        outroUsuario.setId(outroUsuarioId);
    }

    @Test
    void adminNaoPodeRemoverProprioPapelDeAdmin() {
        assertThrows(ConflictException.class, () ->
                userAdminService.updateRole(adminId, Role.USER, adminId));
    }

    @Test
    void adminNaoPodeDesativarASiMesmo() {
        assertThrows(ConflictException.class, () ->
                userAdminService.updateStatus(adminId, false, adminId));
    }

    @Test
    void deveAlterarPapelDeOutroUsuarioComSucesso() {
        when(userRepository.findById(outroUsuarioId)).thenReturn(Optional.of(outroUsuario));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse resp = userAdminService.updateRole(outroUsuarioId, Role.ADMIN, adminId);

        assertEquals(Role.ADMIN, resp.role());
    }

    @Test
    void deveDesativarOutroUsuarioEPublicarEvento() {
        when(userRepository.findById(outroUsuarioId)).thenReturn(Optional.of(outroUsuario));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse resp = userAdminService.updateStatus(outroUsuarioId, false, adminId);

        assertFalse(resp.ativo());
        verify(eventPublisher).publishEvent(any(UsuarioDesativadoEvent.class));
    }
}
