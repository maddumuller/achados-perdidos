package br.udesc.achados.auth.service;

import br.udesc.achados.auth.config.JwtProperties;
import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setJwtSecret("segredo-para-teste-muito-seguro-com-mais-de-32-chars");
        properties.setJwtExpirationMinutes(60);
        jwtService = new JwtService(properties);
    }

    @Test
    void deveGerarEValidarTokenComSucesso() {
        User user = new User("Maria", "maria@email.com", "hash", Role.USER);
        user.setId(UUID.randomUUID());

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        UserTokenData data = jwtService.parseToken(token);
        assertEquals(user.getId(), data.userId());
        assertEquals("maria@email.com", data.email());
        assertTrue(data.hasRole("USER"));
        assertFalse(data.hasRole("ADMIN"));
    }

    @Test
    void deveLancarExcecaoParaTokenInvalidoOuNulo() {
        assertThrows(UnauthorizedException.class, () -> jwtService.parseToken(null));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseToken(""));
        assertThrows(UnauthorizedException.class, () -> jwtService.parseToken("token-invalido-xyz"));
    }
}
