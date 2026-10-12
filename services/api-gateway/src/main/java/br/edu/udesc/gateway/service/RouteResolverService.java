package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.repository.RouteDefinitionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RouteResolverService {

    private final RouteDefinitionRepository repository;
    private final List<RouteDefinition> cachedRoutes = new CopyOnWriteArrayList<>();

    public RouteResolverService(RouteDefinitionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        cachedRoutes.clear();
        cachedRoutes.addAll(repository.findByEnabledTrue());
    }

    public Optional<RouteDefinition> resolve(String path) {
        if (path == null) {
            return Optional.empty();
        }

        return cachedRoutes.stream()
                .filter(RouteDefinition::isEnabled)
                .filter(route -> matchesPrefix(path, route.getPathPrefix()))
                .max(Comparator.comparingInt(r -> r.getPathPrefix().length()));
    }

    private boolean matchesPrefix(String path, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return false;
        }
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
