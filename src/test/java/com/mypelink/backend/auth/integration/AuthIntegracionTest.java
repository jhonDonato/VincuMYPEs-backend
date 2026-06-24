package com.mypelink.backend.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService;
import com.mypelink.backend.usuarios.domain.model.EstadoMype;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MypeRepository mypeRepository;

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
        em.flush();
    }

    @Test
    void registerEstudiante_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"11111111","nombre":"Est Integ","email":"est.integ@upn.pe",
                                 "password":"Pass@1234","telefono":"999111222","codigoEstudiante":"N00111111",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void registerMype_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Integ","email":"mype.integ@email.com","password":"Pass@1234",
                                 "telefono":"999333444","nombreComercial":"Integ SAC","razonSocial":"Integ SAC",
                                 "ruc":"20123456789","rubro":"Tecnologia","direccion":"Av. Principal"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void login_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Login2","email":"mype.login2@email.com","password":"Pass@1234",
                                 "telefono":"999555666","nombreComercial":"Login SAC","razonSocial":"Login SAC",
                                 "ruc":"20555555555","rubro":"Servicios","direccion":"Av. 123"}"""))
                .andExpect(status().isCreated());
        em.flush();

        var usuario = usuarioRepository.findByEmailWithRole("mype.login2@email.com").orElseThrow();
        var mype = mypeRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        mype.setEstado(EstadoMype.APROBADO);
        mypeRepository.save(mype);
        em.flush();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"mype.login2@email.com","password":"Pass@1234","rememberMe":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("MYPE"));
    }

    @Test
    void login_ShouldFail_WhenInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"noexiste@email.com","password":"WrongPass1","rememberMe":false}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_Success() throws Exception {
        var json = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"22222222","nombre":"Est Refresh","email":"est.refresh@upn.pe",
                                 "password":"Pass@1234","telefono":"999777888","codigoEstudiante":"N00222222",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var response = objectMapper.readValue(json, Map.class);
        var refreshToken = response.get("refreshToken");
        if (refreshToken == null) return;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void logout_Success() throws Exception {
        var json = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"33333333","nombre":"Est Logout","email":"est.logout@upn.pe",
                                 "password":"Pass@1234","telefono":"999000111","codigoEstudiante":"N00333333",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var token = objectMapper.readValue(json, Map.class).get("token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk());
    }
}
