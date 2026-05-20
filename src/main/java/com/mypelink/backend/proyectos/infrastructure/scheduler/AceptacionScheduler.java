package com.mypelink.backend.proyectos.infrastructure.scheduler;

import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j  // Lombok genera un logger: log.info(), log.warn(), etc.
public class AceptacionScheduler {

    private final PostulacionRepository postulacionRepository;
    private final NotificacionService notificacionService;

    @Scheduled(fixedDelay = 15 * 60 * 1000) // cada 15 minutos en milisegundos
    @Transactional
    public void procesarPostulacionesExpiradas() {
        LocalDateTime ahora = LocalDateTime.now();
        log.info("[AceptacionScheduler] Ejecutando revisión de postulaciones expiradas: {}", ahora);

        // ── Caso 1: MYPE no respondió en 12h ─────────────────────
        // El admin preseleccionó a un estudiante, pero la MYPE no validó ni rechazó.
        List<Postulacion> expiradasMype = postulacionRepository
                .findExpiradasEnEstado(EstadoPostulacion.PRESELECCIONADO, ahora);

        for (Postulacion p : expiradasMype) {
            log.info("[AceptacionScheduler] Expirando PRESELECCIONADO postulacion id={}", p.getId());

            p.setEstado(EstadoPostulacion.EXPIRADO);
            p.setFechaLimiteConfirmacion(null);
            postulacionRepository.save(p);

            // Notificar a la MYPE (debería haber respondido)
            notificacionService.crearNotificacion(
                    p.getProyecto().getMype().getUsuario(),
                    "Plazo vencido — selección liberada",
                    "No respondiste a la selección del administrador para \""
                            + p.getProyecto().getTitulo() + "\" en el plazo de 12h. "
                            + "El cupo quedó libre y el admin puede seleccionar otro candidato.",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + p.getProyecto().getId()
            );

            // Notificar al estudiante que quedó libre
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "Tu preselección expiró",
                    "La empresa no confirmó tu selección para \""
                            + p.getProyecto().getTitulo() + "\" en el tiempo establecido. "
                            + "Puedes seguir postulando a otros proyectos.",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
        }

        // ── Caso 2: Estudiante no respondió en 12h ────────────────
        // La MYPE validó la selección, pero el estudiante no confirmó ni rechazó.
        List<Postulacion> expiradasEstudiante = postulacionRepository
                .findExpiradasEnEstado(EstadoPostulacion.VALIDADO_MYPE, ahora);

        for (Postulacion p : expiradasEstudiante) {
            log.info("[AceptacionScheduler] Expirando VALIDADO_MYPE postulacion id={}", p.getId());

            p.setEstado(EstadoPostulacion.EXPIRADO);
            p.setFechaLimiteConfirmacion(null);
            postulacionRepository.save(p);

            // Notificar a la MYPE para que el admin reinicie la selección
            notificacionService.crearNotificacion(
                    p.getProyecto().getMype().getUsuario(),
                    "El estudiante no confirmó a tiempo",
                    p.getEstudiante().getUsuario().getNombre()
                            + " no confirmó su participación en \""
                            + p.getProyecto().getTitulo() + "\" dentro de las 12h. "
                            + "El cupo quedó libre.",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + p.getProyecto().getId()
            );

            // Notificar al estudiante
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "Tu oferta expiró",
                    "No confirmaste tu participación en \""
                            + p.getProyecto().getTitulo() + "\" dentro de las 12h. "
                            + "El cupo fue liberado.",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
        }

        log.info("[AceptacionScheduler] Fin: {} MYPE expiradas, {} estudiante expiradas",
                expiradasMype.size(), expiradasEstudiante.size());
    }
}