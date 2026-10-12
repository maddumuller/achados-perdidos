package br.udesc.achados.auth.repository;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void deveSalvarEBuscarPorEmail() {
        User user = new User("Ana", "ana@email.com", "hash", Role.USER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("ana@email.com");
        assertTrue(found.isPresent());
        assertEquals("ana@email.com", found.get().getEmail());
        assertTrue(userRepository.existsByEmail("ana@email.com"));
    }

    @Test
    void deveVerificarExistenciaPorRole() {
        User admin = new User("Admin", "admin@email.com", "hash", Role.ADMIN);
        userRepository.save(admin);

        assertTrue(userRepository.existsByRole(Role.ADMIN));
    }
}
