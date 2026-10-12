package br.edu.udesc.gateway.repository;

import br.edu.udesc.gateway.domain.RouteDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RouteDefinitionRepositoryTest {

    @Autowired
    private RouteDefinitionRepository repository;

    @Test
    void deveSalvarEBuscarRotasHabilitadas() {
        RouteDefinition r1 = new RouteDefinition(
                "itens", "/api/itens", "http://localhost:8081",
                false, true, null, 60, true
        );
        RouteDefinition r2 = new RouteDefinition(
                "desabilitada", "/api/desativada", "http://localhost:8089",
                false, false, null, 60, false
        );

        repository.save(r1);
        repository.save(r2);

        List<RouteDefinition> habilitadas = repository.findByEnabledTrue();

        assertEquals(1, habilitadas.size());
        assertEquals("itens", habilitadas.get(0).getName());
    }

    @Test
    void deveVerificarExistenciaPorNomeEPrefixo() {
        RouteDefinition r = new RouteDefinition(
                "ocorrencias", "/api/ocorrencias", "http://localhost:8082",
                false, true, null, 60, true
        );
        repository.save(r);

        assertTrue(repository.existsByName("ocorrencias"));
        assertFalse(repository.existsByName("inexistente"));

        assertTrue(repository.existsByPathPrefix("/api/ocorrencias"));
        assertFalse(repository.existsByPathPrefix("/api/outro"));
    }
}
