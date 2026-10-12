package br.edu.udesc.gateway.service;

import br.edu.udesc.gateway.config.GatewayProperties;
import br.edu.udesc.gateway.domain.AuthenticatedUser;
import br.edu.udesc.gateway.exception.GatewayException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(GatewayProperties properties) {
        this.secretKey = buildSecretKey(properties.getJwtSecret());
    }

    public AuthenticatedUser validate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new GatewayException(401, "Token ausente ou inválido");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new GatewayException(401, "Token ausente ou inválido");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                Object uid = claims.get("userId");
                if (uid != null) {
                    userId = uid.toString();
                }
            }

            if (userId == null || userId.isBlank()) {
                throw new GatewayException(401, "Token ausente ou inválido");
            }

            List<String> roles = extractRoles(claims.get("roles"));

            return new AuthenticatedUser(userId, roles);
        } catch (JwtException | IllegalArgumentException e) {
            throw new GatewayException(401, "Token ausente ou inválido");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Object rolesClaim) {
        List<String> roles = new ArrayList<>();
        if (rolesClaim instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
        } else if (rolesClaim instanceof String str && !str.isBlank()) {
            Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(roles::add);
        }
        return roles;
    }

    private static SecretKey buildSecretKey(String secret) {
        byte[] bytes = secret != null ? secret.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
