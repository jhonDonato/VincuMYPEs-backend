package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
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
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.proyectos.domain.model.Postulacion;

@Slf4j
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
    private final ProyectoService proyectoService;
    private final EmailService emailService;

    @Transactional
    public EntregableResponse subir(Long proyectoId, String titulo, String descripcion, MultipartFile archivo,
                                    String emailEstudiante, Long entregableId) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        // Validar postulación
        var postulacion = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())
                .orElseThrow(() -> new BusinessException("No eres parte de este proyecto"));

        if (postulacion.getEstado() != EstadoPostulacion.CONFIRMADO) {
            throw new BusinessException("Solo estudiantes confirmados pueden subir entregables");
        }

        String archivoUrl = s3Service.subirEntregablePdf(archivo);

        Entregable entregable;
        boolean esNuevo = false;

        if (entregableId != null) {
            // ✅ ACTUALIZAR entregable existente
            entregable = entregableRepository.findById(entregableId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));

            // Verificar que el entregable pertenece al proyecto (y que el estudiante está en el proyecto)
            if (!entregable.getProyecto().getId().equals(proyectoId)) {
                throw new BusinessException("El entregable no pertenece a este proyecto", HttpStatus.FORBIDDEN);
            }
            // Opcional: verificar que el estudiante esté confirmado (ya se validó arriba)

            // Eliminar archivo anterior si existe
            if (entregable.getArchivo() != null && !entregable.getArchivo().isEmpty()) {
                try {
                    s3Service.eliminarArchivo(entregable.getArchivo());
                    log.debug("Archivo anterior eliminado de S3: {}", entregable.getArchivo());
                } catch (Exception e) {
                    log.warn("No se pudo eliminar archivo anterior de S3: {}", e.getMessage());
                }
            }

            // Actualizar campos
            entregable.setArchivo(archivoUrl);
            entregable.setDescripcion(descripcion);
            entregable.setFechaEntrega(LocalDateTime.now());
            entregable.setEstado(EstadoEntregable.PENDIENTE);
            entregable.setSubidoPor(usuario);
            entregable = entregableRepository.save(entregable);
            log.info("Entregable actualizado ID: {}", entregable.getId());
        } else {
            // ✅ CREAR NUEVO
            entregable = Entregable.builder()
                    .proyecto(proyecto)
                    .estudiante(estudiante)
                    .titulo(titulo)
                    .descripcion(descripcion)
                    .archivo(archivoUrl)
                    .subidoPor(usuario)
                    .build();
            entregable = entregableRepository.save(entregable);
            esNuevo = true;
            log.info("Nuevo entregable creado ID: {}", entregable.getId());
        }
        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "Nuevo entregable recibido",
                "El estudiante " + usuario.getNombre() + " subió un entregable para: " + proyecto.getTitulo(),
                TipoNotificacion.PROYECTO,
                "/dashboard/proyectos/" + proyectoId + "/entregables"
        );

        return toResponse(entregable);
    }

    // ✅ MÉTODO CORREGIDO: listarPorProyecto - Evita duplicados
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

        // 1. Obtener entregables sugeridos del proyecto
        List<String> entregablesSugeridos = obtenerEntregablesSugeridos(proyecto);

        // 2. Obtener entregables reales subidos por estudiantes
        List<Entregable> entregablesReales = entregableRepository.findByProyectoIdWithDetails(proyectoId);

        // 3. Crear un conjunto con los títulos de los entregables reales
        Set<String> titulosReales = entregablesReales.stream()
                .map(Entregable::getTitulo)
                .collect(Collectors.toSet());

        // 4. Construir lista: PRIMERO los reales, LUEGO los sugeridos que NO existen
        List<EntregableResponse> resultado = new ArrayList<>();

        // Agregar entregables reales (los que ya subió el estudiante)
        for (Entregable real : entregablesReales) {
            resultado.add(toResponse(real));
        }

        // Agregar SOLO los sugeridos que NO tienen un entregable real
        for (String tituloSugerido : entregablesSugeridos) {
            if (!titulosReales.contains(tituloSugerido)) {
                resultado.add(crearEntregableVirtual(proyecto, tituloSugerido));
            }
        }

        return resultado;
    }

    // ✅ MÉTODO 1: Obtener TODOS los entregables del estudiante (sin filtrar por proyecto)
    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        return entregableRepository.findByEstudianteIdWithDetails(estudiante.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ✅ MÉTODO 2: Obtener entregables del estudiante POR PROYECTO
    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(Long proyectoId, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        List<Entregable> entregables = entregableRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId());

        return entregables.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
        try {
            emailService.enviarCorreoNotificacion(
                    entregable.getEstudiante().getUsuario().getEmail(),
                    "Tu entregable fue revisado",
                    "Tu entregable \"" + entregable.getTitulo() + "\" del proyecto \""
                            + entregable.getProyecto().getTitulo() + "\" fue marcado como "
                            + request.estado().name()
                            + (request.observaciones() != null && !request.observaciones().isBlank()
                                ? ". Observaciones: " + request.observaciones() : "")
                            + ".\n\nInicia sesión para ver el detalle: http://localhost:5173/login",
                    entregable.getEstudiante().getUsuario().getNombre()
            );
        } catch (Exception e) {
            log.error("Error al enviar email al estudiante sobre revisión de entregable: {}", e.getMessage());
        }

        if (request.estado() == EstadoEntregable.APROBADO
                && proyecto.getEstado() != WorkflowEstado.COMPLETADO) {
            List<String> sugeridos = obtenerEntregablesSugeridos(proyecto);
            List<Entregable> todosEntregables = entregableRepository.findByProyectoId(proyectoId);

            boolean todosListos = false;
            if (!sugeridos.isEmpty()) {
                Set<String> titulosSubidos = todosEntregables.stream()
                        .map(Entregable::getTitulo)
                        .collect(Collectors.toSet());
                boolean todosSubidos = sugeridos.stream().allMatch(titulosSubidos::contains);
                boolean todosAprobados = todosEntregables.stream()
                        .allMatch(e -> e.getEstado() == EstadoEntregable.APROBADO);
                todosListos = todosSubidos && todosAprobados;
            }

            if (todosListos) {
                try {
                    proyectoService.completarProyecto(proyectoId, emailMype);
                    log.info("Proyecto {} completado automáticamente al aprobar el último entregable", proyectoId);
                } catch (Exception ex) {
                    log.error("Error al completar automáticamente el proyecto {}: {}", proyectoId, ex.getMessage());
                }
            }
        }

        return toResponse(saved);
    }

    private EntregableResponse toResponse(Entregable e) {
        String archivoNombre = null;
        if (e.getArchivo() != null && !e.getArchivo().isEmpty()) {
            String[] parts = e.getArchivo().split("/");
            archivoNombre = parts[parts.length - 1];
            archivoNombre = archivoNombre.replace("_", " ").replace("%20", " ");
        }
        // ✅ NUEVO: Obtener nombre de quién subió
        String subidoPorNombre = null;
        if (e.getSubidoPor() != null) {
            subidoPorNombre = e.getSubidoPor().getNombre();
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
                archivoNombre,
                // ✅ NUEVOS CAMPOS
                subidoPorNombre,
                null  // esDelegado se resuelve desde Postulacion
        );
    }

    @Transactional
    public void eliminar(Long proyectoId, Long entregableId, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        var entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable", entregableId));

        if (!entregable.getEstudiante().getId().equals(estudiante.getId())) {
            throw new BusinessException("No puedes eliminar este entregable", HttpStatus.FORBIDDEN);
        }

        if (entregable.getEstado() == EstadoEntregable.APROBADO) {
            throw new BusinessException("No puedes eliminar un entregable ya aprobado", HttpStatus.BAD_REQUEST);
        }

        if (entregable.getArchivo() != null && !entregable.getArchivo().isEmpty()) {
            try {
                s3Service.eliminarArchivo(entregable.getArchivo());
            } catch (Exception e) {
                System.err.println("Error deleting file from S3: " + e.getMessage());
            }
        }

        entregableRepository.delete(entregable);
    }

    // ============================================
    // ✅ MÉTODOS AUXILIARES
    // ============================================

    private List<String> obtenerEntregablesSugeridos(com.mypelink.backend.proyectos.domain.model.Proyecto proyecto) {
        String entregablesText = proyecto.getEntregablesSugeridos();
        if (entregablesText == null || entregablesText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] items = entregablesText.split("[•\\-\\*\\n]");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 3) {
                trimmed = trimmed.replaceAll("^\\d+[\\.\\-\\s]+", "");
                result.add(trimmed);
            }
        }

        if (result.isEmpty() && !entregablesText.trim().isEmpty()) {
            result.add(entregablesText.trim());
        }

        return result;
    }

    private EntregableResponse crearEntregableVirtual(
            com.mypelink.backend.proyectos.domain.model.Proyecto proyecto,
            String titulo) {

        return new EntregableResponse(
                null,                           // id
                proyecto.getId(),               // proyectoId
                proyecto.getTitulo(),          // proyectoTitulo
                null,                           // estudianteId
                null,                           // estudianteNombre
                titulo,                         // titulo
                null,                           // descripcion
                null,                           // archivo
                EstadoEntregable.PENDIENTE,     // estado
                null,                           // observaciones
                null,                           // fechaEntrega
                null,                           // archivoNombre
                null,                           // subidoPorNombre
                false                           // esDelegado
        );
    }

    // ✅ NUEVO MÉTODO: Solo entregables que YA SUBIERON los estudiantes
    @Transactional(readOnly = true)
    public List<EntregableResponse> listarSoloSubidos(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso", HttpStatus.FORBIDDEN);
        }

        return entregableRepository.findByProyectoIdWithDetails(proyectoId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
    // ✅ NUEVO: Listar TODOS los entregables (para cualquier miembro del proyecto)
    @Transactional(readOnly = true)
    public List<EntregableResponse> listarTodosDelProyecto(Long proyectoId, String emailUsuario) {
        var usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        // Verificar que el usuario pertenece al proyecto
        boolean esMype = false;
        boolean esEstudiante = false;

        if (usuario.getRol().getNombre().equals("ROLE_MYPE") || usuario.getRol().getNombre().equals("MYPE")) {
            var mype = mypeRepository.findByUsuarioId(usuario.getId());
            esMype = mype.isPresent() && proyecto.getMype().getId().equals(mype.get().getId());
        } else {
            var estudiante = estudianteRepository.findByUsuarioId(usuario.getId());
            if (estudiante.isPresent()) {
                esEstudiante = postulacionRepository
                        .findByProyectoIdAndEstudianteId(proyectoId, estudiante.get().getId())
                        .map(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                        .orElse(false);
            }
        }

        if (!esMype && !esEstudiante) {
            throw new BusinessException("No tienes permiso para ver estos entregables", HttpStatus.FORBIDDEN);
        }

        // Obtener entregables reales
        List<Entregable> entregablesReales = entregableRepository.findByProyectoIdWithDetails(proyectoId);

        // Obtener entregables sugeridos
        List<String> entregablesSugeridos = obtenerEntregablesSugeridos(proyecto);
        Set<String> titulosReales = entregablesReales.stream()
                .map(Entregable::getTitulo)
                .collect(Collectors.toSet());

        List<EntregableResponse> resultado = new ArrayList<>();

        for (Entregable real : entregablesReales) {
            resultado.add(toResponse(real));
        }

        for (String tituloSugerido : entregablesSugeridos) {
            if (!titulosReales.contains(tituloSugerido)) {
                resultado.add(crearEntregableVirtual(proyecto, tituloSugerido));
            }
        }

        return resultado;
    }
    @Transactional(readOnly = true)
    public List<EntregableResponse> actividadDelEstudiante(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        // Proyectos donde el estudiante está CONFIRMADO (sea delegado o no)
        var confirmadas = postulacionRepository
                .findByEstudianteIdAndEstado(estudiante.getId(), EstadoPostulacion.CONFIRMADO);

        List<EntregableResponse> resultado = new ArrayList<>();
        for (Postulacion post : confirmadas) {
            entregableRepository.findByProyectoIdWithDetails(post.getProyecto().getId())
                    .forEach(e -> resultado.add(toResponse(e)));
        }
        return resultado;
    }
    // ═══════════════════════════════════════════════════════════════
// BLOQUEO DE ENTREGABLES (edición exclusiva)
// ═══════════════════════════════════════════════════════════════

    @Transactional
    public void adquirirBloqueo(Long entregableId, Usuario usuario) {
        Entregable e = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));

        // Si ya está bloqueado por otro usuario y no ha expirado (1 hora)
        if (e.getLockedBy() != null && !e.getLockedBy().equals(usuario.getId())) {
            if (e.getLockedAt().plusHours(1).isAfter(LocalDateTime.now())) {
                Usuario bloqueador = usuarioRepository.findById(e.getLockedBy()).orElse(null);
                String nombre = bloqueador != null ? bloqueador.getNombre() : "Otro usuario";
                throw new BusinessException(
                        "Este entregable está siendo editado por " + nombre,
                        HttpStatus.CONFLICT
                );
            }
        }

        // Asignar bloqueo
        e.setLockedBy(usuario.getId());
        e.setLockedAt(LocalDateTime.now());
        entregableRepository.save(e);
    }

    @Transactional
    public void liberarBloqueo(Long entregableId, Usuario usuario) {
        Entregable e = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable no encontrado"));

        if (usuario.getId().equals(e.getLockedBy())) {
            e.setLockedBy(null);
            e.setLockedAt(null);
            entregableRepository.save(e);
        }
    }

    @Transactional
    public void liberarTodosDeUsuario(Long userId) {
        List<Entregable> bloqueados = entregableRepository.findByLockedBy(userId);
        for (Entregable e : bloqueados) {
            e.setLockedBy(null);
            e.setLockedAt(null);
        }
        entregableRepository.saveAll(bloqueados);
    }
}