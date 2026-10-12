package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.web.dto.RouteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RouteAdminControllerTest {

    private static final String ADMIN_KEY = "admin-dev-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveRetornar401QuandoSemXAdminKeyOuChaveInvalida() throws Exception {
        mockMvc.perform(get("/gateway/admin/routes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/gateway/admin/routes")
                        .header("X-Admin-Key", "chave-errada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExecutarCrudCompletoDeRotasComChaveValida() throws Exception {
        // 1. Listar rotas padrão semeadas
        mockMvc.perform(get("/gateway/admin/routes")
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));

        // 2. Criar nova rota
        RouteRequest novaRota = new RouteRequest(
                "notificacoes",
                "/api/notificacoes",
                "http://localhost:8084",
                false,
                true,
                "ADMIN",
                30,
                true
        );

        String responseBody = mockMvc.perform(post("/gateway/admin/routes")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRota)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("notificacoes"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long idCriado = objectMapper.readTree(responseBody).get("id").asLong();

        // 3. Buscar rota por ID
        mockMvc.perform(get("/gateway/admin/routes/" + idCriado)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("notificacoes"));

        // 4. Atualizar rota
        RouteRequest rotaAtualizada = new RouteRequest(
                "notificacoes-v2",
                "/api/notificacoes",
                "http://localhost:8085",
                true,
                false,
                null,
                100,
                true
        );

        mockMvc.perform(put("/gateway/admin/routes/" + idCriado)
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rotaAtualizada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("notificacoes-v2"))
                .andExpect(jsonPath("$.targetUrl").value("http://localhost:8085"));

        // 5. Deletar rota
        mockMvc.perform(delete("/gateway/admin/routes/" + idCriado)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isNoContent());

        // 6. Verificar que foi removida
        mockMvc.perform(get("/gateway/admin/routes/" + idCriado)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveConsultarLogsDeAuditoria() throws Exception {
        mockMvc.perform(get("/gateway/admin/logs?page=0&size=10")
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
