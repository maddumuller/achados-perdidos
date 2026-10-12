package br.edu.udesc.gateway.config;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.repository.RouteDefinitionRepository;
import br.edu.udesc.gateway.service.RouteResolverService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RouteDefinitionRepository repository;
    private final RouteResolverService routeResolverService;
    private final GatewayProperties properties;

    public DataSeeder(RouteDefinitionRepository repository,
                      RouteResolverService routeResolverService,
                      GatewayProperties properties) {
        this.repository = repository;
        this.routeResolverService = routeResolverService;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            RouteDefinition authRoute = new RouteDefinition(
                    "auth",
                    "/api/auth",
                    properties.getServices().getAuthUrl(),
                    false,
                    false,
                    null,
                    60,
                    true
            );

            RouteDefinition ocorrenciasRoute = new RouteDefinition(
                    "ocorrencias",
                    "/api/ocorrencias",
                    properties.getServices().getOcorrenciaUrl(),
                    false,
                    true,
                    null,
                    60,
                    true
            );

            RouteDefinition itensRoute = new RouteDefinition(
                    "itens",
                    "/api/itens",
                    properties.getServices().getItemUrl(),
                    false,
                    true,
                    null,
                    60,
                    true
            );

            repository.saveAll(List.of(authRoute, ocorrenciasRoute, itensRoute));
            routeResolverService.reload();
        }
    }
}
