package com.mypelink.backend.usuarios.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.repository.RoleRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private WebSocketNotificationService webSocketNotificationService;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByNombre("ROLE_ESTUDIANTE").isEmpty())
            roleRepository.save(Role.builder().nombre("ROLE_ESTUDIANTE").build());
        if (roleRepository.findByNombre("ROLE_MYPE").isEmpty())
            roleRepository.save(Role.builder().nombre("ROLE_MYPE").build());
        if (roleRepository.findByNombre("ROLE_ADMIN").isEmpty())
            roleRepository.save(Role.builder().nombre("ROLE_ADMIN").build());
        em.flush();
    }

    private String createAdminToken() throws Exception {
        var json = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"88888888","nombre":"Admin Test","email":"admin.test@upn.pe",
                                 "password":"Admin@1234","telefono":"999888888","codigoEstudiante":"N00888888",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var usuarioId = Long.valueOf(objectMapper.readValue(json, Map.class).get("usuarioId").toString());
        var adminRole = roleRepository.findByNombre("ROLE_ADMIN").orElseThrow();
        var usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.setRol(adminRole);
        usuarioRepository.save(usuario);
        em.flush();

        var loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin.test@upn.pe","password":"Admin@1234","rememberMe":false}"""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + ((String) objectMapper.readValue(loginJson, Map.class).get("token"));
    }

    @Test
    void listarUsuarios_Success() throws Exception {
        var token = createAdminToken();
        mockMvc.perform(get("/api/admin/usuarios")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listarEstudiantes_Success() throws Exception {
        var token = createAdminToken();
        mockMvc.perform(get("/api/admin/estudiantes")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void dashboardStats_Success() throws Exception {
        var token = createAdminToken();
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void toggleUsuarioEstado_Success() throws Exception {
        var token = createAdminToken();
        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"99999999","nombre":"Est Toggle","email":"est.toggle@upn.pe",
                                 "password":"Pass@1234","telefono":"999999999","codigoEstudiante":"N00999999",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var usuarioId = objectMapper.readValue(estJson, Map.class).get("usuarioId");

        mockMvc.perform(patch("/api/admin/usuarios/" + usuarioId + "/estado")
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void bypassLimite_Success() throws Exception {
        var token = createAdminToken();
        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"12121212","nombre":"Est Bypass","email":"est.bypass@upn.pe",
                                 "password":"Pass@1234","telefono":"999121212","codigoEstudiante":"N00121212",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var usuarioId = objectMapper.readValue(estJson, Map.class).get("usuarioId");

        mockMvc.perform(patch("/api/admin/usuarios/" + usuarioId + "/bypass-limite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nuevoLimite", 1))))
                .andExpect(status().isNoContent());
    }

    @Test
    void bypassLimite_ShouldFail_WhenInvalidLimit() throws Exception {
        var token = createAdminToken();
        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"13131313","nombre":"Est Invalid","email":"est.invalid@upn.pe",
                                 "password":"Pass@1234","telefono":"999131313","codigoEstudiante":"N00131313",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var usuarioId = objectMapper.readValue(estJson, Map.class).get("usuarioId");

        mockMvc.perform(patch("/api/admin/usuarios/" + usuarioId + "/bypass-limite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nuevoLimite", 5))))
                .andExpect(status().isBadRequest());
    }
}
