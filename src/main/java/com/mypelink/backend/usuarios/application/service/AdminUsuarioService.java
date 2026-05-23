package com.mypelink.backend.usuarios.application.service;

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

@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(u -> {
            String rolCompleto = u.getRol().getNombre();
            String rolSimplificado = rolCompleto.replace("ROLE_", "");
            
            String estado = u.getActivo() ? "ACTIVO" : "SUSPENDIDO";
            String carrera = null;
            String sector = null;
            Integer limiteProyectos = null;

            if (rolCompleto.equals("ROLE_ESTUDIANTE")) {
                var estudianteOpt = estudianteRepository.findByUsuarioId(u.getId());
                if (estudianteOpt.isPresent()) {
                    Estudiante est = estudianteOpt.get();
                    carrera = est.getCarrera();
                    limiteProyectos = est.getLimiteProyectos();
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
                    rolSimplificado,
                    estado,
                    carrera,
                    sector,
                    limiteProyectos
            );
        }).collect(Collectors.toList());
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
            });
        }
    }

    @Transactional
    public void cambiarBypassLimite(Long usuarioId, Integer nuevoLimite) {
        if (nuevoLimite == null || nuevoLimite < 1) {
            throw new BusinessException("El límite de proyectos debe ser al menos 1");
        }
        
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));
        
        estudiante.setLimiteProyectos(nuevoLimite);
        estudianteRepository.save(estudiante);
    }
}
