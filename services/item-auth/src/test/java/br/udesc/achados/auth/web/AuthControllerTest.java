package br.udesc.achados.auth.web;

import br.udesc.achados.auth.domain.Role;
import br.udesc.achados.auth.domain.User;
import br.udesc.achados.auth.repository.UserRepository;
import br.udesc.achados.auth.service.JwtService;
import br.udesc.achados.auth.web.dto.LoginRequest;
import br.udesc.achados.auth.web.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void deveCadastrarUsuarioELogar() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Carlos", "carlos@udesc.br", "SenhaSegura1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.email").value("carlos@udesc.br"))
                .andExpect(jsonPath("$.role").value("USER"));

        LoginRequest loginReq = new LoginRequest("carlos@udesc.br", "SenhaSegura1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("carlos@udesc.br"));
    }

    @Test
    void deveBloquearAcessoAoMeSemTokenEResponderComToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        User user = new User("Fernanda", "fernanda@udesc.br", passwordEncoder.encode("Senha1234"), Role.USER);
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("fernanda@udesc.br"));
    }

    @Test
    void usuarioComumNaoDeveAcessarEndpointsDeAdmin() throws Exception {
        User user = new User("Lucas", "lucas@udesc.br", passwordEncoder.encode("Senha1234"), Role.USER);
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
