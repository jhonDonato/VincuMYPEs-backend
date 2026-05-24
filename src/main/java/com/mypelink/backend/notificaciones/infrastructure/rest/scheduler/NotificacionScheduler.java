package com.mypelink.backend.notificaciones.infrastructure.rest.scheduler;

import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final NotificacionService notificacionService;

    // ✅ Cada hora: Notificar nuevos proyectos publicados
    @Scheduled(fixedRate = 3600000)
    @Transactional(readOnly = true)
    public void notificarNuevosProyectos() {
        List<Proyecto> proyectosRecientes = proyectoRepository
                .findByEstadoAndActivoTrue(WorkflowEstado.PENDIENTE);

        LocalDateTime haceUnaHora = LocalDateTime.now().minusHours(1);

        proyectosRecientes.stream()
                .filter(p -> p.getFechaCreacion() != null &&
                        p.getFechaCreacion().isAfter(haceUnaHora))
                .forEach(proyecto -> {
                    List<Estudiante> estudiantes = estudianteRepository.findAll();
                    estudiantes.forEach(estudiante -> {
                        notificacionService.crearNotificacion(
                                estudiante.getUsuario(),
                                "🔔 Nuevo proyecto disponible",
                                "La empresa \"" + proyecto.getMype().getNombreComercial() +
                                        "\" publicó: " + proyecto.getTitulo(),
                                TipoNotificacion.PROYECTO,
                                "/proyectos/" + proyecto.getId()
                        );
                    });
                });
    }

    // ✅ Cada 6 horas: Alertar entregables próximos a vencer (3 días antes)
    @Scheduled(fixedRate = 21600000)
    @Transactional(readOnly = true)
    public void alertarFechasLimiteEntregables() {
        List<Entregable> entregablesPendientes = entregableRepository.findAll();
        LocalDate enTresDias = LocalDate.now().plusDays(3);
        LocalDate ahora = LocalDate.now();

        entregablesPendientes.stream()
                .filter(e -> e.getProyecto().getFechaLimite() != null)
                .filter(e -> {
                    LocalDate fechaLimite = e.getProyecto().getFechaLimite();
                    return !fechaLimite.isAfter(enTresDias) && !fechaLimite.isBefore(ahora);
                })
                .forEach(entregable -> {
                    notificacionService.crearNotificacion(
                            entregable.getEstudiante().getUsuario(),
                            "⏰ Fecha límite próxima",
                            "El proyecto \"" + entregable.getProyecto().getTitulo() +
                                    "\" tiene fecha límite el " + entregable.getProyecto().getFechaLimite() +
                                    ". ¡No olvides subir tus entregables!",
                            TipoNotificacion.ALERTA,
                            "/workspace/" + entregable.getProyecto().getId()
                    );
                });
    }
}