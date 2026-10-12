package br.udesc.achados.auth.service;

import br.udesc.achados.auth.config.JwtProperties;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final int expirationMinutes;

    public JwtService(JwtProperties properties) {
        this.secretKey = buildSecretKey(properties.getJwtSecret());
        this.expirationMinutes = properties.getJwtExpirationMinutes();
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (long) expirationMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public UserTokenData parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Token ausente ou inválido");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new UnauthorizedException("Token ausente ou inválido");
            }

            UUID userId = UUID.fromString(sub);
            String email = claims.get("email", String.class);
            List<String> roles = extractRoles(claims.get("roles"));

            return new UserTokenData(userId, email, roles);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token ausente ou inválido");
        }
    }

    public long getExpirationSeconds() {
        return (long) expirationMinutes * 60;
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
