package com.mypelink.backend.proyectos.infrastructure.scheduler;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AceptacionScheduler {

    private final PostulacionRepository postulacionRepository;
    private final NotificacionService notificacionService;
    private final ProyectoService proyectoService;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntregableRepository entregableRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void procesarPostulacionesExpiradas() {
        LocalDateTime ahora = LocalDateTime.now();
        log.info("[AceptacionScheduler] Ejecutando revisión de postulaciones expiradas: {}", ahora);

        // ── Caso 1: MYPE no respondió en 12h ─────────────────────
        List<Postulacion> expiradasMype = postulacionRepository
                .findExpiradasEnEstado(EstadoPostulacion.PRESELECCIONADO, ahora);

        for (Postulacion p : expiradasMype) {
            log.info("[AceptacionScheduler] Expirando PRESELECCIONADO postulacion id={}", p.getId());

            p.setEstado(EstadoPostulacion.EXPIRADO);
            p.setFechaLimiteConfirmacion(null);
            postulacionRepository.save(p);

            notificacionService.crearNotificacion(
                    p.getProyecto().getMype().getUsuario(),
                    "Plazo vencido — selección liberada",
                    "No respondiste a la selección del administrador para \""
                            + p.getProyecto().getTitulo() + "\" en el plazo de 12h. "
                            + "El cupo quedó libre y el admin puede seleccionar otro candidato.",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + p.getProyecto().getId()
            );

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
        List<Postulacion> expiradasEstudiante = postulacionRepository
                .findExpiradasEnEstado(EstadoPostulacion.VALIDADO_MYPE, ahora);

        for (Postulacion p : expiradasEstudiante) {
            log.info("[AceptacionScheduler] Expirando VALIDADO_MYPE postulacion id={}", p.getId());

            p.setEstado(EstadoPostulacion.EXPIRADO);
            p.setFechaLimiteConfirmacion(null);
            postulacionRepository.save(p);

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

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void procesarVacantesExpiradas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Proyecto> vacantesExpiradas = proyectoRepository
                .findByEstadoAndFechaFinBusquedaBefore(WorkflowEstado.VACANTES_ABIERTAS, ahora);

        for (Proyecto proyecto : vacantesExpiradas) {
            proyecto.setEstado(WorkflowEstado.PENDIENTE_ADMIN);
            proyecto.setFechaFinBusqueda(null);
            proyectoRepository.save(proyecto);

            List<Usuario> admins = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol().getNombre().equals("ROLE_ADMIN"))
                    .toList();

            for (Usuario admin : admins) {
                notificacionService.crearNotificacion(
                        admin,
                        "⏰ Plazo de búsqueda vencido",
                        "El proyecto \"" + proyecto.getTitulo() + "\" no ha encontrado reemplazos. Debes decidir cómo continuar.",
                        TipoNotificacion.PROYECTO,
                        "/admin/proyectos"
                );
                try {
                    emailService.enviarCorreoNotificacion(
                            admin.getEmail(),
                            "Plazo de búsqueda de vacantes vencido",
                            "El proyecto \"" + proyecto.getTitulo() + "\" no encontró reemplazos en el plazo de 3 días. "
                                    + "Debes ingresar al panel de administración para decidir cómo continuar.\n\n"
                                    + "Inicia sesión: http://localhost:5173/login",
                            admin.getNombre()
                    );
                } catch (Exception e) {
                    log.error("Error al enviar email a admin sobre vacantes vencidas: {}", e.getMessage());
                }
            }
        }
    }
    @Scheduled(fixedDelay = 300000) // cada 5 minutos
    @Transactional
    public void liberarBloqueosExpirados() {
        List<Entregable> expirados = entregableRepository.findByLockedAtBefore(LocalDateTime.now().minusHours(1));
        for (Entregable e : expirados) {
            log.warn("[AceptacionScheduler] Liberando bloqueo expirado en entregable id={}, proyecto={}",
                    e.getId(), e.getProyecto() != null ? e.getProyecto().getId() : "null");
            e.setLockedBy(null);
            e.setLockedAt(null);
        }
        entregableRepository.saveAll(expirados);
    }
}