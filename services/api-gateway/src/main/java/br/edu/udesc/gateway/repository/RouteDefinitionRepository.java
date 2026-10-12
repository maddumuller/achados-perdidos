package br.edu.udesc.gateway.repository;

import br.edu.udesc.gateway.domain.RouteDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteDefinitionRepository extends JpaRepository<RouteDefinition, Long> {

    List<RouteDefinition> findByEnabledTrue();

    boolean existsByName(String name);

    boolean existsByPathPrefix(String pathPrefix);
}
