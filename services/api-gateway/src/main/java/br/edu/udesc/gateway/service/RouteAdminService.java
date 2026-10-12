package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.exception.GatewayException;
import br.edu.udesc.gateway.repository.RouteDefinitionRepository;
import br.edu.udesc.gateway.web.dto.RouteRequest;
import br.edu.udesc.gateway.web.dto.RouteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteAdminService {

    private final RouteDefinitionRepository repository;
    private final RouteResolverService routeResolverService;

    public RouteAdminService(RouteDefinitionRepository repository, RouteResolverService routeResolverService) {
        this.repository = repository;
        this.routeResolverService = routeResolverService;
    }

    public List<RouteResponse> findAll() {
        return repository.findAll().stream()
                .map(RouteResponse::fromEntity)
                .toList();
    }

    public RouteResponse findById(Long id) {
        RouteDefinition entity = repository.findById(id)
                .orElseThrow(() -> new GatewayException(404, "Rota não encontrada com ID: " + id));
        return RouteResponse.fromEntity(entity);
    }

    @Transactional
    public RouteResponse create(RouteRequest request) {
        if (repository.existsByName(request.name())) {
            throw new GatewayException(400, "Já existe uma rota com o nome: " + request.name());
        }
        if (repository.existsByPathPrefix(request.pathPrefix())) {
            throw new GatewayException(400, "Já existe uma rota com o prefixo: " + request.pathPrefix());
        }

        RouteDefinition route = new RouteDefinition(
                request.name(),
                request.pathPrefix(),
                request.targetUrl(),
                request.stripPrefix(),
                request.requiresAuth(),
                request.requiredRole(),
                request.rateLimitPerMinute() > 0 ? request.rateLimitPerMinute() : 60,
                request.enabled()
        );

        RouteDefinition saved = repository.save(route);
        routeResolverService.reload();
        return RouteResponse.fromEntity(saved);
    }

    @Transactional
    public RouteResponse update(Long id, RouteRequest request) {
        RouteDefinition entity = repository.findById(id)
                .orElseThrow(() -> new GatewayException(404, "Rota não encontrada com ID: " + id));

        if (!entity.getName().equals(request.name()) && repository.existsByName(request.name())) {
            throw new GatewayException(400, "Já existe uma rota com o nome: " + request.name());
        }
        if (!entity.getPathPrefix().equals(request.pathPrefix()) && repository.existsByPathPrefix(request.pathPrefix())) {
            throw new GatewayException(400, "Já existe uma rota com o prefixo: " + request.pathPrefix());
        }

        entity.setName(request.name());
        entity.setPathPrefix(request.pathPrefix());
        entity.setTargetUrl(request.targetUrl());
        entity.setStripPrefix(request.stripPrefix());
        entity.setRequiresAuth(request.requiresAuth());
        entity.setRequiredRole(request.requiredRole());
        entity.setRateLimitPerMinute(request.rateLimitPerMinute() > 0 ? request.rateLimitPerMinute() : 60);
        entity.setEnabled(request.enabled());

        RouteDefinition saved = repository.save(entity);
        routeResolverService.reload();
        return RouteResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new GatewayException(404, "Rota não encontrada com ID: " + id);
        }
        repository.deleteById(id);
        routeResolverService.reload();
    }
}
