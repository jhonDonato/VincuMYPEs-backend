package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.ejecucion.domain.model.Evaluacion;
import com.mypelink.backend.ejecucion.domain.repository.EvaluacionRepository;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.application.dto.AdminUsuarioResponse;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final EvaluacionRepository evaluacionRepository;

    // AdminUsuarioService.java
    @Transactional(readOnly = true)
    public Page<AdminUsuarioResponse> listarUsuarios(Pageable pageable, String rol) {
        Page<Usuario> usuariosPage;

        // Filtrar por rol si se proporciona y no es "TODOS"
        if (rol != null && !rol.isEmpty() && !"TODOS".equals(rol)) {
            String rolNombre = "ROLE_" + rol;
            usuariosPage = usuarioRepository.findByRolNombre(rolNombre, pageable);
        } else {
            usuariosPage = usuarioRepository.findAll(pageable);
        }

        return usuariosPage.map(u -> {
            String rolCompleto = u.getRol().getNombre();
            String rolSimplificado = rolCompleto.replace("ROLE_", "");
            String estado = u.getActivo() ? "ACTIVO" : "SUSPENDIDO";
            String carrera = null;
            String sector = null;
            Integer limiteProyectos = null;
            Double promedioEstrellas = null;
            Long proyectosCompletados = null;

            if (rolCompleto.equals("ROLE_ESTUDIANTE")) {
                var estudianteOpt = estudianteRepository.findByUsuarioId(u.getId());
                if (estudianteOpt.isPresent()) {
                    Estudiante est = estudianteOpt.get();
                    carrera = est.getCarrera();
                    limiteProyectos = est.getLimiteProyectos();
                    List<Postulacion> postulaciones = postulacionRepository
                            .findByEstudianteIdWithDetails(est.getId());
                    proyectosCompletados = postulaciones.stream()
                            .filter(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                            .count();
                    List<Evaluacion> evaluaciones = evaluacionRepository
                            .findByEstudianteId(est.getId());
                    double promedio = evaluaciones.stream()
                            .mapToDouble(e -> (e.getPuntualidad() + e.getCalidadTrabajo() + e.getComunicacion()) / 3.0)
                            .average()
                            .orElse(0.0);
                    promedioEstrellas = Math.round(promedio * 10.0) / 10.0;
                }
            } else if (rolCompleto.equals("ROLE_MYPE")) {
                var mypeOpt = mypeRepository.findByUsuarioId(u.getId());
                if (mypeOpt.isPresent()) {
                    Mype mype = mypeOpt.get();
                    sector = mype.getRubro();
                }
            }

            return new AdminUsuarioResponse(
                    u.getId(),
                    u.getNombre(),
                    u.getEmail(),
                    u.getTelefono() != null ? u.getTelefono() : "",
                    rolSimplificado,
                    estado,
                    carrera,
                    sector,
                    limiteProyectos,
                    promedioEstrellas,
                    proyectosCompletados
            );
        });
    }

    @Transactional
    public void cambiarEstadoUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("No se pueden suspender cuentas de administradores");
        }

        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);

        if (usuario.getRol().getNombre().equals("ROLE_ESTUDIANTE")) {
            estudianteRepository.findByUsuarioId(usuarioId).ifPresent(est -> {
                est.setActivo(usuario.getActivo());
                estudianteRepository.save(est);
            });
        } else if (usuario.getRol().getNombre().equals("ROLE_MYPE")) {
            mypeRepository.findByUsuarioId(usuarioId).ifPresent(m -> {
                m.setActivo(usuario.getActivo());
                mypeRepository.save(m);
                if (!usuario.getActivo()) {
                    List<Proyecto> proyectos = proyectoRepository.findByMypeId(m.getId());
                    for (Proyecto p : proyectos) {
                        if (p.getEstado() != WorkflowEstado.COMPLETADO
                                && p.getEstado() != WorkflowEstado.BORRADOR) {
                            p.setEstado(WorkflowEstado.COMPLETADO);
                            proyectoRepository.save(p);
                        }
                    }
                }
            });
        }
    }

    @Transactional
    public void cambiarBypassLimite(Long usuarioId, Integer nuevoLimite) {
        if (nuevoLimite == null || nuevoLimite < 1 || nuevoLimite > 3) {
            throw new BusinessException("El límite debe estar entre 1 y 3");
        }

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));

        // Validar reputación para bypass a 2 o 3
        if (nuevoLimite > 1) {
            List<Postulacion> postulaciones = postulacionRepository
                    .findByEstudianteIdWithDetails(estudiante.getId());
            long completados = postulaciones.stream()
                    .filter(p -> p.getEstado() == EstadoPostulacion.CONFIRMADO)
                    .count();
            List<Evaluacion> evaluaciones = evaluacionRepository
                    .findByEstudianteId(estudiante.getId());
            double promedio = evaluaciones.stream()
                    .mapToDouble(e -> (e.getPuntualidad() + e.getCalidadTrabajo() + e.getComunicacion()) / 3.0)
                    .average()
                    .orElse(0.0);

            if (nuevoLimite >= 2 && (promedio < 4.0 || completados < 4)) {
                throw new BusinessException(
                        "El estudiante necesita 4+ estrellas y al menos 4 proyectos completados para acceder a 2 proyectos simultáneos");
            }
            if (nuevoLimite == 3 && (promedio < 4.0 || completados < 15)) {
                throw new BusinessException(
                        "Se requieren 15+ proyectos completados y 4+ estrellas para acceder a 3 proyectos");
            }
        }

        estudiante.setLimiteProyectos(nuevoLimite);
        estudianteRepository.save(estudiante);
    }
}