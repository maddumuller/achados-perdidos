package br.udesc.achados.auth.service;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.*;
import br.udesc.achados.auth.repository.UserRepository;
import br.udesc.achados.auth.web.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        String hash = passwordEncoder.encode(request.senha());
        User user = new User(request.nome().trim(), normalizedEmail, hash, Role.USER);
        User saved = userRepository.save(user);

        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        // Não revela se o e-mail existe
        if (user == null) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        Instant agora = Instant.now();

        // 1. Verifica se está bloqueado
        if (user.isBloqueado(agora)) {
            throw new LockedException("Conta bloqueada temporariamente até " + user.getBloqueadoAte());
        }

        // 2. Verifica se está ativo
        if (!user.isAtivo()) {
            throw new ForbiddenException("Usuário desativado");
        }

        // 3. Verifica senha
        if (!passwordEncoder.matches(request.senha(), user.getSenhaHash())) {
            user.registrarTentativaFalha(agora);
            userRepository.save(user);

            if (user.isBloqueado(agora)) {
                throw new LockedException("Conta bloqueada temporariamente até " + user.getBloqueadoAte());
            }
            throw new UnauthorizedException("Credenciais inválidas");
        }

        // 4. Sucesso: zera tentativas falhas e gera token
        user.zerarTentativasFalhas();
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserResponse.fromEntity(user)
        );
    }

    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!user.isAtivo()) {
            throw new ForbiddenException("Usuário desativado");
        }

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.senhaAtual(), user.getSenhaHash())) {
            throw new UnauthorizedException("Senha atual incorreta");
        }

        if (passwordEncoder.matches(request.novaSenha(), user.getSenhaHash())) {
            throw new ConflictException("A nova senha não pode ser igual à senha atual");
        }

        user.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        userRepository.save(user);
    }
}
