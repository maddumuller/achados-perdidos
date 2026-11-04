package br.udesc.achados.auth.web;

import br.udesc.achados.auth.service.JwtService;
import br.udesc.achados.auth.service.UserTokenData;
import br.udesc.achados.auth.web.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Atua somente nos endpoints protegidos do auth-service
        return !path.startsWith("/api/auth/me") && !path.startsWith("/api/auth/users");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            writeUnauthorized(response, request, "Token ausente ou inválido");
            return;
        }

        String token = authHeader.substring(7).trim();
        try {
            UserTokenData user = jwtService.parseToken(token);
            request.setAttribute("userId", user.userId());
            request.setAttribute("roles", user.roles());
            request.setAttribute("email", user.email());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, request, "Token ausente ou inválido");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String cid = Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                message,
                request.getRequestURI(),
                cid
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
