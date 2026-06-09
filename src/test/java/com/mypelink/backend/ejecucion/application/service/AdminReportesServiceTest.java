package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.ejecucion.application.dto.AdminReporteResponse;
import com.mypelink.backend.ejecucion.application.service.AdminReportesService;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportesServiceTest {

    @Mock private CalificacionRepository calificacionRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private ProyectoRepository proyectoRepository;

    @InjectMocks
    private AdminReportesService adminReportesService;

    @Test
    void obtenerReportesYStats_Success() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Object[] rowMypeAEst = {1L, now, "Proyecto A", 10L, "Estudiante A", 5};
        Object[] rowEstAMype = {2L, now, "Proyecto B", "Estudiante B", 20L, 4};

        when(calificacionRepository.findMypeAEstudiante()).thenReturn(Collections.singletonList(rowMypeAEst));
        when(calificacionRepository.findEstudianteAMype()).thenReturn(Collections.singletonList(rowEstAMype));
        when(mypeRepository.findByUsuarioIdIn(anySet())).thenReturn(List.of());
        when(calificacionRepository.promedioGeneral()).thenReturn(4.5);
        when(calificacionRepository.promedioMypeAEstudiante()).thenReturn(4.3);
        when(calificacionRepository.promedioEstudianteAMype()).thenReturn(4.7);
        when(proyectoRepository.findAllConMype()).thenReturn(List.of(
                Proyecto.builder().id(1L).areaSistemas(AreaSistemas.DESARROLLO_WEB).build()
        ));

        AdminReporteResponse response = adminReportesService.obtenerReportesYStats();

        assertEquals(2, response.reportes().size());
        assertEquals(4.5, response.promedioGeneral());
        assertEquals(4.3, response.promedioMypeAEstudiante());
        assertEquals(4.7, response.promedioEstudianteAMype());
        assertEquals(2, response.totalCalificaciones());
        assertEquals(1, response.distribucionAreas().size());
    }
}
