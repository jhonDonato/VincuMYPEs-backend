package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.comunicacion.application.service.ChatGrupalService;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.dto.Decision;
import com.mypelink.backend.proyectos.application.dto.DecidirRequest;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDecisionService {

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final VotacionDelegadoRepository votacionDelegadoRepository;
    private final VotoDelegadoRepository votoDelegadoRepository;
    private final ProyectoService proyectoService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;
    private final ChatGrupalService chatGrupalService;   // ← nuevo

    @Transactional
    public void decidir(Long proyectoId, DecidirRequest request, String emailAdmin) {
        Usuario admin = usuarioRepository.findByEmailWithRole(emailAdmin)
                .orElseThrow(() -> new BusinessException("Administrador no encontrado"));

        if (!admin.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("Solo los administradores pueden tomar esta decisión");
        }

        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (proyecto.getEstado() != WorkflowEstado.PENDIENTE_ADMIN &&
                proyecto.getEstado() != WorkflowEstado.VACANTES_ABIERTAS) {
            throw new BusinessException("El proyecto no requiere una decisión administrativa en este momento");
        }

        switch (request.decision()) {
            case CONTINUAR -> continuar(proyecto, admin);
            case AMPLIAR -> ampliar(proyecto, request.diasExtra(), admin);
            case VACANTES -> abrirVacantes(proyecto, admin);
            case CANCELAR -> proyectoService.cancelarProyecto(proyectoId, emailAdmin);
        }
    }

    private void continuar(Proyecto proyecto, Usuario admin) {
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
        proyectoRepository.save(proyecto);

        // Crear chats grupales
        crearChats(proyecto);

        notificarEstudiantes(proyecto,
                "✅ Proyecto autorizado",
                "El administrador ha aprobado el inicio del proyecto \"" + proyecto.getTitulo() + "\". Ya puedes empezar a trabajar.");
    }

    private void ampliar(Proyecto proyecto, Integer diasExtra, Usuario admin) {
        if (diasExtra == null || diasExtra < 1) {
            throw new BusinessException("Debes indicar una cantidad válida de días extra (mayor a 0)");
        }

        if (proyecto.getFechaLimite() == null) {
            throw new BusinessException("El proyecto no tiene una fecha límite definida");
        }

        proyecto.setFechaAmpliada(proyecto.getFechaLimite().plusDays(diasExtra));
        proyecto.setFechaLimite(proyecto.getFechaAmpliada());
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
        proyectoRepository.save(proyecto);

        // Crear chats grupales
        crearChats(proyecto);

        notificarEstudiantes(proyecto,
                "📅 Plazo ampliado",
                "El administrador amplió el plazo de entrega del proyecto \"" + proyecto.getTitulo() + "\" en " + diasExtra + " día(s). Nueva fecha límite: " + proyecto.getFechaLimite());
    }

    private void abrirVacantes(Proyecto proyecto, Usuario admin) {
        VotacionDelegado votacion = votacionDelegadoRepository.findCompletadaByProyectoId(proyecto.getId())
                .orElseThrow(() -> new BusinessException("No se encontró la votación de delegado para este proyecto"));

        List<VotoDelegado> votos = votoDelegadoRepository.findByVotacionIdWithDetails(votacion.getId());
        Set<Long> idsVotantes = votos.stream()
                .map(v -> v.getVotante().getId())
                .collect(Collectors.toSet());

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyecto.getId(), EstadoPostulacion.CONFIRMADO);

        List<Postulacion> aExpulsar = confirmados.stream()
                .filter(p -> !idsVotantes.contains(p.getEstudiante().getId()))
                .toList();

        for (Postulacion p : aExpulsar) {
            p.setEstado(EstadoPostulacion.RECHAZADO);
            p.setObservacionesRespuesta("Expulsado por inactividad en la votación de delegado");
            p.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(p);

            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "⚠️ Has sido retirado del proyecto",
                    "No participaste en la votación de delegado y el administrador ha liberado tu cupo en \"" + proyecto.getTitulo() + "\".",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
            try {
                emailService.enviarCorreoNotificacion(
                        p.getEstudiante().getUsuario().getEmail(),
                        "Has sido retirado del proyecto",
                        "No participaste en la votación de delegado del proyecto \""
                                + proyecto.getTitulo() + "\". El administrador ha liberado tu cupo.\n\n"
                                + "Inicia sesión para ver tus proyectos disponibles: http://localhost:5173/login",
                        p.getEstudiante().getUsuario().getNombre()
                );
            } catch (Exception e) {
                log.error("Error al enviar email a estudiante expulsado: {}", e.getMessage());
            }
        }

        proyectoService.reasignarDelegadoSiEsNecesario(proyecto);

        proyecto.setFechaFinBusqueda(LocalDateTime.now().plusDays(3));
        proyecto.setEstado(WorkflowEstado.VACANTES_ABIERTAS);
        proyectoRepository.save(proyecto);

        List<Postulacion> restantes = confirmados.stream()
                .filter(p -> idsVotantes.contains(p.getEstudiante().getId()))
                .toList();
        for (Postulacion p : restantes) {
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "🔄 Buscando nuevos compañeros",
                    "El administrador ha abierto vacantes en \"" + proyecto.getTitulo() + "\" por inactividad. Se buscarán reemplazos durante 3 días.",
                    TipoNotificacion.PROYECTO,
                    "/workspace/" + proyecto.getId()
            );
        }

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "🔄 Se abrieron vacantes en tu proyecto",
                "El administrador expulsó a " + aExpulsar.size() + " estudiante(s) inactivo(s) en \"" + proyecto.getTitulo() + "\". Se están buscando reemplazos.",
                TipoNotificacion.PROYECTO,
                "/dashboard/proyectos"
        );
        try {
            emailService.enviarCorreoNotificacion(
                    proyecto.getMype().getUsuario().getEmail(),
                    "Se abrieron vacantes en tu proyecto",
                    "El administrador expulsó a " + aExpulsar.size() + " estudiante(s) por inactividad en la votación del proyecto \""
                            + proyecto.getTitulo() + "\". Se están buscando reemplazos durante 3 días.\n\n"
                            + "Inicia sesión para ver el estado: http://localhost:5173/login",
                    proyecto.getMype().getUsuario().getNombre()
            );
        } catch (Exception e) {
            log.error("Error al enviar email a la MYPE sobre apertura de vacantes: {}", e.getMessage());
        }
    }

    private void notificarEstudiantes(Proyecto proyecto, String titulo, String mensaje) {
        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyecto.getId(), EstadoPostulacion.CONFIRMADO);
        for (Postulacion p : confirmados) {
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(), titulo, mensaje,
                    TipoNotificacion.PROYECTO,
                    "/workspace/" + proyecto.getId()
            );
        }
    }

    private void crearChats(Proyecto proyecto) {
        log.info("🔧 AdminDecision: asegurando chats para proyecto {}", proyecto.getId());
        try {
            chatGrupalService.asegurarMiembrosEnChats(proyecto.getId());
            log.info("✅ AdminDecision: asegurarMiembrosEnChats ejecutado correctamente");
        } catch (Exception e) {
            log.error("❌ Error al asegurar miembros en los chats del proyecto {}: {}", proyecto.getId(), e.getMessage(), e);
        }
    }
}