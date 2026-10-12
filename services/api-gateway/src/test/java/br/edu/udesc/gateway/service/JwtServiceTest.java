package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.config.GatewayProperties;
import br.edu.udesc.gateway.domain.AuthenticatedUser;
import br.edu.udesc.gateway.exception.GatewayException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-muito-seguro-com-mais-de-32-chars";
    private JwtService jwtService;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.setJwtSecret(SECRET);
        jwtService = new JwtService(properties);

        testKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void deveValidarTokenValidoEExtrairDados() {
        String token = Jwts.builder()
                .subject("user-123")
                .claim("roles", List.of("ADMIN", "USER"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(testKey)
                .compact();

        AuthenticatedUser user = jwtService.validate("Bearer " + token);

        assertNotNull(user);
        assertEquals("user-123", user.id());
        assertTrue(user.hasRole("ADMIN"));
        assertTrue(user.hasRole("ROLE_ADMIN"));
        assertTrue(user.hasRole("user"));
        assertFalse(user.hasRole("SUPERVISOR"));
    }

    @Test
    void deveLancar401ParaTokenExpirado() {
        String token = Jwts.builder()
                .subject("user-123")
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(testKey)
                .compact();

        GatewayException ex = assertThrows(GatewayException.class, () ->
                jwtService.validate("Bearer " + token));
        assertEquals(401, ex.getStatus());
    }

    @Test
    void deveLancar401ParaAssinaturaInvalida() {
        SecretKey otherKey = Keys.hmacShaKeyFor("outro-segredo-com-tamanho-suficiente-para-hs256".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-123")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        GatewayException ex = assertThrows(GatewayException.class, () ->
                jwtService.validate("Bearer " + token));
        assertEquals(401, ex.getStatus());
    }

    @Test
    void deveLancar401ParaHeaderSemBearerOuVazio() {
        GatewayException ex1 = assertThrows(GatewayException.class, () -> jwtService.validate(null));
        assertEquals(401, ex1.getStatus());

        GatewayException ex2 = assertThrows(GatewayException.class, () -> jwtService.validate(""));
        assertEquals(401, ex2.getStatus());

        GatewayException ex3 = assertThrows(GatewayException.class, () -> jwtService.validate("Basic 123456"));
        assertEquals(401, ex3.getStatus());

        GatewayException ex4 = assertThrows(GatewayException.class, () -> jwtService.validate("Bearer "));
        assertEquals(401, ex4.getStatus());
    }
}
