package br.udesc.achados.auth.config;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties properties;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            String adminEmail = properties.getAdminEmail().toLowerCase().trim();
            log.info("Nenhum ADMIN encontrado. Criando administrador padrão: {}", adminEmail);

            String hash = passwordEncoder.encode(properties.getAdminPassword());
            User admin = new User("Administrador", adminEmail, hash, Role.ADMIN);
            userRepository.save(admin);
            log.info("Administrador padrão criado com sucesso.");
        }
    }
}
