package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.config.GatewayProperties;
import br.edu.udesc.gateway.web.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
public class AdminKeyFilter extends OncePerRequestFilter {

    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;

    public AdminKeyFilter(GatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/gateway/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String adminKey = req.getHeader("X-Admin-Key");

        if (adminKey == null || !adminKey.equals(properties.getAdminKey())) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");

            String cid = Optional.ofNullable(req.getHeader("X-Correlation-Id"))
                    .filter(s -> !s.isBlank())
                    .orElse(UUID.randomUUID().toString());

            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Chave de administração inválida ou ausente",
                    req.getRequestURI(),
                    cid
            );

            res.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        chain.doFilter(req, res);
    }
}
