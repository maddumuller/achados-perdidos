package br.edu.udesc.gateway.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    private static final String DEFAULT_ALLOWED_HEADERS =
            "Authorization, Content-Type, Accept, Origin, X-Requested-With, X-Admin-Key, X-User-Id, X-User-Roles, X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String reqHeaders = req.getHeader("Access-Control-Request-Headers");
        String allowHeaders = (reqHeaders != null && !reqHeaders.isBlank())
                ? DEFAULT_ALLOWED_HEADERS + ", " + reqHeaders
                : DEFAULT_ALLOWED_HEADERS;

        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
        res.setHeader("Access-Control-Allow-Headers", allowHeaders);
        res.setHeader("Access-Control-Expose-Headers", "X-Correlation-Id, Authorization");
        res.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}
