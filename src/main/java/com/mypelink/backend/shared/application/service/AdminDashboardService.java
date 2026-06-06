package com.mypelink.backend.shared.application.service;

import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.application.dto.DashboardStatsResponse;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final CertificadoRepository certificadoRepository;
    private final CalificacionRepository calificacionRepository;

    public DashboardStatsResponse getStats() {
        long totalEstudiantes = usuarioRepository.countByRolNombre("ROLE_ESTUDIANTE");
        long totalMypes       = usuarioRepository.countByRolNombre("ROLE_MYPE");
        long totalAdmins      = usuarioRepository.countByRolNombre("ROLE_ADMIN");

        long proyectosActivos = proyectoRepository.countByEstadoInAndActivoTrue(
                List.of(WorkflowEstado.PENDIENTE, WorkflowEstado.EN_DESARROLLO));
        long proyectosCompletados = proyectoRepository.countByEstadoAndActivoTrue(WorkflowEstado.COMPLETADO);

        long postulacionesPendientes = postulacionRepository.countByEstado(EstadoPostulacion.PENDIENTE);
        long certificadosEmitidos    = certificadoRepository.count();

        Double promedioMypes       = calificacionRepository.promedioByCalificadoRol("ROLE_MYPE");
        Double promedioEstudiantes = calificacionRepository.promedioByCalificadoRol("ROLE_ESTUDIANTE");

        List<Object[]> rows = proyectoRepository.countGroupByAreaSistemas();
        Map<String, Long> proyectosPorArea = new LinkedHashMap<>();
        for (Object[] row : rows) {
            proyectosPorArea.put(((AreaSistemas) row[0]).name(), (Long) row[1]);
        }

        return new DashboardStatsResponse(
                totalEstudiantes, totalMypes, totalAdmins,
                proyectosActivos, proyectosCompletados,
                postulacionesPendientes, certificadosEmitidos,
                promedioMypes, promedioEstudiantes,
                proyectosPorArea
        );
    }
}
