package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.ejecucion.application.dto.AdminReporteResponse;
import com.mypelink.backend.ejecucion.application.dto.AreaDistribucionResponse;
import com.mypelink.backend.ejecucion.application.dto.EvaluacionDetalleResponse;
import com.mypelink.backend.ejecucion.domain.model.Evaluacion;
import com.mypelink.backend.ejecucion.domain.repository.EvaluacionRepository;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportesService {

    private final EvaluacionRepository evaluacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final MypeRepository mypeRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public AdminReporteResponse obtenerReportesYStats() {
        List<Evaluacion> evaluaciones = evaluacionRepository.findAllWithDetails();
        List<Proyecto> proyectos = proyectoRepository.findAllConMype();

        // 1. Mapear Evaluaciones
        List<EvaluacionDetalleResponse> reportes = evaluaciones.stream().map(e -> {
            String id = "REP-" + String.format("%03d", e.getId());
            String proyecto = e.getProyecto().getTitulo();
            String mype = e.getEvaluadoPor().getNombreComercial();
            String estudiante = e.getEstudiante().getUsuario().getNombre();

            int duracionDias = 15;
            if (e.getProyecto().getFechaInicio() != null && e.getProyecto().getFechaLimite() != null) {
                duracionDias = (int) ChronoUnit.DAYS.between(
                        e.getProyecto().getFechaInicio(),
                        e.getProyecto().getFechaLimite()
                );
            }

            double calificacion = (e.getPuntualidad() + e.getCalidadTrabajo() + e.getComunicacion()) / 3.0;
            calificacion = Math.round(calificacion * 10.0) / 10.0;

            return new EvaluacionDetalleResponse(
                    id, proyecto, mype, estudiante,
                    e.getFechaEvaluacion(), duracionDias, calificacion, e.getObservaciones()
            );
        }).collect(Collectors.toList());

        // 2. KPIs principales
        double satisfaccionPromedio = evaluaciones.isEmpty() ? 5.0 :
                Math.round((evaluaciones.stream()
                            .mapToDouble(e -> (e.getPuntualidad() + e.getCalidadTrabajo() + e.getComunicacion()) / 3.0)
                            .sum() / evaluaciones.size()) * 10.0) / 10.0;

        long totalDays = 0, completedCount = 0;
        for (Proyecto p : proyectos) {
            if (p.getEstado() == WorkflowEstado.COMPLETADO && p.getFechaInicio() != null && p.getFechaLimite() != null) {
                totalDays += ChronoUnit.DAYS.between(p.getFechaInicio(), p.getFechaLimite());
                completedCount++;
            }
        }
        int tiempoPromedio = completedCount > 0 ? (int) (totalDays / completedCount) : 15;

        long totalIniciados = proyectos.stream().filter(p -> p.getEstado() != WorkflowEstado.BORRADOR).count();
        long totalCompletados = proyectos.stream().filter(p -> p.getEstado() == WorkflowEstado.COMPLETADO).count();
        double tasaExito = totalIniciados > 0 ? Math.round((double) totalCompletados / totalIniciados * 1000.0) / 10.0 : 100.0;

        // 3. Conteos para KPIs del dashboard
        long totalMypes = mypeRepository.count();
        long estudiantesActivos = estudianteRepository.count(); // si hay campo activo, se puede filtrar
        long proyectosEnDesarrollo = proyectoRepository.countByEstadoAndActivoTrue(WorkflowEstado.EN_DESARROLLO);
        long totalEvaluaciones = evaluacionRepository.count();

        // 4. Distribución por área
        Map<AreaSistemas, Long> conteoAreas = proyectos.stream()
                .collect(Collectors.groupingBy(Proyecto::getAreaSistemas, Collectors.counting()));
        long totalProyectos = proyectos.size();
        List<AreaDistribucionResponse> distribucion = conteoAreas.entrySet().stream()
                .map(entry -> {
                    AreaSistemas area = entry.getKey();
                    long cantidad = entry.getValue();
                    double porcentaje = totalProyectos > 0 ? Math.round((cantidad * 100.0 / totalProyectos) * 10.0) / 10.0 : 0.0;
                    return new AreaDistribucionResponse(area.name(), getLabel(area), cantidad, porcentaje);
                })
                .collect(Collectors.toList());

        return new AdminReporteResponse(
                reportes, satisfaccionPromedio, tiempoPromedio, tasaExito,
                totalMypes, estudiantesActivos, proyectosEnDesarrollo, totalEvaluaciones,
                distribucion
        );
    }

    private String getLabel(AreaSistemas area) {
        return switch (area) {
            case DESARROLLO_WEB -> "Desarrollo Web";
            case DESARROLLO_MOVIL -> "Desarrollo Móvil";
            case BASE_DE_DATOS -> "Base de Datos";
            case DESARROLLO_SOFTWARE -> "Desarrollo de Software";
            case ANALISIS_DATOS -> "Análisis de Datos";
            case SOPORTE_TI -> "Soporte TI";
            default -> area.name().replace("_", " ");
        };
    }
}