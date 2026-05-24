package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.dto.RevisarEntregableRequest;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.mypelink.backend.shared.domain.enums.EstadoEntregable;
@Service
@RequiredArgsConstructor
public class EntregableService {

    private final EntregableRepository entregableRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final S3Service s3Service; // ✨ AÑADIDO

    @Transactional
    public EntregableResponse subir(Long proyectoId, String titulo, String descripcion, MultipartFile archivo, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        boolean postulacionAceptada = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())
                .map(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                .orElse(false);

        if (!postulacionAceptada) {
            throw new BusinessException("Solo estudiantes aceptados pueden subir entregables");
        }

        String archivoUrl = s3Service.subirEntregablePdf(archivo);

        var entregable = entregableRepository.save(Entregable.builder()
                .proyecto(proyecto)
                .estudiante(estudiante)
                .titulo(titulo)
                .descripcion(descripcion)
                .archivo(archivoUrl)
                .build());

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "Nuevo entregable recibido",
                "El estudiante " + usuario.getNombre() + " subió un entregable para: " + proyecto.getTitulo(),
                TipoNotificacion.PROYECTO,
                "/dashboard/proyectos/" + proyectoId + "/entregables"
        );

        return toResponse(entregable);
    }

    @Transactional(readOnly = true)
    public List<EntregableResponse> listarPorProyecto(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para ver estos entregables", HttpStatus.FORBIDDEN);
        }

        return entregableRepository.findByProyectoIdWithDetails(proyectoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(Long proyectoId, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        // Obtener entregables reales del estudiante para este proyecto
        List<Entregable> entregablesReales = entregableRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId());

        // Convertir a response
        List<EntregableResponse> responses = new java.util.ArrayList<>();
        for (Entregable e : entregablesReales) {
            responses.add(toResponse(e));
        }

        // Si no hay entregables reales, crear desde los sugeridos del proyecto
        if (responses.isEmpty() && proyecto.getEntregablesSugeridos() != null
                && !proyecto.getEntregablesSugeridos().isBlank()) {

            String[] sugeridos = proyecto.getEntregablesSugeridos().split(",");
            for (String titulo : sugeridos) {
                String tituloLimpio = titulo.trim();
                if (!tituloLimpio.isEmpty()) {
                    // ✅ Truncar título si es muy largo
                    String tituloTruncado = tituloLimpio.length() > 200
                            ? tituloLimpio.substring(0, 197) + "..."
                            : tituloLimpio;

                    responses.add(new EntregableResponse(
                            null,
                            proyecto.getId(),
                            proyecto.getTitulo(),
                            estudiante.getId(),
                            estudiante.getUsuario().getNombre(),
                            tituloTruncado,  // ✅ Usar título truncado
                            "Entregable sugerido por la MYPE",  // ✅ Descripción corta
                            null,
                            EstadoEntregable.PENDIENTE,
                            null,
                            null
                    ));
                }
            }
        }

        return responses;
    }

    @Transactional
    public EntregableResponse revisar(Long proyectoId, Long entregableId,
                                      RevisarEntregableRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para revisar estos entregables", HttpStatus.FORBIDDEN);
        }

        var entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable", entregableId));

        entregable.setEstado(request.estado());
        entregable.setObservaciones(request.observaciones());
        var saved = entregableRepository.save(entregable);

        notificacionService.crearNotificacion(
                entregable.getEstudiante().getUsuario(),
                "Tu entregable fue revisado",
                "Tu entregable \"" + entregable.getTitulo() + "\" fue marcado como " + request.estado().name(),
                TipoNotificacion.PROYECTO,
                "/mis-entregables"
        );

        return toResponse(saved);
    }

    private EntregableResponse toResponse(Entregable e) {
        return new EntregableResponse(
                e.getId(),
                e.getProyecto().getId(),
                e.getProyecto().getTitulo(),
                e.getEstudiante().getId(),
                e.getEstudiante().getUsuario().getNombre(),
                e.getTitulo(),
                e.getDescripcion(),
                e.getArchivo(),
                e.getEstado(),
                e.getObservaciones(),
                e.getFechaEntrega()
        );
    }
}