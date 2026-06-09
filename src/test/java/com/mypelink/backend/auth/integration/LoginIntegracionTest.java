package com.mypelink.backend.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService;
import com.mypelink.backend.usuarios.application.dto.LoginRequest;
import com.mypelink.backend.usuarios.application.dto.RegisterEstudianteRequest;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private WebSocketNotificationService webSocketNotificationService;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByNombre("ROLE_ESTUDIANTE").isEmpty()) {
            Role role = new Role();
            role.setNombre("ROLE_ESTUDIANTE");
            roleRepository.save(role);
        }

        // Register user for login
        RegisterEstudianteRequest registerRequest = new RegisterEstudianteRequest(
                "88888888", 
                "Login Test", 
                "login@upn.pe", 
                "Login@1234", 
                "987654321", 
                "N00888888", 
                "Ingeniería de Sistemas", 
                "UPN"
        );

        mockMvc.perform(post("/api/auth/register/estudiante")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("login@upn.pe", "Login@1234", false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }
}
