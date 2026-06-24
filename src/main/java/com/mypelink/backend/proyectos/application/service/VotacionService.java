package com.mypelink.backend.proyectos.application.service;

import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.dto.VotacionResponse;
import com.mypelink.backend.proyectos.application.dto.VotarRequest;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.domain.enums.FaseVotacion;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.comunicacion.application.service.ChatGrupalService;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import java.util.Set;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor
public class VotacionService {
    private static final Logger log = LoggerFactory.getLogger(VotacionService.class);

    private final VotacionDelegadoRepository votacionRepository;
    private final VotoDelegadoRepository votoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final ChatGrupalService chatGrupalService;

    private static final int HORAS_VOTACION = 48;

    // ═══════════════════════════════════════════════════════════
    // INICIAR VOTACIÓN
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public VotacionResponse iniciarVotacion(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (votacionRepository.existsByProyectoId(proyectoId)) {
            throw new BusinessException("Ya existe una votación para este proyecto");
        }

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO);

        // ✅ 1 estudiante → Delegado automático
        if (confirmados.size() == 1) {
            return asignarDelegadoAutomatico(proyecto, confirmados);
        }

        // ✅ 2 estudiantes → Azar (sin votación)
        if (confirmados.size() == 2) {
            return elegirDelegadoAlAzar(proyecto, confirmados);
        }

        // ✅ 3+ estudiantes → Votación normal
        VotacionDelegado votacion = VotacionDelegado.builder()
                .proyecto(proyecto)
                .fechaLimite(LocalDateTime.now().plusHours(HORAS_VOTACION))
                .estado(FaseVotacion.EN_VOTACION)
                .build();
        votacion = votacionRepository.save(votacion);

        proyecto.setFaseVotacion(FaseVotacion.EN_VOTACION);
        proyectoRepository.save(proyecto);

        for (Postulacion p : confirmados) {
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "🗳️ ¡Votación de delegado iniciada!",
                    "Ya puedes votar por el delegado del proyecto \"" + proyecto.getTitulo() +
                            "\". Tienes " + HORAS_VOTACION + " horas para votar.",
                    TipoNotificacion.PROYECTO,
                    "/workspace/" + proyectoId
            );
        }

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "🗳️ Votación de delegado iniciada",
                "Los estudiantes de \"" + proyecto.getTitulo() +
                        "\" están votando por su delegado.",
                TipoNotificacion.PROYECTO,
                "/dashboard/mype/ejecucion"
        );

        return buildVotacionResponse(votacion, null);
    }

    // ═══════════════════════════════════════════════════════════
    // VOTAR
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public VotacionResponse votar(Long proyectoId, VotarRequest request, String emailEstudiante) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Estudiante votante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        VotacionDelegado votacion = votacionRepository.findActivaByProyectoId(proyectoId)
                .orElseThrow(() -> new BusinessException("No hay votación activa para este proyecto"));

        if (LocalDateTime.now().isAfter(votacion.getFechaLimite())) {
            finalizarVotacion(votacion);
            throw new BusinessException("La votación ya ha expirado");
        }

        Postulacion postulacionVotante = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, votante.getId())
                .orElseThrow(() -> new BusinessException("No eres parte de este proyecto"));

        if (postulacionVotante.getEstado() != EstadoPostulacion.CONFIRMADO) {
            throw new BusinessException("Solo estudiantes confirmados pueden votar");
        }

        if (votoRepository.existsByVotacionIdAndVotanteId(votacion.getId(), votante.getId())) {
            throw new BusinessException("Ya has votado en esta elección");
        }

        Estudiante candidato = estudianteRepository.findById(request.candidatoId())
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", request.candidatoId()));

        boolean candidatoConfirmado = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, candidato.getId())
                .map(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                .orElse(false);

        if (!candidatoConfirmado) {
            throw new BusinessException("El candidato seleccionado no es válido");
        }

        votoRepository.save(VotoDelegado.builder()
                .votacion(votacion)
                .votante(votante)
                .candidato(candidato)
                .build());

        // ✅ SOLO VERIFICAMOS SI TODOS VOTARON (sin mayoría automática)
        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO);

        long totalVotos = votoRepository.findByVotacionIdWithDetails(votacion.getId()).size();

        if (totalVotos >= confirmados.size()) {
            return finalizarVotacion(votacion);
        }

        return buildVotacionResponse(votacion, votante.getId());
    }

    @Transactional
    public VotacionResponse proponerseComoCandidato(Long proyectoId, String emailEstudiante) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        VotacionDelegado votacion = votacionRepository.findActivaByProyectoId(proyectoId)
                .orElseThrow(() -> new BusinessException("No hay votación activa para este proyecto"));

        // Verificar que el estudiante pertenece al proyecto y está confirmado
        Postulacion postulacion = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())
                .orElseThrow(() -> new BusinessException("No eres parte de este proyecto"));

        if (postulacion.getEstado() != EstadoPostulacion.CONFIRMADO) {
            throw new BusinessException("Solo estudiantes confirmados pueden postularse");
        }

        // No es necesario hacer nada más, los candidatos son todos los confirmados
        // Este endpoint simplemente confirma que el estudiante quiere participar
        // y devuelve el estado actual de la votación

        return buildVotacionResponse(votacion, estudiante.getId());
    }

    // ═══════════════════════════════════════════════════════════
    // OBTENER ESTADO DE VOTACIÓN
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public VotacionResponse obtenerVotacion(Long proyectoId, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        VotacionDelegado votacion = votacionRepository.findActivaByProyectoId(proyectoId)
                .orElse(null);

        if (votacion == null) {
            votacion = votacionRepository.findCompletadaByProyectoId(proyectoId)
                    .orElseThrow(() -> new BusinessException("No hay votación para este proyecto"));
        }

        Long estudianteId = null;
        if (usuario.getRol().getNombre().equals("ROLE_ESTUDIANTE") ||
                usuario.getRol().getNombre().equals("ESTUDIANTE")) {
            Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                    .orElse(null);
            if (estudiante != null) {
                estudianteId = estudiante.getId();
            }
        }

        return buildVotacionResponse(votacion, estudianteId);
    }

    @Transactional
    public VotacionResponse finalizarVotacion(VotacionDelegado votacion) {
        Proyecto proyecto = votacion.getProyecto();

        List<VotoDelegado> votos = votoRepository.findByVotacionIdWithDetails(votacion.getId());

        Map<Long, Long> conteo = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();

        for (VotoDelegado voto : votos) {
            Long candidatoId = voto.getCandidato().getId();
            conteo.put(candidatoId, conteo.getOrDefault(candidatoId, 0L) + 1);
            nombres.put(candidatoId, voto.getCandidato().getUsuario().getNombre());
        }

        Long ganadorId = null;
        long maxVotos = 0;
        boolean empate = false;

        for (Map.Entry<Long, Long> entry : conteo.entrySet()) {
            if (entry.getValue() > maxVotos) {
                maxVotos = entry.getValue();
                ganadorId = entry.getKey();
                empate = false;
            } else if (entry.getValue() == maxVotos && ganadorId != null) {
                empate = true;
            }
        }

        if (empate) {
            final long maxVotosFinal = maxVotos;
            List<Long> empatados = conteo.entrySet().stream()
                    .filter(e -> e.getValue() == maxVotosFinal)
                    .map(Map.Entry::getKey)
                    .toList();

            int totalCandidatos = conteo.size();
            if (totalCandidatos == 2 || empatados.size() == 2) {
                ganadorId = empatados.get(new Random().nextInt(empatados.size()));
            } else {
                return reiniciarVotacion(votacion, empatados, nombres);
            }
        }

        if (ganadorId == null) {
            throw new BusinessException("No se pudo determinar un ganador");
        }

        // Limpiar delegados anteriores
        List<Postulacion> todasPostulaciones = postulacionRepository.findByProyectoId(proyecto.getId());
        for (Postulacion p : todasPostulaciones) {
            p.setEsDelegado(false);
            postulacionRepository.save(p);
        }

        Postulacion postulacionGanadora = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyecto.getId(), ganadorId)
                .orElseThrow(() -> new BusinessException("Error al encontrar al ganador"));
        postulacionGanadora.setEsDelegado(true);
        postulacionRepository.save(postulacionGanadora);

        votacion.setPostulacionGanadora(postulacionGanadora);
        votacion.setEstado(FaseVotacion.COMPLETADA);
        votacionRepository.save(votacion);

        proyecto.setFaseVotacion(FaseVotacion.COMPLETADA);
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
        proyectoRepository.save(proyecto);

        // Crear chats grupales
        try {
            chatGrupalService.crearChatsParaProyecto(proyecto.getId());
        } catch (Exception e) {
            log.error("Error al crear chats para proyecto " + proyecto.getId() + ": " + e.getMessage(), e);
        }

        // Notificaciones
        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyecto.getId(), EstadoPostulacion.CONFIRMADO);

        notificacionService.crearNotificacion(
                postulacionGanadora.getEstudiante().getUsuario(),
                "🎉 ¡Eres el delegado del equipo!",
                "Has sido elegido delegado para \"" + proyecto.getTitulo() + "\".",
                TipoNotificacion.PROYECTO,
                "/workspace/" + proyecto.getId()
        );

        for (Postulacion p : confirmados) {
            if (!p.getEstudiante().getId().equals(ganadorId)) {
                notificacionService.crearNotificacion(
                        p.getEstudiante().getUsuario(),
                        "✅ Delegado elegido",
                        postulacionGanadora.getEstudiante().getUsuario().getNombre()
                                + " es el delegado de \"" + proyecto.getTitulo() + "\".",
                        TipoNotificacion.PROYECTO,
                        "/workspace/" + proyecto.getId()
                );
            }
        }

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "✅ Delegado elegido",
                postulacionGanadora.getEstudiante().getUsuario().getNombre()
                        + " es el delegado de \"" + proyecto.getTitulo() + "\".",
                TipoNotificacion.PROYECTO,
                "/dashboard/mype/ejecucion"
        );

        return buildVotacionResponse(votacion, null);
    }

    // ═══════════════════════════════════════════════════════════
    // TAREA PROGRAMADA
    // ═══════════════════════════════════════════════════════════
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void finalizarVotacionesExpiradas() {
        List<VotacionDelegado> votacionesActivas = votacionRepository.findAll()
                .stream()
                .filter(v -> v.getEstado() == FaseVotacion.EN_VOTACION &&
                        LocalDateTime.now().isAfter(v.getFechaLimite()))
                .toList();

        for (VotacionDelegado votacion : votacionesActivas) {
            finalizarVotacion(votacion);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ═══════════════════════════════════════════════════════════

    private VotacionResponse asignarDelegadoAutomatico(Proyecto proyecto, List<Postulacion> confirmados) {
        if (confirmados.isEmpty()) {
            throw new BusinessException("No hay estudiantes confirmados");
        }

        Postulacion unico = confirmados.get(0);
        unico.setEsDelegado(true);
        postulacionRepository.save(unico);

        proyecto.setFaseVotacion(FaseVotacion.COMPLETADA);
        proyectoRepository.save(proyecto);

        VotacionDelegado votacion = VotacionDelegado.builder()
                .proyecto(proyecto)
                .fechaLimite(LocalDateTime.now())
                .estado(FaseVotacion.COMPLETADA)
                .postulacionGanadora(unico)
                .build();
        votacion = votacionRepository.save(votacion);

        notificacionService.crearNotificacion(
                unico.getEstudiante().getUsuario(),
                "🎉 Eres el delegado",
                "Eres el único estudiante en \"" + proyecto.getTitulo() +
                        "\", así que eres el delegado automáticamente.",
                TipoNotificacion.PROYECTO,
                "/workspace/" + proyecto.getId()
        );

        return buildVotacionResponse(votacion, null);
    }

    // ✅ NUEVO: Elegir delegado al azar cuando hay 2 estudiantes
    private VotacionResponse elegirDelegadoAlAzar(Proyecto proyecto, List<Postulacion> confirmados) {
        Postulacion ganador = confirmados.get(new Random().nextInt(confirmados.size()));
        Postulacion perdedor = confirmados.stream()
                .filter(p -> !p.getId().equals(ganador.getId()))
                .findFirst()
                .orElse(null);

        ganador.setEsDelegado(true);
        postulacionRepository.save(ganador);

        if (perdedor != null) {
            perdedor.setEsDelegado(false);
            postulacionRepository.save(perdedor);
        }

        VotacionDelegado votacion = VotacionDelegado.builder()
                .proyecto(proyecto)
                .fechaLimite(LocalDateTime.now())
                .estado(FaseVotacion.COMPLETADA)
                .postulacionGanadora(ganador)
                .build();
        votacion = votacionRepository.save(votacion);

        proyecto.setFaseVotacion(FaseVotacion.COMPLETADA);
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
        proyectoRepository.save(proyecto);

        try {
            chatGrupalService.crearChatsParaProyecto(proyecto.getId());
        } catch (Exception e) {
            log.error("Error al crear chats para proyecto " + proyecto.getId() + ": " + e.getMessage(), e);
        }

        notificacionService.crearNotificacion(
                ganador.getEstudiante().getUsuario(),
                "🎉 ¡Eres el delegado del equipo!",
                "El sistema te ha elegido como delegado para \"" + proyecto.getTitulo() +
                        "\". Solo tú puedes subir los entregables.",
                TipoNotificacion.PROYECTO,
                "/workspace/" + proyecto.getId()
        );

        if (perdedor != null) {
            notificacionService.crearNotificacion(
                    perdedor.getEstudiante().getUsuario(),
                    "✅ Delegado elegido",
                    ganador.getEstudiante().getUsuario().getNombre() +
                            " ha sido elegido delegado de \"" + proyecto.getTitulo() +
                            "\" por el sistema.",
                    TipoNotificacion.PROYECTO,
                    "/workspace/" + proyecto.getId()
            );
        }

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "✅ Delegado elegido",
                ganador.getEstudiante().getUsuario().getNombre() +
                        " es el delegado de \"" + proyecto.getTitulo() + "\".",
                TipoNotificacion.PROYECTO,
                "/dashboard/mype/ejecucion"
        );

        return buildVotacionResponse(votacion, null);
    }

    // ✅ Reiniciar votación cuando hay empate múltiple (3+)
    private VotacionResponse reiniciarVotacion(VotacionDelegado votacionActual,
                                               List<Long> empatados,
                                               Map<Long, String> nombres) {
        Proyecto proyecto = votacionActual.getProyecto();

        votacionActual.setEstado(FaseVotacion.COMPLETADA);
        votacionRepository.save(votacionActual);

        VotacionDelegado nuevaVotacion = VotacionDelegado.builder()
                .proyecto(proyecto)
                .fechaLimite(LocalDateTime.now().plusHours(HORAS_VOTACION))
                .estado(FaseVotacion.EN_VOTACION)
                .build();
        nuevaVotacion = votacionRepository.save(nuevaVotacion);

        proyecto.setFaseVotacion(FaseVotacion.EN_VOTACION);
        proyectoRepository.save(proyecto);

        String nombresEmpatados = empatados.stream()
                .map(id -> nombres.getOrDefault(id, "Desconocido"))
                .collect(Collectors.joining(" y "));

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyecto.getId(), EstadoPostulacion.CONFIRMADO)
                .stream()
                .filter(p -> empatados.contains(p.getEstudiante().getId()))
                .toList();

        for (Postulacion p : confirmados) {
            notificacionService.crearNotificacion(
                    p.getEstudiante().getUsuario(),
                    "🔄 Empate - Nueva votación",
                    "Hubo un empate entre " + nombresEmpatados +
                            ". Se ha iniciado una nueva ronda. Tienes " + HORAS_VOTACION + " horas.",
                    TipoNotificacion.PROYECTO,
                    "/workspace/" + proyecto.getId()
            );
        }

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "🔄 Empate en votación",
                "Los estudiantes empataron. Se ha iniciado una nueva ronda.",
                TipoNotificacion.PROYECTO,
                "/dashboard/mype/ejecucion"
        );

        return buildVotacionResponse(nuevaVotacion, null);
    }

    private VotacionResponse buildVotacionResponse(VotacionDelegado votacion, Long miEstudianteId) {
        Proyecto proyecto = votacion.getProyecto();

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyecto.getId(), EstadoPostulacion.CONFIRMADO);

        List<VotacionResponse.CandidatoDto> candidatos = new ArrayList<>();
        for (Postulacion p : confirmados) {
            long votosRecibidos = votoRepository.countVotosByCandidato(
                    votacion.getId(), p.getEstudiante().getId());

            boolean esGanador = votacion.getPostulacionGanadora() != null &&
                    votacion.getPostulacionGanadora().getId().equals(p.getId());

            boolean esYo = miEstudianteId != null &&
                    p.getEstudiante().getId().equals(miEstudianteId);

            candidatos.add(new VotacionResponse.CandidatoDto(
                    p.getEstudiante().getId(),
                    p.getEstudiante().getUsuario().getNombre(),
                    votosRecibidos,
                    esGanador,
                    esYo
            ));
        }

        boolean yaVote = false;
        if (miEstudianteId != null) {
            yaVote = votoRepository.existsByVotacionIdAndVotanteId(votacion.getId(), miEstudianteId);
        }

        int totalVotos = votoRepository.findByVotacionIdWithDetails(votacion.getId()).size();

        Long ganadorId = null;
        String ganadorNombre = null;
        if (votacion.getPostulacionGanadora() != null) {
            ganadorId = votacion.getPostulacionGanadora().getEstudiante().getId();
            ganadorNombre = votacion.getPostulacionGanadora().getEstudiante().getUsuario().getNombre();
        }

        return new VotacionResponse(
                votacion.getId(),
                proyecto.getId(),
                proyecto.getTitulo(),
                votacion.getEstado(),
                votacion.getFechaLimite(),
                votacion.getFechaCreacion(),
                ganadorId,
                ganadorNombre,
                candidatos,
                yaVote,
                totalVotos
        );
    }
}