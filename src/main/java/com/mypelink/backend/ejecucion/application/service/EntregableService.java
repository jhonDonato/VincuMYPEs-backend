package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.dto.RevisarEntregableRequest;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoEntregable;
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
    private final S3Service s3Service;

    @Transactional
    public EntregableResponse subir(Long proyectoId, String titulo, String descripcion, MultipartFile archivo,
                                    String emailEstudiante) {
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
                "El estudiante " + usuario.getNombre() + " subió un entregable para: "
                        + proyecto.getTitulo(),
                TipoNotificacion.PROYECTO,
                "/dashboard/proyectos/" + proyectoId + "/entregables");

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
            throw new BusinessException("No tienes permiso para ver estos entregables",
                    HttpStatus.FORBIDDEN);
        }

        return entregableRepository.findByProyectoIdWithDetails(proyectoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        return entregableRepository.findByEstudianteIdWithDetails(estudiante.getId())
                .stream().map(this::toResponse).toList();
    }

    // ✅ ESTE ES EL MÉTODO CORREGIDO - REEMPLÁZALO COMPLETAMENTE
    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(Long proyectoId, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        // Obtener entregables REALES del estudiante para este proyecto
        List<Entregable> entregablesReales = entregableRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId());

        // Convertir a response - SIN CREAR ENTREGABLES FALSOS
        return entregablesReales.stream()
                .map(this::toResponse)
                .toList();
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
            throw new BusinessException("No tienes permiso para revisar estos entregables",
                    HttpStatus.FORBIDDEN);
        }

        var entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable", entregableId));

        entregable.setEstado(request.estado());
        entregable.setObservaciones(request.observaciones());
        var saved = entregableRepository.save(entregable);

        notificacionService.crearNotificacion(
                entregable.getEstudiante().getUsuario(),
                "Tu entregable fue revisado",
                "Tu entregable \"" + entregable.getTitulo() + "\" fue marcado como "
                        + request.estado().name(),
                TipoNotificacion.PROYECTO,
                "/mis-entregables");

        return toResponse(saved);
    }

    private EntregableResponse toResponse(Entregable e) {
        // Extraer nombre del archivo de la URL
        String archivoNombre = null;
        if (e.getArchivo() != null && !e.getArchivo().isEmpty()) {
            String[] parts = e.getArchivo().split("/");
            archivoNombre = parts[parts.length - 1];
            // Decodificar caracteres especiales si es necesario
            archivoNombre = archivoNombre.replace("_", " ").replace("%20", " ");
        }

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
                e.getFechaEntrega(),
                archivoNombre);  // ← AGREGAR ESTE PARÁMETRO
    }

    @Transactional
    public void eliminar(Long proyectoId, Long entregableId, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        var entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable", entregableId));

        // Verificar que el entregable pertenezca al estudiante
        if (!entregable.getEstudiante().getId().equals(estudiante.getId())) {
            throw new BusinessException("No puedes eliminar este entregable", HttpStatus.FORBIDDEN);
        }

        // Verificar que el entregable esté en estado PENDIENTE o RECHAZADO
        if (entregable.getEstado() == EstadoEntregable.APROBADO) {
            throw new BusinessException("No puedes eliminar un entregable ya aprobado", HttpStatus.BAD_REQUEST);
        }

        // Opcional: Eliminar el archivo de S3
        if (entregable.getArchivo() != null && !entregable.getArchivo().isEmpty()) {
            try {
                s3Service.eliminarArchivo(entregable.getArchivo());
            } catch (Exception e) {
                System.err.println("Error deleting file from S3: " + e.getMessage());
            }
        }

        entregableRepository.delete(entregable);
    }
}