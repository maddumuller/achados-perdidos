package br.udesc.achados.auth.service;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.ConflictException;
import br.udesc.achados.auth.exception.ForbiddenException;
import br.udesc.achados.auth.exception.LockedException;
import br.udesc.achados.auth.exception.UnauthorizedException;
import br.udesc.achados.auth.repository.UserRepository;
import br.udesc.achados.auth.web.dto.ChangePasswordRequest;
import br.udesc.achados.auth.web.dto.LoginRequest;
import br.udesc.achados.auth.web.dto.LoginResponse;
import br.udesc.achados.auth.web.dto.RegisterRequest;
import br.udesc.achados.auth.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Maria", "maria@email.com", "hashSenha", Role.USER);
        user.setId(UUID.randomUUID());
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        RegisterRequest req = new RegisterRequest("Maria", "maria@email.com", "Senha123");

        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("hashSenha");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("maria@email.com", resp.email());
        assertEquals(Role.USER, resp.role());
    }

    @Test
    void deveLancar409AoCadastrarEmailDuplicado() {
        RegisterRequest req = new RegisterRequest("Maria", "maria@email.com", "Senha123");
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(req));
    }

    @Test
    void deveEfetuarLoginComSucessoEZerarFalhas() {
        LoginRequest req = new LoginRequest("maria@email.com", "Senha123");
        user.setTentativasFalhas(2);

        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha123", "hashSenha")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("token-jwt", resp.accessToken());
        assertEquals(0, user.getTentativasFalhas());
        verify(userRepository).save(user);
    }

    @Test
    void deveIncrementarFalhasELancar401ParaSenhaIncorreta() {
        LoginRequest req = new LoginRequest("maria@email.com", "SenhaErrada");

        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", "hashSenha")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(req));
        assertEquals("Credenciais inválidas", ex.getMessage());
        assertEquals(1, user.getTentativasFalhas());
    }

    @Test
    void deveBloquearContaNoQuintoErro() {
        LoginRequest req = new LoginRequest("maria@email.com", "SenhaErrada");
        user.setTentativasFalhas(4);

        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", "hashSenha")).thenReturn(false);

        assertThrows(LockedException.class, () -> authService.login(req));
        assertEquals(5, user.getTentativasFalhas());
        assertNotNull(user.getBloqueadoAte());
    }

    @Test
    void naoDevePermitirLoginEmContaInativa() {
        LoginRequest req = new LoginRequest("maria@email.com", "Senha123");
        user.setAtivo(false);

        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> authService.login(req));
    }

    @Test
    void deveTrocarSenhaComSucesso() {
        ChangePasswordRequest req = new ChangePasswordRequest("Senha123", "NovaSenha1");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha123", "hashSenha")).thenReturn(true);
        when(passwordEncoder.matches("NovaSenha1", "hashSenha")).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("novoHash");

        authService.changePassword(user.getId(), req);

        assertEquals("novoHash", user.getSenhaHash());
        verify(userRepository).save(user);
    }

    @Test
    void deveRejeitarTrocaDeSenhaComSenhaAtualIncorreta() {
        ChangePasswordRequest req = new ChangePasswordRequest("Errada123", "NovaSenha1");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Errada123", "hashSenha")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.changePassword(user.getId(), req));
    }
}
