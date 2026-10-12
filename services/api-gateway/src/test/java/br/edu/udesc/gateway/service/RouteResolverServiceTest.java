package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.repository.RouteDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteResolverServiceTest {

    @Mock
    private RouteDefinitionRepository repository;

    private RouteResolverService service;

    @BeforeEach
    void setUp() {
        RouteDefinition rItens = new RouteDefinition(
                "itens", "/api/itens", "http://localhost:8081",
                false, true, null, 60, true
        );

        RouteDefinition rItensEspeciais = new RouteDefinition(
                "itens-especiais", "/api/itens/especiais", "http://localhost:8081",
                false, true, null, 60, true
        );

        RouteDefinition rDesabilitada = new RouteDefinition(
                "desabilitada", "/api/desabilitada", "http://localhost:8089",
                false, false, null, 60, false
        );

        when(repository.findByEnabledTrue()).thenReturn(List.of(rItens, rItensEspeciais));

        service = new RouteResolverService(repository);
        service.reload();
    }

    @Test
    void deveResolverPorMaiorPrefixo() {
        Optional<RouteDefinition> match = service.resolve("/api/itens/especiais/123");

        assertTrue(match.isPresent());
        assertEquals("itens-especiais", match.get().getName());
    }

    @Test
    void deveResolverRotaPadraoQuandoNaoForSubPrefixoEspecial() {
        Optional<RouteDefinition> match = service.resolve("/api/itens/123");

        assertTrue(match.isPresent());
        assertEquals("itens", match.get().getName());
    }

    @Test
    void deveIgnorarRotaDesabilitada() {
        Optional<RouteDefinition> match = service.resolve("/api/desabilitada");

        assertFalse(match.isPresent());
    }

    @Test
    void naoDeveCasarQuandoApenasSufixoDeString() {
        // /api/itensx não deve casar com /api/itens
        Optional<RouteDefinition> match = service.resolve("/api/itensx");

        assertFalse(match.isPresent());
    }

    @Test
    void deveCasarCaminhoExato() {
        Optional<RouteDefinition> match = service.resolve("/api/itens");

        assertTrue(match.isPresent());
        assertEquals("itens", match.get().getName());
    }

    @Test
    void deveRetornarVazioParaCaminhoNuloOuInexistente() {
        assertFalse(service.resolve(null).isPresent());
        assertFalse(service.resolve("/api/outra-coisa").isPresent());
    }
}
