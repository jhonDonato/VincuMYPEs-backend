package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.ActualizarMypeRequest;
import com.mypelink.backend.usuarios.application.dto.MypePerfilResponse;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.model.EntregableTipo;

import com.mypelink.backend.usuarios.domain.model.Estudiante;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final MypeRepository mypeRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final WorkflowHistorialRepository workflowHistorialRepository;
    private final TipoProyectoRepository tipoProyectoRepository;
    private final EntregableTipoRepository entregableTipoRepository;
    private final InsumoTipoRepository insumoTipoRepository;
    private final InsumoProyectoRepository insumoProyectoRepository;
    private final S3Service s3Service;

    private static final int HORAS_PLAZO = 12;

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS EXISTENTES (sin cambios respecto al original)
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ProyectoResponse> listarPorMype(String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        return proyectoRepository.findByMypeIdConMype(mype.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProyectoResponse> listarPublicos(Pageable pageable) {
        List<ProyectoResponse> lista = proyectoRepository
                .findPublicosConMype(WorkflowEstado.PENDIENTE)
                .stream().map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), lista.size());
        return new org.springframework.data.domain.PageImpl<>(
                lista.subList(start, end), pageable, lista.size());
    }

    public ProyectoResponse obtenerPorId(Long id) {
        return proyectoRepository.findByIdWithMype(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", id));
    }

    @Transactional
    public ProyectoResponse crear(CrearProyectoRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado para este usuario"));
        var proyecto = Proyecto.builder()
                .mype(mype).titulo(request.titulo()).descripcion(request.descripcion())
                .objetivo(request.objetivo()).requisitos(request.requisitos())
                .entregablesSugeridos(request.entregablesSugeridos())
                .areaSistemas(request.areaSistemas())
                .cupos(request.cupos() != null ? request.cupos() : 1)
                .delegarGestionAdmin(false)
                .fechaInicio(request.fechaInicio()).fechaLimite(request.fechaLimite())
                .estado(WorkflowEstado.BORRADOR).build();
        // Si viene tipoProyectoId, asociarlo y generar entregables desde EntregableTipo
        if (request.tipoProyectoId() != null) {
            TipoProyecto tipo = tipoProyectoRepository.findById(request.tipoProyectoId())
                    .orElseThrow(() -> new ResourceNotFoundException("TipoProyecto", request.tipoProyectoId()));
            proyecto.setTipoProyecto(tipo);
            // Generar entregablesSugeridos desde EntregableTipo si no se enviaron
            if (request.entregablesSugeridos() == null || request.entregablesSugeridos().isBlank()) {
                List<EntregableTipo> entregablesTipo = entregableTipoRepository
                        .findByTipoProyectoIdOrderByOrdenAsc(tipo.getId());
                String viñetas = entregablesTipo.stream()
                        .map(e -> "• " + e.getTitulo())
                        .collect(Collectors.joining("\n"));
                proyecto.setEntregablesSugeridos(viñetas);
            }
        }
        if (request.diasEstimados() != null) {
            proyecto.setDiasEstimados(request.diasEstimados());
        }
        return toResponse(proyectoRepository.save(proyecto));
    }

    @Transactional
    public PostulacionResponse postular(Long proyectoId, PostulacionRequest request, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (proyecto.getEstado() != WorkflowEstado.PENDIENTE) {
            throw new BusinessException("El proyecto no está disponible para postulaciones");
        }
        if (postulacionRepository.existsByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())) {
            throw new BusinessException("Ya postulaste a este proyecto");
        }
        long proyectosActivos = contarProyectosActivosDelEstudiante(estudiante.getId());
        int maxProyectos = estudiante.getLimiteProyectos();
        if (proyectosActivos >= maxProyectos) {
            throw new BusinessException(
                    "Alcanzaste tu límite de proyectos activos. Termina alguno antes de postular a otro.",
                    HttpStatus.CONFLICT
            );
        }

        var postulacion = postulacionRepository.save(Postulacion.builder()
                .proyecto(proyecto).estudiante(estudiante)
                .mensajePostulacion(request.mensajePostulacion())
                .archivoAdjunto(request.archivoAdjunto())
                .build());

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "Nueva postulación recibida",
                "El estudiante " + estudiante.getUsuario().getNombre()
                        + " postuló a tu proyecto: " + proyecto.getTitulo(),
                TipoNotificacion.POSTULACION,
                "/dashboard/postulaciones/" + proyecto.getId()
        );
        return toPostulacionResponse(postulacion);
    }

    @Transactional
    public ProyectoResponse publicar(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para publicar este proyecto", HttpStatus.FORBIDDEN);
        }
        if (proyecto.getEstado() != WorkflowEstado.BORRADOR) {
            throw new BusinessException("Solo los proyectos en BORRADOR pueden publicarse");
        }
        if (proyecto.getTipoProyecto() != null) {
            List<InsumoTipo> obligatorios = insumoTipoRepository
                    .findByTipoProyectoIdAndObligatorioTrue(proyecto.getTipoProyecto().getId());
            for (InsumoTipo obligatorio : obligatorios) {
                long count = insumoProyectoRepository.countByProyectoIdAndInsumoTipoId(proyectoId, obligatorio.getId());
                if (count == 0) {
                    throw new BusinessException(
                            "Falta el insumo obligatorio: " + obligatorio.getNombre() + ". Debes subirlo antes de publicar.");
                }
            }
        }
        // Hard block: una MYPE no puede tener dos proyectos del MISMO TipoProyecto
        // simultáneamente en estado activo. Razones documentadas:
        // - Evita que la MYPE evalúe dos equipos haciendo lo mismo en paralelo.
        // - Evita que los estudiantes vean publicaciones repetidas y se desmotiven.
        // Para conseguir más estudiantes, la vía correcta es esperar a que el primero
        // termine, no abrir otro del mismo tipo ni editar cupos del existente.
        if (proyecto.getTipoProyecto() != null) {
            List<WorkflowEstado> estadosActivos = List.of(
                    WorkflowEstado.PENDIENTE,
                    WorkflowEstado.EN_DESARROLLO,
                    WorkflowEstado.EN_REVISION
            );
            List<Proyecto> proyectosActivosMismoTipo = proyectoRepository
                    .findByMypeIdAndTipoProyectoIdAndEstadoIn(
                            proyecto.getMype().getId(),
                            proyecto.getTipoProyecto().getId(),
                            estadosActivos
                    );
            if (!proyectosActivosMismoTipo.isEmpty()) {
                Proyecto existente = proyectosActivosMismoTipo.get(0);
                throw new BusinessException(
                        "Ya tienes un proyecto activo de este tipo: \"" + existente.getTitulo() + "\". " +
                                "Debes esperar a que termine antes de publicar otro del mismo tipo.",
                        HttpStatus.CONFLICT
                );
            }
        }
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        Proyecto guardado = proyectoRepository.save(proyecto);

        // Notificar a TODOS los estudiantes
        List<Estudiante> estudiantes = estudianteRepository.findAll();
        for (Estudiante estudiante : estudiantes) {
            notificacionService.crearNotificacion(
                    estudiante.getUsuario(),
                    "🔔 Nuevo proyecto disponible",
                    "La empresa \"" + mype.getNombreComercial() + "\" publicó: " + proyecto.getTitulo(),
                    TipoNotificacion.PROYECTO,
                    "/proyectos?selected=" + proyecto.getId()
            );
        }

        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> listarPostulacionesAceptadas(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para ver este proyecto", HttpStatus.FORBIDDEN);
        }
        return postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO)
                .stream().map(this::toPostulacionResponse).toList();
    }

    public List<PostulacionResponse> listarPostulaciones(Long proyectoId, String emailGestor) {
        var usuario = usuarioRepository.findByEmailWithRole(emailGestor)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        validarPermisoGestionPostulaciones(usuario, proyecto);
        return postulacionRepository.findByProyectoIdWithDetails(proyectoId)
                .stream().map(this::toPostulacionResponse).toList();
    }

    public List<PostulacionResponse> misPostulaciones(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        return postulacionRepository.findByEstudianteIdWithDetails(estudiante.getId())
                .stream().map(this::toPostulacionResponse).toList();
    }

    @Transactional
    public ProyectoResponse cerrarProyecto(Long proyectoId, String emailGestor) {
        var usuario = usuarioRepository.findByEmailWithRole(emailGestor)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        boolean esAdmin = usuario.getRol().getNombre().equals("ROLE_ADMIN");
        boolean esDuenoMype = false;
        if (!esAdmin) {
            var mypeOpcional = mypeRepository.findByUsuarioId(usuario.getId());
            if (mypeOpcional.isPresent()) {
                esDuenoMype = proyecto.getMype().getId().equals(mypeOpcional.get().getId());
            }
        }
        if (!esAdmin && !esDuenoMype) {
            throw new BusinessException("No tienes permiso para cerrar este proyecto", HttpStatus.FORBIDDEN);
        }
        proyecto.setActivo(false);
        return toResponse(proyectoRepository.save(proyecto));
    }

    /**
     * Edita un proyecto. Reglas de qué se puede editar según el estado:
     *
     *   BORRADOR                       → todos los campos editables.
     *   PENDIENTE                      → editable salvo `cupos`. La cantidad
     *                                    de estudiantes ya está anunciada al
     *                                    público, cambiarla rompe expectativas
     *                                    de quienes ya postularon o evaluaron
     *                                    si postular.
     *   EN_REVISION                    → mismo bloqueo de `cupos` que PENDIENTE.
     *   EN_DESARROLLO / COMPLETADO     → nada editable (regla previa).
     *
     * Si la MYPE necesita más estudiantes para algo parecido, la vía es esperar
     * a que el proyecto termine y publicar otro nuevo; NO editar cupos.
     */
    @Transactional
    public ProyectoResponse editar(Long proyectoId, EditarProyectoRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para editar este proyecto", HttpStatus.FORBIDDEN);
        }
        if (proyecto.getEstado() == WorkflowEstado.EN_DESARROLLO ||
                proyecto.getEstado() == WorkflowEstado.COMPLETADO) {
            throw new BusinessException("No puedes editar un proyecto que ya está en desarrollo o completado");
        }
        // Bloqueo: díasEstimados solo editable en BORRADOR
        boolean intentaCambiarDias = request.getDiasEstimados() != null
                && !request.getDiasEstimados().equals(proyecto.getDiasEstimados());
        if (intentaCambiarDias && proyecto.getEstado() != WorkflowEstado.BORRADOR) {
            throw new BusinessException(
                    "Los días de duración no se pueden modificar después de publicar.",
                    HttpStatus.CONFLICT
            );
        }

        // Bloqueo nuevo: cupos solo editables en BORRADOR.
        // Si el proyecto ya está PENDIENTE o EN_REVISION y la MYPE intenta cambiar
        // los cupos, rechazamos el cambio con mensaje claro.
        boolean intentaCambiarCupos = request.getCupos() != null
                && !request.getCupos().equals(proyecto.getCupos());
        if (intentaCambiarCupos && proyecto.getEstado() != WorkflowEstado.BORRADOR) {
            throw new BusinessException(
                    "Los cupos no se pueden modificar después de publicar el proyecto. " +
                            "Si necesitas más estudiantes, espera a que este proyecto termine y publica uno nuevo.",
                    HttpStatus.CONFLICT
            );
        }

        proyecto.setTitulo(request.getTitulo());
        proyecto.setDescripcion(request.getDescripcion());
        proyecto.setObjetivo(request.getObjetivo());
        proyecto.setRequisitos(request.getRequisitos());
        proyecto.setEntregablesSugeridos(request.getEntregablesSugeridos());
        proyecto.setAreaSistemas(request.getAreaSistemas());
        // Cupos: solo se aplica si estado == BORRADOR (la validación de arriba ya filtró
        // los casos en que se intenta cambiar fuera de BORRADOR).
        if (proyecto.getEstado() == WorkflowEstado.BORRADOR) {
            proyecto.setCupos(request.getCupos());
        }
        if (request.getDiasEstimados() != null) {
            proyecto.setDiasEstimados(request.getDiasEstimados());
        }
        proyecto.setFechaInicio(request.getFechaInicio());
        proyecto.setFechaLimite(request.getFechaLimite());
        return toResponse(proyectoRepository.save(proyecto));
    }

    @Transactional
    public void eliminar(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para eliminar este proyecto", HttpStatus.FORBIDDEN);
        }
        if (proyecto.getEstado() == WorkflowEstado.EN_DESARROLLO) {
            throw new BusinessException("No puedes eliminar un proyecto que ya tiene estudiantes asignados");
        }
        proyectoRepository.delete(proyecto);
    }

    // ══════════════════════════════════════════════════════════════
    // FLUJO TRILATERAL (sin cambios)
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public PostulacionResponse cambiarEstadoPostulacion(
            Long proyectoId, Long postulacionId,
            CambiarEstadoPostulacionRequest request, String emailGestor) {

        var usuario = usuarioRepository.findByEmailWithRole(emailGestor)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        validarPermisoGestionPostulaciones(usuario, proyecto);

        var postulacion = postulacionRepository.findById(postulacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Postulacion", postulacionId));

        boolean esAdmin = usuario.getRol().getNombre().equals("ROLE_ADMIN");
        EstadoPostulacion estadoActual = postulacion.getEstado();
        EstadoPostulacion nuevoEstado = request.estado();

        if (esAdmin && nuevoEstado == EstadoPostulacion.PRESELECCIONADO) {
            long activos = contarProyectosActivosDelEstudiante(postulacion.getEstudiante().getId());
            int limite = postulacion.getEstudiante().getLimiteProyectos();
            if (activos >= limite) {
                throw new BusinessException(
                        "Este estudiante ya alcanzó su límite de proyectos activos. " +
                                "No puede ser preseleccionado.",
                        HttpStatus.CONFLICT
                );
            }
            if (estadoActual != EstadoPostulacion.PENDIENTE) {
                throw new BusinessException("Solo se puede preseleccionar postulaciones en estado PENDIENTE");
            }

            long yaConfirmados = postulacionRepository.findByProyectoId(proyectoId)
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                    .count();

            long yaEnProceso = postulacionRepository.findByProyectoId(proyectoId)
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPostulacion.PRESELECCIONADO
                            || p.getEstado() == EstadoPostulacion.VALIDADO_MYPE)
                    .count();

            if (yaConfirmados + yaEnProceso >= proyecto.getCupos()) {
                throw new BusinessException(
                        "No puedes preseleccionar más estudiantes. Ya hay "
                                + proyecto.getCupos() + " cupo(s) cubiertos o en proceso de confirmación."
                );
            }

            postulacion.setEstado(EstadoPostulacion.PRESELECCIONADO);
            postulacion.setFechaLimiteConfirmacion(LocalDateTime.now().plusHours(HORAS_PLAZO));
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    proyecto.getMype().getUsuario(),
                    "El admin seleccionó un estudiante para tu proyecto",
                    "El administrador preseleccionó a " + postulacion.getEstudiante().getUsuario().getNombre()
                            + " para \"" + proyecto.getTitulo() + "\". Tienes " + HORAS_PLAZO
                            + "h para validar o rechazar esta selección.",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + proyecto.getId()
            );
            return toPostulacionResponse(postulacion);
        }

        if (esAdmin && nuevoEstado == EstadoPostulacion.RECHAZADO) {
            if (estadoActual != EstadoPostulacion.PENDIENTE) {
                throw new BusinessException("Solo se pueden rechazar postulaciones en estado PENDIENTE");
            }
            postulacion.setEstado(EstadoPostulacion.RECHAZADO);
            postulacion.setFechaLimiteConfirmacion(null);
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    postulacion.getEstudiante().getUsuario(),
                    "Tu postulación fue revisada",
                    "Tu postulación al proyecto \"" + proyecto.getTitulo() + "\" no fue seleccionada en esta ocasión.",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
            return toPostulacionResponse(postulacion);
        }

        if (!esAdmin && nuevoEstado == EstadoPostulacion.VALIDADO_MYPE) {
            System.out.println("=== DEBUG VALIDACION MYPE ===");
            System.out.println("Postulacion ID: " + postulacion.getId());
            System.out.println("Estudiante ID: " + postulacion.getEstudiante().getId());
            long activos = contarProyectosActivosDelEstudiante(postulacion.getEstudiante().getId());
            int limite = postulacion.getEstudiante().getLimiteProyectos();
            System.out.println("Activos contados: " + activos);
            System.out.println("Límite del estudiante: " + limite);
            if (activos >= limite) {
                System.out.println("VALIDACION FALLIDA: se lanzará 409");
                throw new BusinessException(
                        "Este estudiante ya alcanzó su límite de proyectos activos. " +
                                "No puedes validarlo hasta que termine alguno de los actuales.",
                        HttpStatus.CONFLICT
                );
            }
            else {
                System.out.println("VALIDACION OK: activos < limite, se permite validar");
            }
            postulacion.setEstado(EstadoPostulacion.VALIDADO_MYPE);
            postulacion.setFechaLimiteConfirmacion(LocalDateTime.now().plusHours(HORAS_PLAZO));
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    postulacion.getEstudiante().getUsuario(),
                    "¡Fuiste aceptado en un proyecto!",
                    "La empresa \"" + proyecto.getMype().getNombreComercial()
                            + "\" aceptó tu postulación al proyecto \""
                            + proyecto.getTitulo() + "\". Tienes " + HORAS_PLAZO
                            + "h para confirmar o rechazar.",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
            return toPostulacionResponse(postulacion);
        }

        if (!esAdmin && nuevoEstado == EstadoPostulacion.RECHAZADO) {
            if (estadoActual != EstadoPostulacion.PRESELECCIONADO) {
                throw new BusinessException("Solo se puede rechazar una postulación PRESELECCIONADA desde la MYPE");
            }
            postulacion.setEstado(EstadoPostulacion.RECHAZADO);
            postulacion.setFechaLimiteConfirmacion(null);
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    usuario,
                    "La MYPE rechazó tu selección",
                    "La MYPE \"" + proyecto.getMype().getNombreComercial()
                            + "\" rechazó al estudiante preseleccionado para \""
                            + proyecto.getTitulo() + "\". Por favor selecciona otro postulante.",
                    TipoNotificacion.POSTULACION,
                    "/admin/proyectos/" + proyecto.getId() + "/postulaciones"
            );
            return toPostulacionResponse(postulacion);
        }

        throw new BusinessException("Transición de estado no permitida: "
                + estadoActual + " → " + nuevoEstado);
    }

    @Transactional
    public PostulacionResponse confirmarPostulacion(Long postulacionId, boolean confirmar, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var postulacion = postulacionRepository.findById(postulacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Postulacion", postulacionId));

        if (!postulacion.getEstudiante().getId().equals(estudiante.getId())) {
            throw new BusinessException("No tienes permiso para responder esta postulación", HttpStatus.FORBIDDEN);
        }
        if (postulacion.getEstado() != EstadoPostulacion.VALIDADO_MYPE) {
            throw new BusinessException("Solo puedes confirmar postulaciones en estado VALIDADO_MYPE");
        }

        var proyecto = postulacion.getProyecto();

        long yaConfirmados = postulacionRepository.findByProyectoId(proyecto.getId())
                .stream()
                .filter(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                .count();

        if (yaConfirmados >= proyecto.getCupos()) {
            postulacion.setEstado(EstadoPostulacion.EXPIRADO);
            postulacion.setFechaLimiteConfirmacion(null);
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    postulacion.getEstudiante().getUsuario(),
                    "Los cupos ya fueron cubiertos",
                    "Lo sentimos, otro estudiante confirmó antes que tú y los cupos del proyecto \""
                            + proyecto.getTitulo() + "\" se llenaron.",
                    TipoNotificacion.POSTULACION,
                    "/mis-postulaciones"
            );
            throw new BusinessException(
                    "Lo sentimos, los cupos de este proyecto ya fueron cubiertos por otros estudiantes."
            );
        }

        if (confirmar) {
            postulacion.setEstado(EstadoPostulacion.CONFIRMADO);
            postulacion.setFechaLimiteConfirmacion(null);
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);
            // Autocancelar otras postulaciones del estudiante en estado VALIDADO_MYPE
            // (excluyendo el proyecto actual)
            long activosTrasConfirmar = contarProyectosActivosDelEstudiante(estudiante.getId());
            int limite = estudiante.getLimiteProyectos();
            if (activosTrasConfirmar >= limite) {
                List<Postulacion> otrasValidadas = postulacionRepository
                        .findByEstudianteIdAndEstadoInExcluyendoProyecto(
                                estudiante.getId(),
                                List.of(EstadoPostulacion.VALIDADO_MYPE),
                                proyecto.getId()
                        );
                for (Postulacion otra : otrasValidadas) {
                    otra.setEstado(EstadoPostulacion.RECHAZADO);
                    otra.setFechaLimiteConfirmacion(null);
                    otra.setFechaRespuesta(LocalDateTime.now());
                    postulacionRepository.save(otra);

                    // Notificar a la otra MYPE
                    notificacionService.crearNotificacion(
                            otra.getProyecto().getMype().getUsuario(),
                            "El estudiante eligió otro proyecto",
                            estudiante.getUsuario().getNombre() + " confirmó participación en otro proyecto. " +
                                    "Por favor selecciona otro postulante para \"" +
                                    otra.getProyecto().getTitulo() + "\".",
                            TipoNotificacion.POSTULACION,
                            "/dashboard/postulaciones/" + otra.getProyecto().getId()
                    );
                }
            }

            long confirmadosActualizados = postulacionRepository.findByProyectoId(proyecto.getId())
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                    .count();

            if (confirmadosActualizados >= proyecto.getCupos()) {
                WorkflowEstado estadoAnterior = proyecto.getEstado();
                proyecto.setFechaInicioReal(LocalDateTime.now());
                proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
                proyectoRepository.save(proyecto);

                workflowHistorialRepository.save(WorkflowHistorial.builder()
                        .proyecto(proyecto).cambiadoPor(usuario)
                        .estadoAnterior(estadoAnterior).estadoNuevo(WorkflowEstado.EN_DESARROLLO)
                        .comentario("Cupos cubiertos. Estudiante " + usuario.getNombre() + " confirmó.")
                        .build());

                List<EstadoPostulacion> estadosActivos = List.of(
                        EstadoPostulacion.PENDIENTE,
                        EstadoPostulacion.PRESELECCIONADO,
                        EstadoPostulacion.VALIDADO_MYPE
                );

                postulacionRepository.findByProyectoId(proyecto.getId()).stream()
                        .filter(p -> estadosActivos.contains(p.getEstado()))
                        .forEach(p -> {
                            p.setEstado(EstadoPostulacion.RECHAZADO);
                            p.setFechaLimiteConfirmacion(null);
                            p.setFechaRespuesta(LocalDateTime.now());
                            postulacionRepository.save(p);

                            notificacionService.crearNotificacion(
                                    p.getEstudiante().getUsuario(),
                                    "Los cupos del proyecto se llenaron",
                                    "Lo sentimos, los cupos del proyecto \"" + proyecto.getTitulo()
                                            + "\" ya fueron cubiertos. ¡Sigue postulando a otros proyectos!",
                                    TipoNotificacion.POSTULACION,
                                    "/mis-postulaciones"
                            );
                        });
            }

            notificacionService.crearNotificacion(
                    proyecto.getMype().getUsuario(),
                    "¡Estudiante confirmado!",
                    usuario.getNombre() + " confirmó su participación en \""
                            + proyecto.getTitulo() + "\".",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + proyecto.getId()
            );

            notificacionService.crearNotificacion(
                    usuario,
                    "¡Proyecto confirmado!",
                    "Has confirmado tu participación en \"" + proyecto.getTitulo()
                            + "\". Ya puedes acceder al workspace para subir tus entregables.",
                    TipoNotificacion.POSTULACION,
                    "/workspace/" + proyecto.getId()
            );

        } else {
            postulacion.setEstado(EstadoPostulacion.RECHAZADO);
            postulacion.setFechaLimiteConfirmacion(null);
            postulacion.setFechaRespuesta(LocalDateTime.now());
            postulacionRepository.save(postulacion);

            notificacionService.crearNotificacion(
                    proyecto.getMype().getUsuario(),
                    "El estudiante rechazó la oferta",
                    usuario.getNombre() + " rechazó la oferta para \""
                            + proyecto.getTitulo() + "\". El administrador deberá seleccionar otro postulante.",
                    TipoNotificacion.POSTULACION,
                    "/dashboard/postulaciones/" + proyecto.getId()
            );
        }

        return toPostulacionResponse(postulacion);
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS ADMIN (sin cambios)
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ProyectoAdminResponse> listarParaAdmin(String emailAdmin) {
        validarRolAdmin(emailAdmin);
        return proyectoRepository.findAllConMype().stream().map(p -> {
            long confirmados = postulacionRepository.findByProyectoId(p.getId())
                    .stream()
                    .filter(post -> post.getEstado() == EstadoPostulacion.CONFIRMADO || post.getEstado() == EstadoPostulacion.ACEPTADO)
                    .count();
            long pendientes = postulacionRepository.findByProyectoId(p.getId())
                    .stream()
                    .filter(post -> post.getEstado() == EstadoPostulacion.PENDIENTE)
                    .count();
            return new ProyectoAdminResponse(
                    p.getId(), p.getTitulo(), p.getAreaSistemas(), p.getEstado(),
                    p.getCupos(), confirmados, p.getFechaCreacion(),
                    p.getMype() != null ? p.getMype().getNombreComercial() : "Sin MYPE",
                    p.getMype() != null ? p.getMype().getId() : null,
                    pendientes,
                    p.getDelegarGestionAdmin()
            );
        }).toList();
    }

    @Transactional
    public void cederGestion(Long proyectoId, String emailAdmin) {
        var admin = validarRolAdmin(emailAdmin);
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        proyecto.setDelegarGestionAdmin(true);
        proyectoRepository.save(proyecto);
        workflowHistorialRepository.save(WorkflowHistorial.builder()
                .proyecto(proyecto).cambiadoPor(admin)
                .estadoAnterior(proyecto.getEstado()).estadoNuevo(proyecto.getEstado())
                .comentario("El administrador cedió la gestión de postulantes a la MYPE.")
                .build());
    }

    @Transactional
    public void auditarAbandono(Long proyectoId, Long postulacionId, String emailAdmin) {
        var admin = validarRolAdmin(emailAdmin);
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        var postulacion = postulacionRepository.findById(postulacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Postulacion", postulacionId));
        WorkflowEstado estadoAnterior = proyecto.getEstado();
        postulacion.setEstado(EstadoPostulacion.RECHAZADO);
        postulacionRepository.save(postulacion);
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        proyectoRepository.save(proyecto);
        workflowHistorialRepository.save(WorkflowHistorial.builder()
                .proyecto(proyecto).cambiadoPor(admin)
                .estadoAnterior(estadoAnterior).estadoNuevo(WorkflowEstado.PENDIENTE)
                .comentario("Abandono reportado. Estudiante expulsado y proyecto reabierto.")
                .build());
        postulacionRepository.findByProyectoId(proyectoId).forEach(p -> {
            if (!p.getId().equals(postulacionId)) {
                notificacionService.crearNotificacion(
                        p.getEstudiante().getUsuario(),
                        "¡Cupo liberado!",
                        "Se ha liberado un cupo de emergencia para: " + proyecto.getTitulo() + ". ¡Vuelve a postular!",
                        TipoNotificacion.PROYECTO,
                        "/proyectos/" + proyecto.getId()
                );
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════

    private void validarPermisoGestionPostulaciones(Usuario usuarioLogueado, Proyecto proyecto) {
        boolean esAdmin = usuarioLogueado.getRol().getNombre().equals("ROLE_ADMIN");
        boolean esDuenoMype = false;
        if (!esAdmin) {
            var mypeOpcional = mypeRepository.findByUsuarioId(usuarioLogueado.getId());
            if (mypeOpcional.isPresent()) {
                esDuenoMype = proyecto.getMype().getId().equals(mypeOpcional.get().getId());
            }
        }
        if (!esAdmin && !esDuenoMype) {
            throw new BusinessException(
                    "No tienes permiso para gestionar las postulaciones de este proyecto",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private Usuario validarRolAdmin(String email) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (!usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("Acceso denegado: Se requiere rol Administrador", HttpStatus.FORBIDDEN);
        }
        return usuario;
    }

    // ══════════════════════════════════════════════════════════════
    // MYPEs (sin cambios)
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public MypePerfilResponse obtenerPerfilMype(Long mypeId, String emailSolicitante) {
        var mype = mypeRepository.findById(mypeId)
                .orElseThrow(() -> new ResourceNotFoundException("MYPE", mypeId));

        String nivelAcceso = "PUBLICO";

        if (emailSolicitante != null) {
            var usuarioSolicitante = usuarioRepository.findByEmailWithRole(emailSolicitante).orElse(null);

            if (usuarioSolicitante != null) {
                String rol = usuarioSolicitante.getRol().getNombre();

                if (rol.equals("ROLE_MYPE") || rol.equals("MYPE")) {
                    var mypeSolicitante = mypeRepository.findByUsuarioId(usuarioSolicitante.getId());
                    if (mypeSolicitante.isPresent() && mypeSolicitante.get().getId().equals(mypeId)) {
                        nivelAcceso = "PROPIO";
                    }
                }

                if (rol.equals("ROLE_ESTUDIANTE") || rol.equals("ESTUDIANTE")) {
                    var estudiante = estudianteRepository.findByUsuarioId(usuarioSolicitante.getId());
                    if (estudiante.isPresent()) {
                        boolean tieneProyectoConfirmado = postulacionRepository
                                .findByEstudianteIdAndEstado(
                                        estudiante.get().getId(),
                                        EstadoPostulacion.CONFIRMADO
                                )
                                .stream()
                                .anyMatch(p -> p.getProyecto().getMype().getId().equals(mypeId));

                        if (tieneProyectoConfirmado) {
                            nivelAcceso = "CONFIRMADO";
                        }
                    }
                }

                if (rol.equals("ROLE_ADMIN") || rol.equals("ADMIN")) {
                    nivelAcceso = "PROPIO";
                }
            }
        }

        var todosProyectos = proyectoRepository.findByMypeIdConMype(mypeId);
        long totalProyectos = todosProyectos.size();
        long proyectosActivos = todosProyectos.stream()
                .filter(p -> p.getEstado() == WorkflowEstado.PENDIENTE
                        || p.getEstado() == WorkflowEstado.EN_DESARROLLO)
                .count();

        boolean tieneAcceso = nivelAcceso.equals("PROPIO") || nivelAcceso.equals("CONFIRMADO");

        List<ProyectoResponse> proyectosResponse = todosProyectos.stream()
                .filter(p -> tieneAcceso || p.getEstado() == WorkflowEstado.PENDIENTE)
                .map(this::toResponse)
                .toList();

        return new MypePerfilResponse(
                mype.getId(),
                mype.getNombreComercial(),
                mype.getRazonSocial(),
                mype.getRubro(),
                mype.getUsuario().getFotoPerfil(),
                mype.getDescripcion(),
                mype.getSitioWeb(),
                mype.getInstagram(),
                mype.getFacebook(),
                mype.getTiktok(),
                mype.getWhatsapp(),
                tieneAcceso ? mype.getRuc()           : null,
                tieneAcceso ? mype.getDireccion()     : null,
                tieneAcceso ? mype.getTelefono()      : null,
                tieneAcceso ? mype.getEmailContacto() : null,
                nivelAcceso,
                totalProyectos,
                proyectosActivos,
                proyectosResponse
        );
    }

    @Transactional
    public List<InsumoProyectoResponse> subirInsumos(Long proyectoId, List<MultipartFile> archivos,
                                                     List<Long> insumoTipoIds, List<String> valoresTexto,
                                                     String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));
        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para subir insumos a este proyecto", HttpStatus.FORBIDDEN);
        }

        List<InsumoProyectoResponse> responses = new ArrayList<>();
        for (int i = 0; i < archivos.size(); i++) {
            MultipartFile archivo = archivos.get(i);
            Long insumoTipoId = (insumoTipoIds != null && i < insumoTipoIds.size()) ? insumoTipoIds.get(i) : null;
            String valorTexto = (valoresTexto != null && i < valoresTexto.size()) ? valoresTexto.get(i) : null;

            InsumoTipo insumoTipo = null;
            if (insumoTipoId != null) {
                insumoTipo = insumoTipoRepository.findById(insumoTipoId).orElse(null);
            }

            String archivoUrl = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl = s3Service.subirInsumo(archivo);
            }

            InsumoProyecto insumo = InsumoProyecto.builder()
                    .proyecto(proyecto)
                    .insumoTipo(insumoTipo)
                    .valorTexto(valorTexto)
                    .archivoUrl(archivoUrl)
                    .build();
            insumo = insumoProyectoRepository.save(insumo);
            responses.add(new InsumoProyectoResponse(insumo.getId(), proyectoId,
                    insumoTipoId, insumoTipo != null ? insumoTipo.getNombre() : null,
                    valorTexto, archivoUrl));
        }
        return responses;
    }

    @Transactional
    public MypePerfilResponse actualizarPerfil(Long mypeId, ActualizarMypeRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));

        if (!mype.getId().equals(mypeId)) {
            throw new BusinessException("No tienes permiso para editar este perfil", HttpStatus.FORBIDDEN);
        }

        if (request.rubro()         != null) mype.setRubro(request.rubro().isBlank() ? null : request.rubro());
        if (request.descripcion()   != null) mype.setDescripcion(request.descripcion().isBlank() ? null : request.descripcion());
        if (request.sitioWeb()      != null) mype.setSitioWeb(request.sitioWeb().isBlank() ? null : request.sitioWeb());
        if (request.instagram()     != null) mype.setInstagram(request.instagram().isBlank() ? null : request.instagram());
        if (request.facebook()      != null) mype.setFacebook(request.facebook().isBlank() ? null : request.facebook());
        if (request.tiktok()        != null) mype.setTiktok(request.tiktok().isBlank() ? null : request.tiktok());
        if (request.whatsapp()      != null) mype.setWhatsapp(request.whatsapp().isBlank() ? null : request.whatsapp());
        if (request.direccion()     != null) mype.setDireccion(request.direccion().isBlank() ? null : request.direccion());
        if (request.telefono()      != null) mype.setTelefono(request.telefono().isBlank() ? null : request.telefono());
        if (request.emailContacto() != null) mype.setEmailContacto(request.emailContacto().isBlank() ? null : request.emailContacto());

        mypeRepository.save(mype);
        return obtenerPerfilMype(mypeId, emailMype);
    }

    @Transactional(readOnly = true)
    public MypePerfilResponse miPerfilMype(String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        return obtenerPerfilMype(mype.getId(), emailMype);
    }

    // ══════════════════════════════════════════════════════════════
    // MAPPERS (sin cambios)
    // ══════════════════════════════════════════════════════════════

    private ProyectoResponse toResponse(Proyecto p) {
        LocalDate fechaLimiteCalculada = null;
        if (p.getFechaInicioReal() != null && p.getDiasEstimados() != null) {
            fechaLimiteCalculada = p.getFechaInicioReal()
                    .plusDays(p.getDiasEstimados())
                    .toLocalDate();
        }

        long cuposOcupados = postulacionRepository.countByProyectoIdAndEstado(
                p.getId(), EstadoPostulacion.CONFIRMADO
        );

        return new ProyectoResponse(
                p.getId(), p.getTitulo(), p.getDescripcion(), p.getObjetivo(),
                p.getRequisitos(), p.getEntregablesSugeridos(), p.getAreaSistemas(),
                p.getEstado(), p.getCupos(), p.getFechaInicio(), p.getFechaLimite(),
                p.getFechaCreacion(),
                p.getMype() != null ? p.getMype().getNombreComercial() : null,
                p.getMype() != null ? p.getMype().getId() : null,
                p.getDiasEstimados(),
                p.getFechaInicioReal(),
                fechaLimiteCalculada,
                cuposOcupados
        );
    }

    private PostulacionResponse toPostulacionResponse(Postulacion p) {
        long activos = contarProyectosActivosDelEstudiante(p.getEstudiante().getId());
        int limite = p.getEstudiante().getLimiteProyectos();
        boolean ocupado = activos >= limite;

        System.out.println("=== DEPURACION POSTULACION ===");
        System.out.println("Postulacion ID: " + p.getId());
        System.out.println("Estudiante ID: " + p.getEstudiante().getId());
        System.out.println("Proyectos activos: " + activos);
        System.out.println("Límite del estudiante: " + limite);
        System.out.println("¿Está ocupado?: " + ocupado);

        return new PostulacionResponse(
                p.getId(), p.getProyecto().getId(), p.getProyecto().getTitulo(),
                p.getEstudiante().getId(), p.getEstudiante().getUsuario().getNombre(),
                p.getEstado(), p.getMensajePostulacion(), p.getFechaPostulacion(),
                p.getEstudiante().getCvUrl(),
                p.getFechaLimiteConfirmacion(),
                ocupado
        );
    }

    private long contarProyectosActivosDelEstudiante(Long estudianteId) {
        List<WorkflowEstado> proyectoEstadosActivos = List.of(
                WorkflowEstado.PENDIENTE,
                WorkflowEstado.EN_DESARROLLO,
                WorkflowEstado.EN_REVISION
        );
        long count = postulacionRepository.countByEstudianteIdAndEstadoAndProyectoEstadoIn(
                estudianteId,
                EstadoPostulacion.CONFIRMADO,
                proyectoEstadosActivos
        );
        System.out.println("contarProyectosActivosDelEstudiante(" + estudianteId + ") = " + count);
        return count;

    }
}