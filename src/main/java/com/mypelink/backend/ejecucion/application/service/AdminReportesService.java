package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.ejecucion.application.dto.AdminReporteResponse;
import com.mypelink.backend.ejecucion.application.dto.EvaluacionDetalleResponse;
import com.mypelink.backend.ejecucion.domain.model.Evaluacion;
import com.mypelink.backend.ejecucion.domain.repository.EvaluacionRepository;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportesService {

    private final EvaluacionRepository evaluacionRepository;
    private final ProyectoRepository proyectoRepository;

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
                    id,
                    proyecto,
                    mype,
                    estudiante,
                    e.getFechaEvaluacion(),
                    duracionDias,
                    calificacion,
                    e.getObservaciones()
            );
        }).collect(Collectors.toList());

        // 2. Calcular KPIs
        double satisfaccionPromedio = 5.0;
        if (!evaluaciones.isEmpty()) {
            double sumaCalificaciones = evaluaciones.stream()
                    .mapToDouble(e -> (e.getPuntualidad() + e.getCalidadTrabajo() + e.getComunicacion()) / 3.0)
                    .sum();
            satisfaccionPromedio = sumaCalificaciones / evaluaciones.size();
            satisfaccionPromedio = Math.round(satisfaccionPromedio * 10.0) / 10.0;
        }

        long totalDays = 0;
        long completedCount = 0;
        for (Proyecto p : proyectos) {
            if (p.getEstado() == WorkflowEstado.COMPLETADO && p.getFechaInicio() != null && p.getFechaLimite() != null) {
                totalDays += ChronoUnit.DAYS.between(p.getFechaInicio(), p.getFechaLimite());
                completedCount++;
            }
        }
        int tiempoPromedio = completedCount > 0 ? (int) (totalDays / completedCount) : 15;

        long totalIniciados = proyectos.stream()
                .filter(p -> p.getEstado() != WorkflowEstado.BORRADOR)
                .count();
        long totalCompletados = proyectos.stream()
                .filter(p -> p.getEstado() == WorkflowEstado.COMPLETADO)
                .count();
        double tasaExito = totalIniciados > 0 ? ((double) totalCompletados / totalIniciados) * 100.0 : 100.0;
        tasaExito = Math.round(tasaExito * 10.0) / 10.0;

        return new AdminReporteResponse(
                reportes,
                satisfaccionPromedio,
                tiempoPromedio,
                tasaExito
        );
    }
}
