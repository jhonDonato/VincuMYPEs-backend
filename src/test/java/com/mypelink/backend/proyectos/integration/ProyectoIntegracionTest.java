package com.mypelink.backend.proyectos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProyectoIntegracionTest {

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
    void crearProyecto_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Crear","email":"mype.crear@email.com","password":"Pass@1234",
                                 "telefono":"999101010","nombreComercial":"Crear SAC","razonSocial":"Crear SAC",
                                 "ruc":"20101010101","rubro":"Tecnologia","direccion":"Av. 1"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var token = approveAndLoginMype("mype.crear@email.com", "Pass@1234");

        mockMvc.perform(post("/api/proyectos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Proyecto Integracion","descripcion":"Descripcion del proyecto",
                                 "objetivo":"Objetivo","areaSistemas":"DESARROLLO_WEB","cupos":3}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void crearYPublicarProyecto_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Pub","email":"mype.pub@email.com","password":"Pass@1234",
                                 "telefono":"999202020","nombreComercial":"Pub SAC","razonSocial":"Pub SAC",
                                 "ruc":"20202020202","rubro":"Tecnologia","direccion":"Av. 2"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var token = approveAndLoginMype("mype.pub@email.com", "Pass@1234");

        var crearJson = mockMvc.perform(post("/api/proyectos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Proyecto Publicar","descripcion":"Desc",
                                 "areaSistemas":"DESARROLLO_WEB","cupos":2}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var id = objectMapper.readValue(crearJson, Map.class).get("id");

        mockMvc.perform(patch("/api/proyectos/" + id + "/publicar")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void postularAProyecto_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype Post","email":"mype.post@email.com","password":"Pass@1234",
                                 "telefono":"999303030","nombreComercial":"Post SAC","razonSocial":"Post SAC",
                                 "ruc":"20303030303","rubro":"Tecnologia","direccion":"Av. 3"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var mypeToken = approveAndLoginMype("mype.post@email.com", "Pass@1234");

        var proyJson = mockMvc.perform(post("/api/proyectos")
                        .header("Authorization", mypeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Proyecto Postular","descripcion":"Desc",
                                 "areaSistemas":"DESARROLLO_WEB","cupos":2}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var proyId = objectMapper.readValue(proyJson, Map.class).get("id");

        mockMvc.perform(patch("/api/proyectos/" + proyId + "/publicar")
                        .header("Authorization", mypeToken)).andExpect(status().isOk());

        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"60606060","nombre":"Est Postular","email":"est.postular@upn.pe",
                                 "password":"Pass@1234","telefono":"999606060","codigoEstudiante":"N00606060",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var estToken = "Bearer " + ((String) objectMapper.readValue(estJson, Map.class).get("token"));

        mockMvc.perform(post("/api/proyectos/" + proyId + "/postular")
                        .header("Authorization", estToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensajePostulacion\":\"Me interesa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void listarProyectosPublicos_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register/mype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mype List","email":"mype.list@email.com","password":"Pass@1234",
                                 "telefono":"999404040","nombreComercial":"List SAC","razonSocial":"List SAC",
                                 "ruc":"20404040404","rubro":"Tecnologia","direccion":"Av. 4"}"""))
                .andExpect(status().isCreated());
        em.flush();
        var mypeToken = approveAndLoginMype("mype.list@email.com", "Pass@1234");

        var proyJson = mockMvc.perform(post("/api/proyectos")
                        .header("Authorization", mypeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Proyecto Listar","descripcion":"Desc",
                                 "areaSistemas":"DESARROLLO_WEB","cupos":3}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var proyId = objectMapper.readValue(proyJson, Map.class).get("id");

        mockMvc.perform(patch("/api/proyectos/" + proyId + "/publicar")
                        .header("Authorization", mypeToken)).andExpect(status().isOk());

        var estJson = mockMvc.perform(post("/api/auth/register/estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":"70707070","nombre":"Est Listar","email":"est.listar@upn.pe",
                                 "password":"Pass@1234","telefono":"999707070","codigoEstudiante":"N00707070",
                                 "carrera":"Ing. Sistemas","universidad":"UPN"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        em.flush();
        var estToken = "Bearer " + ((String) objectMapper.readValue(estJson, Map.class).get("token"));

        mockMvc.perform(get("/api/proyectos").header("Authorization", estToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
