package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.exception.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private MutableClock clock;
    private RateLimitService rateLimitService;
    private RouteDefinition route;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-12T12:00:00Z"));
        rateLimitService = new RateLimitService(clock);

        route = new RouteDefinition(
                "itens", "/api/itens", "http://localhost:8081",
                false, false, null, 3, true
        );
    }

    @Test
    void devePermitirAteOLimiteEBloquearNoExcesso() {
        // Limite = 3 requisições por minuto
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));

        GatewayException ex = assertThrows(GatewayException.class, () ->
                rateLimitService.check("user-1", route));

        assertEquals(429, ex.getStatus());
        assertTrue(ex.getHeaders().containsKey("Retry-After"));
        assertEquals("60", ex.getHeaders().get("Retry-After"));
    }

    @Test
    void deveLiberarNovamenteEmNovaJanelaDeTempo() {
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));

        assertThrows(GatewayException.class, () -> rateLimitService.check("user-1", route));

        // Avança 60 segundos para a próxima janela
        clock.advanceSeconds(60);

        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
    }

    @Test
    void clientesDiferentesNaoDevemSeAfetar() {
        // user-1 esgota a cota
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertDoesNotThrow(() -> rateLimitService.check("user-1", route));
        assertThrows(GatewayException.class, () -> rateLimitService.check("user-1", route));

        // user-2 continua liberado
        assertDoesNotThrow(() -> rateLimitService.check("user-2", route));
    }

    @Test
    void deveLimparJanelasExpiradas() {
        rateLimitService.check("user-1", route);
        assertEquals(1, rateLimitService.getActiveWindowsCount());

        clock.advanceSeconds(120);
        rateLimitService.cleanupExpiredWindows();

        assertEquals(0, rateLimitService.getActiveWindowsCount());
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;

        MutableClock(Instant initial) {
            this.currentInstant = initial;
        }

        void advanceSeconds(long seconds) {
            this.currentInstant = this.currentInstant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.currentInstant;
        }
    }
}
