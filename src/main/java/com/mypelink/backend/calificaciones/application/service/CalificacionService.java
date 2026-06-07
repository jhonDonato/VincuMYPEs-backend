// com.mypelink.backend.calificaciones.application.service.CalificacionService
package com.mypelink.backend.calificaciones.application.service;

import com.mypelink.backend.calificaciones.application.dto.*;
import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MypeRepository mypeRepository;

    @Transactional
    public void crear(CrearCalificacionRequest request, String emailCalificador) {
        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        if (proyecto.getEstado() != WorkflowEstado.COMPLETADO) {
            throw new BusinessException("Solo puedes calificar proyectos completados", HttpStatus.BAD_REQUEST);
        }

        Usuario calificador = usuarioRepository.findByEmailWithRole(emailCalificador)
                .orElseThrow(() -> new BusinessException("Usuario calificador no encontrado"));

        Usuario calificado = usuarioRepository.findById(request.calificadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario a calificar no encontrado"));

        if (calificador.getId().equals(calificado.getId())) {
            throw new BusinessException("No puedes calificarte a ti mismo", HttpStatus.BAD_REQUEST);
        }

        String rolCalificador = calificador.getRol().getNombre().replace("ROLE_", "");
        String rolCalificado = calificado.getRol().getNombre().replace("ROLE_", "");

        // Validación para MYPE calificando ESTUDIANTE
        if (rolCalificador.equals("MYPE")) {
            // Verificar que la MYPE sea la dueña del proyecto
            Mype mype = mypeRepository.findByUsuarioId(calificador.getId())
                    .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
            if (!proyecto.getMype().getId().equals(mype.getId())) {
                throw new BusinessException("Solo la MYPE dueña del proyecto puede calificar", HttpStatus.FORBIDDEN);
            }
            // El calificado debe ser estudiante
            if (!rolCalificado.equals("ESTUDIANTE")) {
                throw new BusinessException("Una MYPE solo puede calificar estudiantes", HttpStatus.BAD_REQUEST);
            }
            // El estudiante debe haber sido CONFIRMADO en el proyecto
            boolean fueConfirmado = postulacionRepository.existsByProyectoIdAndEstudianteUsuarioIdAndEstado(
                    proyecto.getId(), calificado.getId(), EstadoPostulacion.CONFIRMADO
            );
            if (!fueConfirmado) {
                throw new BusinessException("El estudiante no participó en este proyecto", HttpStatus.BAD_REQUEST);
            }
        }
        // Validación para ESTUDIANTE calificando MYPE
        else if (rolCalificador.equals("ESTUDIANTE")) {
            // Verificar que el estudiante haya estado CONFIRMADO en el proyecto
            boolean participo = postulacionRepository.existsByProyectoIdAndEstudianteUsuarioIdAndEstado(
                    proyecto.getId(), calificador.getId(), EstadoPostulacion.CONFIRMADO
            );
            if (!participo) {
                throw new BusinessException("No participaste en este proyecto", HttpStatus.FORBIDDEN);
            }
            // El calificado debe ser la MYPE dueña del proyecto
            if (!rolCalificado.equals("MYPE")) {
                throw new BusinessException("Un estudiante solo puede calificar a la MYPE", HttpStatus.BAD_REQUEST);
            }
            if (!proyecto.getMype().getUsuario().getId().equals(calificado.getId())) {
                throw new BusinessException("La MYPE indicada no es la dueña del proyecto", HttpStatus.BAD_REQUEST);
            }
        }
        else {
            throw new BusinessException("Solo MYPEs y estudiantes pueden calificar", HttpStatus.FORBIDDEN);
        }

        // Verificar duplicado
        if (calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(
                proyecto.getId(), calificador.getId(), calificado.getId())) {
            throw new BusinessException("Ya calificaste a este usuario en este proyecto", HttpStatus.CONFLICT);
        }

        Calificacion calificacion = Calificacion.builder()
                .proyecto(proyecto)
                .calificador(calificador)
                .calificado(calificado)
                .puntuacion(request.puntuacion())
                .build();
        calificacionRepository.save(calificacion);
    }

    @Transactional(readOnly = true)
    public List<CalificacionPendienteResponse> obtenerPendientes(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        String rol = usuario.getRol().getNombre().replace("ROLE_", "");
        List<CalificacionPendienteResponse> pendientes = new ArrayList<>();

        if (rol.equals("MYPE")) {
            Mype mype = mypeRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
            // Obtener proyectos COMPLETADOS de esta MYPE
            List<Proyecto> proyectosCompletados = proyectoRepository.findByMypeIdAndEstado(mype.getId(), WorkflowEstado.COMPLETADO);
            for (Proyecto p : proyectosCompletados) {
                // Obtener postulaciones CONFIRMADAS del proyecto
                var postulaciones = postulacionRepository.findByProyectoIdAndEstado(p.getId(), EstadoPostulacion.CONFIRMADO);
                for (var post : postulaciones) {
                    Usuario estUsuario = post.getEstudiante().getUsuario();
                    boolean yaCalifico = calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(
                            p.getId(), usuario.getId(), estUsuario.getId()
                    );
                    if (!yaCalifico) {
                        pendientes.add(new CalificacionPendienteResponse(
                                p.getId(),
                                p.getTitulo(),
                                estUsuario.getId(),
                                estUsuario.getNombre(),
                                estUsuario.getFotoPerfil(),
                                "ESTUDIANTE"
                        ));
                    }
                }
            }
        }
        else if (rol.equals("ESTUDIANTE")) {
            // Obtener postulaciones CONFIRMADAS del estudiante
            var postulacionesConfirmadas = postulacionRepository.findByEstudianteUsuarioIdAndEstado(usuario.getId(), EstadoPostulacion.CONFIRMADO);
            for (var post : postulacionesConfirmadas) {
                Proyecto p = post.getProyecto();
                if (p.getEstado() != WorkflowEstado.COMPLETADO) continue;
                Usuario mypeUsuario = p.getMype().getUsuario();
                boolean yaCalifico = calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(
                        p.getId(), usuario.getId(), mypeUsuario.getId()
                );
                if (!yaCalifico) {
                    pendientes.add(new CalificacionPendienteResponse(
                            p.getId(),
                            p.getTitulo(),
                            mypeUsuario.getId(),
                            p.getMype().getNombreComercial(),
                            mypeUsuario.getFotoPerfil(),
                            "MYPE"
                    ));
                }
            }
        }

        return pendientes;
    }

    @Transactional(readOnly = true)
    public RatingResponse obtenerRating(Long usuarioId, String emailViewer) {
        Usuario target = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Usuario viewer = usuarioRepository.findByEmailWithRole(emailViewer)
                .orElseThrow(() -> new BusinessException("Viewer no encontrado"));

        String rolViewer = viewer.getRol().getNombre().replace("ROLE_", "");
        String rolTarget = target.getRol().getNombre().replace("ROLE_", "");

        boolean viewerIsAdmin = rolViewer.equals("ADMIN");
        boolean viewerIsMype = rolViewer.equals("MYPE");
        boolean viewerIsEstudiante = rolViewer.equals("ESTUDIANTE");
        boolean targetIsMype = rolTarget.equals("MYPE");
        boolean targetIsEstudiante = rolTarget.equals("ESTUDIANTE");

        boolean permitido = viewerIsAdmin ||
                (viewerIsMype && targetIsEstudiante) ||
                (viewerIsEstudiante && targetIsMype);

        if (!permitido) {
            throw new BusinessException("No tienes permiso para ver este rating", HttpStatus.FORBIDDEN);
        }

        Double promedio = calificacionRepository.promedioDeUsuario(usuarioId);
        long cantidad = calificacionRepository.cantidadDeUsuario(usuarioId);
        return new RatingResponse(promedio, cantidad);
    }
}