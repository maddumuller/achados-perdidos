package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.domain.AuthenticatedUser;
import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.exception.GatewayException;
import br.edu.udesc.gateway.service.*;
import br.edu.udesc.gateway.web.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(2)
public class GatewayFilter extends OncePerRequestFilter {

    private final RouteResolverService routeResolver;
    private final JwtService jwtService;
    private final RateLimitService rateLimit;
    private final ProxyService proxyService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public GatewayFilter(RouteResolverService routeResolver,
                         JwtService jwtService,
                         RateLimitService rateLimit,
                         ProxyService proxyService,
                         AuditLogService auditLogService,
                         ObjectMapper objectMapper) {
        this.routeResolver = routeResolver;
        this.jwtService = jwtService;
        this.rateLimit = rateLimit;
        this.proxyService = proxyService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return p.startsWith("/gateway/") || p.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String cid = Optional.ofNullable(req.getHeader("X-Correlation-Id"))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());
        res.setHeader("X-Correlation-Id", cid);

        int status = 500;
        RouteDefinition route = null;
        AuthenticatedUser user = null;

        try {
            route = routeResolver.resolve(req.getRequestURI())
                    .orElseThrow(() -> new GatewayException(404, "Rota não encontrada"));

            if (route.isRequiresAuth()) {
                user = jwtService.validate(req.getHeader("Authorization"));
                if (route.getRequiredRole() != null && !user.hasRole(route.getRequiredRole())) {
                    throw new GatewayException(403, "Sem permissão");
                }
            }

            String key = user != null ? user.id() : ProxyService.extractClientIp(req);
            rateLimit.check(key, route);

            status = proxyService.forward(req, res, route, user, cid);
        } catch (GatewayException e) {
            status = e.getStatus();
            writeError(res, e, req, cid);
        } catch (Exception e) {
            status = 500;
            writeError(res, new GatewayException(500, "Erro interno no gateway"), req, cid);
        } finally {
            auditLogService.log(req, route, user, status,
                    System.currentTimeMillis() - start, cid);
        }
    }

    private void writeError(HttpServletResponse res, GatewayException e,
                            HttpServletRequest req, String cid) throws IOException {
        res.setStatus(e.getStatus());
        res.setContentType("application/json;charset=UTF-8");
        if (e.getHeaders() != null) {
            e.getHeaders().forEach(res::setHeader);
        }

        HttpStatus httpStatus = HttpStatus.resolve(e.getStatus());
        String errorName = httpStatus != null ? httpStatus.getReasonPhrase() : "Error";

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                e.getStatus(),
                errorName,
                e.getMessage(),
                req.getRequestURI(),
                cid
        );

        res.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
