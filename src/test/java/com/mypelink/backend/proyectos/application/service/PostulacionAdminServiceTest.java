package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.PostulacionAdminResponse;
import com.mypelink.backend.proyectos.application.service.PostulacionAdminService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostulacionAdminServiceTest {

    @Mock private PostulacionRepository postulacionRepository;

    @InjectMocks
    private PostulacionAdminService postulacionAdminService;

    @Test
    void buscar_Success() {
        var role = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        var usuario = Usuario.builder().id(1L).nombre("Est").email("est@test.com").rol(role).build();
        var estudiante = Estudiante.builder().id(1L).usuario(usuario).build();
        var mype = Mype.builder().id(1L).nombreComercial("MYPE SAS").build();
        var proyecto = Proyecto.builder().id(1L).titulo("Proyecto").mype(mype).areaSistemas(AreaSistemas.DESARROLLO_WEB).build();
        var postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.PENDIENTE)
                .fechaPostulacion(LocalDateTime.now()).build();

        when(postulacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(postulacion)));
        when(postulacionRepository.findProyectoIdsConEstado(anyList(), eq(EstadoPostulacion.PRESELECCIONADO)))
                .thenReturn(List.of());

        Page<PostulacionAdminResponse> result = postulacionAdminService.buscar(
                1L, null, null, null, null, null, null, 0, 10, null);

        assertFalse(result.isEmpty());
        assertEquals("Est", result.getContent().get(0).estudianteNombre());
    }
}
