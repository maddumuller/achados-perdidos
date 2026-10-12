package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.domain.RouteDefinition;
import br.edu.udesc.gateway.repository.RequestLogRepository;
import br.edu.udesc.gateway.repository.RouteDefinitionRepository;
import br.edu.udesc.gateway.service.RouteResolverService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayIntegrationTest {

    private static final String JWT_SECRET = "troque-este-segredo-com-no-minimo-32-caracteres";
    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RouteDefinitionRepository routeRepository;

    @Autowired
    private RouteResolverService routeResolverService;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setupRoutes() {
        wireMockServer.resetAll();
        routeRepository.deleteAll();

        // 1. Rota pública de Auth
        RouteDefinition rAuth = new RouteDefinition(
                "auth",
                "/api/auth",
                wireMockServer.baseUrl(),
                false,
                false,
                null,
                100,
                true
        );

        // 2. Rota protegida de Itens
        RouteDefinition rItens = new RouteDefinition(
                "itens",
                "/api/itens",
                wireMockServer.baseUrl(),
                false,
                true,
                null,
                100,
                true
        );

        // 3. Rota restrita por role ADMIN
        RouteDefinition rAdminOnly = new RouteDefinition(
                "admin-ocorrencias",
                "/api/admin/ocorrencias",
                wireMockServer.baseUrl(),
                false,
                true,
                "ADMIN",
                100,
                true
        );

        // 4. Rota com rate limit baixo (2 reqs/min)
        RouteDefinition rRateLimited = new RouteDefinition(
                "limitada",
                "/api/limitada",
                wireMockServer.baseUrl(),
                false,
                false,
                null,
                2,
                true
        );

        // 5. Rota apontando para serviço indisponível
        RouteDefinition rIndisponivel = new RouteDefinition(
                "fora-do-ar",
                "/api/fora-do-ar",
                "http://127.0.0.1:59998",
                false,
                false,
                null,
                100,
                true
        );

        routeRepository.saveAll(List.of(rAuth, rItens, rAdminOnly, rRateLimited, rIndisponivel));
        routeResolverService.reload();
    }

    private String generateToken(String userId, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(key)
                .compact();
    }

    @Test
    void rotaPublicaDeveRepassarComSucessoSemToken() throws Exception {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/api/auth/login"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"abc-token\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teste@udesc.br\",\"senha\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.token").value("abc-token"));

        wireMockServer.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/auth/login"))
                .withHeader("X-Correlation-Id", WireMock.matching(".+")));
    }

    @Test
    void rotaProtegidaSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/itens"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void rotaProtegidaComTokenValidoDeveRepassarEInjetarHeadersDoUsuario() throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/itens"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"nome\":\"Mochila preta\"}]")));

        String token = generateToken("aluno-42", List.of("ALUNO"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/itens")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "tentativa-spoofing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Mochila preta"));

        // Anti-spoofing: X-User-Id recebido pelo serviço de destino DEVE ser aluno-42, e não tentativa-spoofing
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/itens"))
                .withHeader("X-User-Id", WireMock.equalTo("aluno-42"))
                .withHeader("X-User-Roles", WireMock.equalTo("ALUNO")));
    }

    @Test
    void rotaComRoleExigidaDeveRetornar403QuandoUsuarioNaoTemPermissao() throws Exception {
        String tokenSemAdmin = generateToken("aluno-42", List.of("ALUNO"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/ocorrencias")
                        .header("Authorization", "Bearer " + tokenSemAdmin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Sem permissão"));
    }

    @Test
    void rotaInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Rota não encontrada"));
    }

    @Test
    void destinoParadoDeveRetornar502() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/fora-do-ar"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Serviço não respondeu"));
    }

    @Test
    void estourarRateLimitDeveRetornar429ComHeaderRetryAfter() throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/limitada"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

        // Limite = 2 req/min
        mockMvc.perform(MockMvcRequestBuilders.get("/api/limitada")).andExpect(status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/limitada")).andExpect(status().isOk());

        // 3ª requisição deve estourar
        mockMvc.perform(MockMvcRequestBuilders.get("/api/limitada"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void requisicoesDevemGerarLogsDeAuditoria() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rota-que-nao-existe"));

        assertFalse(requestLogRepository.findAll().isEmpty());
    }
}
