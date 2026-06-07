package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.ejecucion.application.dto.*;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportesService {

    private final CalificacionRepository calificacionRepository;
    private final MypeRepository mypeRepository;
    private final ProyectoRepository proyectoRepository; // solo para distribucionAreas

    @Transactional(readOnly = true)
    public AdminReporteResponse obtenerReportesYStats() {
        // 1. Obtener calificaciones MYPE → ESTUDIANTE
        List<Object[]> rowsMypeAEst = calificacionRepository.findMypeAEstudiante();
        Set<Long> idsMype = rowsMypeAEst.stream()
                .map(row -> (Long) row[3]) // calif.id (MYPE)
                .collect(Collectors.toSet());
        Map<Long, Mype> mypeMap = mypeRepository.findByUsuarioIdIn(idsMype).stream()
                .collect(Collectors.toMap(m -> m.getUsuario().getId(), m -> m));

        List<ReporteItemResponse> reportes = new ArrayList<>();
        for (Object[] row : rowsMypeAEst) {
            Long id = (Long) row[0];
            LocalDateTime fecha = convertToLocalDateTime(row[1]);
            String proyecto = (String) row[2];
            Long calificadorId = (Long) row[3];
            String estudiante = (String) row[4];
            Integer puntuacion = (Integer) row[5];

            Mype mype = mypeMap.get(calificadorId);
            String nombreMype = mype != null ? mype.getNombreComercial() : "MYPE desconocida";

            reportes.add(new ReporteItemResponse(
                    "CAL-" + row[0] + "-" + row[1], // ID provisional, mejor usar proyectoId y calificacionId
                    fecha,
                    proyecto,
                    nombreMype,
                    estudiante,
                    "MYPE a Estudiante",
                    puntuacion
            ));
        }

        // 2. Calificaciones ESTUDIANTE → MYPE
        List<Object[]> rowsEstAMype = calificacionRepository.findEstudianteAMype();
        Set<Long> idsMypeCalificados = rowsEstAMype.stream()
                .map(row -> (Long) row[4]) // califdo.id (MYPE)
                .collect(Collectors.toSet());
        Map<Long, Mype> mypeCalificadoMap = mypeRepository.findByUsuarioIdIn(idsMypeCalificados).stream()
                .collect(Collectors.toMap(m -> m.getUsuario().getId(), m -> m));

        for (Object[] row : rowsEstAMype) {
            Long id = (Long) row[0];
            LocalDateTime fecha = convertToLocalDateTime(row[1]);
            String proyecto = (String) row[2];
            String estudiante = (String) row[3];
            Long calificadoId = (Long) row[4];
            Integer puntuacion = (Integer) row[5];

            Mype mype = mypeCalificadoMap.get(calificadoId);
            String nombreMype = mype != null ? mype.getNombreComercial() : "MYPE desconocida";

            reportes.add(new ReporteItemResponse(
                    "CAL-" + row[0] + "-" + row[1],
                    fecha,
                    proyecto,
                    estudiante,
                    nombreMype,
                    "Estudiante a MYPE",
                    puntuacion
            ));
        }

        // Ordenar por fecha descendente
        reportes.sort((a, b) -> b.fecha().compareTo(a.fecha()));

        // 3. KPIs
        double promedioGeneral = calificacionRepository.promedioGeneral();
        double promedioMypeAEst = calificacionRepository.promedioMypeAEstudiante();
        double promedioEstAMype = calificacionRepository.promedioEstudianteAMype();
        long totalCalificaciones = reportes.size();

        // 4. Distribución por áreas (opcional, lo dejo si quieres mantenerlo)
        List<Proyecto> proyectos = proyectoRepository.findAllConMype();
        Map<AreaSistemas, Long> conteoAreas = proyectos.stream()
                .collect(Collectors.groupingBy(Proyecto::getAreaSistemas, Collectors.counting()));
        long totalProyectos = proyectos.size();
        List<AreaDistribucionResponse> distribucion = conteoAreas.entrySet().stream()
                .map(entry -> {
                    AreaSistemas area = entry.getKey();
                    long cantidad = entry.getValue();
                    double porcentaje = totalProyectos > 0 ?
                            Math.round((cantidad * 100.0 / totalProyectos) * 10.0) / 10.0 : 0.0;
                    return new AreaDistribucionResponse(area.name(), getLabel(area), cantidad, porcentaje);
                })
                .collect(Collectors.toList());

        return new AdminReporteResponse(
                reportes, promedioGeneral, promedioMypeAEst, promedioEstAMype,
                totalCalificaciones, distribucion
        );
    }

    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        else if (value instanceof LocalDateTime ldt) return ldt;
        else return null;
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