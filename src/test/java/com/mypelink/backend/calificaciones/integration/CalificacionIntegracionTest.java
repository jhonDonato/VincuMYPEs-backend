package com.mypelink.backend.calificaciones.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalificacionIntegracionTest {

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

    private String approveAndLoginMype(String email, String password) throws Exception {
        var usuario = usuarioRepository.findByEmailWithRole(email).orElseThrow();
        var mype = mypeRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        mype.setEstado(EstadoMype.APROBADO);
        mypeRepository.save(mype);
        em.flush();

        var loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + ((String) objectMapper.readValue(loginJson, Map.class).get("token"));
    }

    @Test
    void pendientes_AsMype_ReturnsList() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Cali1","email":"mype.cali1@email.com","password":"Pass@1234",
                                 "telefono":"999505050","nombreComercial":"Cali SAC","razonSocial":"Cali SAC",
                                 "ruc":"20505050501","rubro":"Servicios","direccion":"Av. 5"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var mypeToken = approveAndLoginMype("mype.cali1@email.com", "Pass@1234");

        mockMvc.perform(get("/api/calificaciones/me/pendientes")
                        .header("Authorization", mypeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerRating_AsMypeOfStudent_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Cali2","email":"mype.cali2@email.com","password":"Pass@1234",
                                 "telefono":"999606060","nombreComercial":"Rating SAC","razonSocial":"Rating SAC",
                                 "ruc":"20606060601","rubro":"Servicios","direccion":"Av. 6"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var mypeToken = approveAndLoginMype("mype.cali2@email.com", "Pass@1234");

        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"80808080","nombre":"Est Rating","email":"est.rating@upn.pe",
                                 "password":"Pass@1234","telefono":"999808080","codigoEstudiante":"N00808080",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var estUsuarioId = objectMapper.readValue(estJson, Map.class).get("usuarioId");

        mockMvc.perform(get("/api/calificaciones/usuarios/" + estUsuarioId + "/rating")
                        .header("Authorization", mypeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }
}
