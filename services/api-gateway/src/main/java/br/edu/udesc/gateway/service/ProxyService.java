package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.domain.AuthenticatedUser;
import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.exception.GatewayException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

@Service
public class ProxyService {

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ProxyService(RestClient restClient, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = restClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public int forward(HttpServletRequest req, HttpServletResponse res, RouteDefinition route,
                       AuthenticatedUser user, String correlationId) throws IOException {
        URI uri = buildTargetUri(req, route);
        byte[] body = req.getInputStream().readAllBytes();
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(route.getName());

        try {
            ResponseEntity<byte[]> responseEntity = cb.executeSupplier(() -> {
                RestClient.RequestBodySpec spec = restClient
                        .method(HttpMethod.valueOf(req.getMethod()))
                        .uri(uri)
                        .headers(headers -> copyHeaders(req, headers, user, correlationId));

                if (body.length > 0) {
                    spec = spec.body(body);
                }

                return spec.retrieve()
                        .onStatus(status -> true, (clientReq, clientRes) -> {})
                        .toEntity(byte[].class);
            });

            writeResponse(res, responseEntity);
            return responseEntity.getStatusCode().value();
        } catch (CallNotPermittedException e) {
            throw new GatewayException(503, "Serviço temporariamente indisponível");
        } catch (ResourceAccessException e) {
            throw new GatewayException(502, "Serviço não respondeu");
        }
    }

    private URI buildTargetUri(HttpServletRequest req, RouteDefinition route) {
        String path = req.getRequestURI();
        String query = req.getQueryString();

        String resolvedPath;
        if (route.isStripPrefix()) {
            String prefix = route.getPathPrefix();
            String sub = path.substring(prefix.length());
            resolvedPath = (sub.isEmpty() || sub.startsWith("/")) ? sub : "/" + sub;
        } else {
            resolvedPath = path;
        }

        String base = route.getTargetUrl();
        if (base.endsWith("/") && resolvedPath.startsWith("/")) {
            base = base.substring(0, base.length() - 1);
        } else if (!base.endsWith("/") && !resolvedPath.startsWith("/") && !resolvedPath.isEmpty()) {
            base = base + "/";
        }

        String fullUrl = base + resolvedPath + (query != null && !query.isBlank() ? "?" + query : "");
        return URI.create(fullUrl);
    }

    private void copyHeaders(HttpServletRequest req, HttpHeaders targetHeaders, AuthenticatedUser user, String cid) {
        Enumeration<String> headerNames = req.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String lower = headerName.toLowerCase();
                if (lower.equals("host") || lower.equals("connection") || lower.equals("content-length")
                        || lower.equals("transfer-encoding") || lower.equals("x-user-id") || lower.equals("x-user-roles")) {
                    continue;
                }
                Enumeration<String> values = req.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    targetHeaders.add(headerName, values.nextElement());
                }
            }
        }

        targetHeaders.set("X-Correlation-Id", cid);

        String clientIp = extractClientIp(req);
        String existingXff = req.getHeader("X-Forwarded-For");
        if (existingXff != null && !existingXff.isBlank()) {
            targetHeaders.set("X-Forwarded-For", existingXff + ", " + clientIp);
        } else {
            targetHeaders.set("X-Forwarded-For", clientIp);
        }

        if (user != null) {
            targetHeaders.set("X-User-Id", user.id());
            if (user.roles() != null && !user.roles().isEmpty()) {
                targetHeaders.set("X-User-Roles", String.join(",", user.roles()));
            }
        }
    }

    private void writeResponse(HttpServletResponse res, ResponseEntity<byte[]> responseEntity) throws IOException {
        res.setStatus(responseEntity.getStatusCode().value());
        HttpHeaders headers = responseEntity.getHeaders();
        headers.forEach((headerName, values) -> {
            String lower = headerName.toLowerCase();
            if (!lower.equals("transfer-encoding")) {
                for (String value : values) {
                    res.addHeader(headerName, value);
                }
            }
        });

        byte[] body = responseEntity.getBody();
        if (body != null && body.length > 0) {
            res.getOutputStream().write(body);
            res.getOutputStream().flush();
        }
    }

    public static String extractClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }
}
