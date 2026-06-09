package com.mypelink.backend.shared.application.service;

import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.application.dto.DashboardStatsResponse;
import com.mypelink.backend.shared.application.service.AdminDashboardService;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private CertificadoRepository certificadoRepository;
    @Mock private CalificacionRepository calificacionRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getStats_Success() {
        when(usuarioRepository.countByRolNombre("ROLE_ESTUDIANTE")).thenReturn(100L);
        when(usuarioRepository.countByRolNombre("ROLE_MYPE")).thenReturn(20L);
        when(usuarioRepository.countByRolNombre("ROLE_ADMIN")).thenReturn(3L);
        when(proyectoRepository.countByEstadoInAndActivoTrue(anyList())).thenReturn(15L);
        when(proyectoRepository.countByEstadoAndActivoTrue(WorkflowEstado.COMPLETADO)).thenReturn(30L);
        when(postulacionRepository.countByEstado(EstadoPostulacion.PENDIENTE)).thenReturn(45L);
        when(certificadoRepository.count()).thenReturn(60L);
        when(calificacionRepository.promedioByCalificadoRol("ROLE_MYPE")).thenReturn(4.2);
        when(calificacionRepository.promedioByCalificadoRol("ROLE_ESTUDIANTE")).thenReturn(4.5);
        when(proyectoRepository.countGroupByAreaSistemas()).thenReturn(List.of(
                new Object[]{AreaSistemas.DESARROLLO_WEB, 10L},
                new Object[]{AreaSistemas.DESARROLLO_MOVIL, 5L}
        ));

        DashboardStatsResponse stats = adminDashboardService.getStats();

        assertEquals(100, stats.totalEstudiantes());
        assertEquals(20, stats.totalMypes());
        assertEquals(3, stats.totalAdmins());
        assertEquals(15, stats.proyectosActivos());
        assertEquals(30, stats.proyectosCompletados());
        assertEquals(45, stats.postulacionesPendientes());
        assertEquals(60, stats.certificadosEmitidos());
        assertEquals(4.2, stats.promedioCalificacionMypes());
        assertEquals(4.5, stats.promedioCalificacionEstudiantes());
        assertEquals(2, stats.proyectosPorArea().size());
    }
}
