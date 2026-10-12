package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.AuthenticatedUser;
import br.edu.udesc.gateway.domain.RequestLog;
import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.repository.RequestLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {

    private final RequestLogRepository requestLogRepository;

    public AuditLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    public RequestLog log(HttpServletRequest req, RouteDefinition route, AuthenticatedUser user,
                          int status, long durationMs, String correlationId) {
        String clientIp = ProxyService.extractClientIp(req);
        String routeName = route != null ? route.getName() : null;
        String userId = user != null ? user.id() : null;

        RequestLog entry = new RequestLog(
                correlationId,
                req.getMethod(),
                req.getRequestURI(),
                routeName,
                status,
                durationMs,
                clientIp,
                userId,
                Instant.now()
        );

        return requestLogRepository.save(entry);
    }
}
