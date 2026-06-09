// com.mypelink.backend.usuarios.application.service.AdminUsuarioService.java (CORREGIDO)
package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.model.*;
import com.mypelink.backend.usuarios.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== LISTAR USUARIOS (ya existente) ====================

    @Transactional(readOnly = true)
    public Page<AdminUsuarioResponse> listarUsuarios(Pageable pageable, String rol) {
        Page<Usuario> usuariosPage;
        if (rol != null && !rol.isEmpty() && !"TODOS".equals(rol)) {
            String rolNombre = "ROLE_" + rol;
            usuariosPage = usuarioRepository.findByRolNombre(rolNombre, pageable);
        } else {
            usuariosPage = usuarioRepository.findAll(pageable);
        }
        return usuariosPage.map(this::toAdminResponse);
    }

    // ==================== NUEVO: OBTENER DETALLE DE USUARIO (CORREGIDO) ====================

    @Transactional(readOnly = true)
    public UsuarioDetailAdminResponse obtenerDetalleUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", usuarioId));

        // Usar Optional y variables locales para evitar problemas con lambdas
        UsuarioDetailAdminResponse.EstudianteInfo estudianteInfo = null;
        UsuarioDetailAdminResponse.MypeInfo mypeInfo = null;

        if (usuario.getRol().getNombre().equals("ROLE_ESTUDIANTE")) {
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByUsuarioId(usuarioId);
            if (estudianteOpt.isPresent()) {
                Estudiante est = estudianteOpt.get();
                estudianteInfo = new UsuarioDetailAdminResponse.EstudianteInfo(
                        est.getId(), est.getCodigoEstudiante(), est.getUniversidad(),
                        est.getCarrera(), est.getBio(), est.getSkills(),
                        est.getPortafolioUrl(), est.getLinkedinUrl(), est.getCvUrl(),
                        est.getLimiteProyectos(), est.getCiudad(), est.getPais(), est.getSector()
                );
            }
        } else if (usuario.getRol().getNombre().equals("ROLE_MYPE")) {
            Optional<Mype> mypeOpt = mypeRepository.findByUsuarioId(usuarioId);
            if (mypeOpt.isPresent()) {
                Mype mype = mypeOpt.get();
                mypeInfo = new UsuarioDetailAdminResponse.MypeInfo(
                        mype.getId(), mype.getNombreComercial(), mype.getRazonSocial(),
                        mype.getNombreRepresentante(), mype.getRuc(), mype.getRubro(),
                        mype.getDireccion(), mype.getTelefono(), mype.getEmailContacto(),
                        mype.getDescripcion(), mype.getSitioWeb(), mype.getInstagram(),
                        mype.getFacebook(), mype.getTiktok(), mype.getWhatsapp(),
                        mype.getCiudad(), mype.getSector()
                );
            }
        }

        return new UsuarioDetailAdminResponse(
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getDni(),
                usuario.getTelefono(), usuario.getFotoPerfil(),
                usuario.getRol().getNombre().replace("ROLE_", ""),
                usuario.getActivo(), usuario.getEmailVerified(),
                usuario.getFechaRegistro(), usuario.getUltimaSesion(),
                estudianteInfo, mypeInfo
        );
    }

    // ==================== NUEVO: CREAR USUARIO ====================

    @Transactional
    public AdminUsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        // Validaciones básicas
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El email ya está registrado", HttpStatus.CONFLICT);
        }
        if (request.telefono() != null && usuarioRepository.existsByTelefono(request.telefono())) {
            throw new BusinessException("El teléfono ya está registrado", HttpStatus.CONFLICT);
        }

        Role role = roleRepository.findByNombre("ROLE_" + request.rol())
                .orElseThrow(() -> new BusinessException("Rol no encontrado: " + request.rol()));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(role)
                .activo(true)
                .emailVerified(true) // Admin puede marcar como verificado directamente
                .build();
        usuario = usuarioRepository.save(usuario);

        // Crear perfil específico según rol
        if (request.rol().equals("ESTUDIANTE")) {
            Estudiante estudiante = Estudiante.builder()
                    .usuario(usuario)
                    .codigoEstudiante(request.codigoEstudiante())
                    .carrera(request.carrera() != null ? request.carrera() : "Ingeniería de Sistemas Computacionales")
                    .universidad(request.universidad() != null ? request.universidad() : "Universidad Privada del Norte")
                    .limiteProyectos(1)
                    .activo(true)
                    .build();
            estudianteRepository.save(estudiante);
        } else if (request.rol().equals("MYPE")) {
            Mype mype = Mype.builder()
                    .usuario(usuario)
                    .nombreComercial(request.nombreComercial())
                    .razonSocial(request.razonSocial())
                    .ruc(request.ruc())
                    .rubro(request.rubro())
                    .direccion(request.direccion())
                    .activo(true)
                    .build();
            mypeRepository.save(mype);
        }

        log.info("Admin creó nuevo usuario: {} con rol {}", request.email(), request.rol());
        return toAdminResponse(usuario);
    }

    // ==================== NUEVO: ACTUALIZAR USUARIO ====================

    @Transactional
    public AdminUsuarioResponse actualizarUsuario(Long usuarioId, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", usuarioId));

        String rolActual = usuario.getRol().getNombre().replace("ROLE_", "");

        // Actualizar campos base de Usuario
        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (!usuario.getEmail().equals(request.email()) && usuarioRepository.existsByEmail(request.email())) {
                throw new BusinessException("El email ya está en uso", HttpStatus.CONFLICT);
            }
            usuario.setEmail(request.email());
        }
        if (request.telefono() != null) {
            if (usuario.getTelefono() != null && !usuario.getTelefono().equals(request.telefono())
                    && usuarioRepository.existsByTelefono(request.telefono())) {
                throw new BusinessException("El teléfono ya está en uso", HttpStatus.CONFLICT);
            }
            usuario.setTelefono(request.telefono());
        }

        // Cambiar rol (con validaciones)
        if (request.rol() != null && !request.rol().isBlank() && !request.rol().equals(rolActual)) {
            Role nuevoRol = roleRepository.findByNombre("ROLE_" + request.rol())
                    .orElseThrow(() -> new BusinessException("Rol no encontrado: " + request.rol()));
            usuario.setRol(nuevoRol);
        }

        usuario = usuarioRepository.save(usuario);

        // Actualizar perfil específico
        if (rolActual.equals("ESTUDIANTE") || (request.rol() != null && request.rol().equals("ESTUDIANTE"))) {
            estudianteRepository.findByUsuarioId(usuarioId).ifPresent(est -> {
                if (request.codigoEstudiante() != null) est.setCodigoEstudiante(request.codigoEstudiante());
                if (request.carrera() != null) est.setCarrera(request.carrera());
                if (request.universidad() != null) est.setUniversidad(request.universidad());
                if (request.limiteProyectos() != null) est.setLimiteProyectos(request.limiteProyectos());
                estudianteRepository.save(est);
            });
        } else if (rolActual.equals("MYPE") || (request.rol() != null && request.rol().equals("MYPE"))) {
            mypeRepository.findByUsuarioId(usuarioId).ifPresent(mype -> {
                if (request.nombreComercial() != null) mype.setNombreComercial(request.nombreComercial());
                if (request.razonSocial() != null) mype.setRazonSocial(request.razonSocial());
                if (request.ruc() != null) mype.setRuc(request.ruc());
                if (request.rubro() != null) mype.setRubro(request.rubro());
                if (request.direccion() != null) mype.setDireccion(request.direccion());
                if (request.descripcion() != null) mype.setDescripcion(request.descripcion());
                if (request.sitioWeb() != null) mype.setSitioWeb(request.sitioWeb());
                if (request.instagram() != null) mype.setInstagram(request.instagram());
                if (request.facebook() != null) mype.setFacebook(request.facebook());
                if (request.tiktok() != null) mype.setTiktok(request.tiktok());
                if (request.whatsapp() != null) mype.setWhatsapp(request.whatsapp());
                mypeRepository.save(mype);
            });
        }

        log.info("Admin actualizó usuario ID: {}", usuarioId);
        return toAdminResponse(usuario);
    }

    // ==================== NUEVO: ELIMINAR USUARIO (SOFT DELETE) ====================

    @Transactional
    public void eliminarUsuario(Long usuarioId, boolean permanente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", usuarioId));

        if (usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            long adminCount = usuarioRepository.countByRolNombre("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new BusinessException("No se puede eliminar al único administrador del sistema", HttpStatus.BAD_REQUEST);
            }
        }

        if (permanente) {
            // Eliminación permanente
            if (usuario.getRol().getNombre().equals("ROLE_ESTUDIANTE")) {
                estudianteRepository.findByUsuarioId(usuarioId).ifPresent(estudianteRepository::delete);
            } else if (usuario.getRol().getNombre().equals("ROLE_MYPE")) {
                mypeRepository.findByUsuarioId(usuarioId).ifPresent(mypeRepository::delete);
            }
            usuarioRepository.delete(usuario);
            log.warn("Admin eliminó permanentemente al usuario ID: {}", usuarioId);
        } else {
            // Soft delete (desactivar)
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            log.info("Admin desactivó al usuario ID: {}", usuarioId);
        }
    }

    // ==================== MÉTODOS EXISTENTES ====================

    @Transactional
    public void cambiarEstadoUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            long adminCount = usuarioRepository.countByRolNombre("ROLE_ADMIN");
            if (adminCount <= 1 && usuario.getActivo()) {
                throw new BusinessException("No se puede desactivar al único administrador del sistema", HttpStatus.BAD_REQUEST);
            }
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
        if (nuevoLimite == null || nuevoLimite < 1 || nuevoLimite > 5) {
            throw new BusinessException("El límite debe estar entre 1 y 5");
        }
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));
        estudiante.setLimiteProyectos(nuevoLimite);
        estudianteRepository.save(estudiante);
    }

    private AdminUsuarioResponse toAdminResponse(Usuario u) {
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
            }
        } else if (rolCompleto.equals("ROLE_MYPE")) {
            var mypeOpt = mypeRepository.findByUsuarioId(u.getId());
            if (mypeOpt.isPresent()) {
                sector = mypeOpt.get().getRubro();
            }
        }

        return new AdminUsuarioResponse(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getTelefono() != null ? u.getTelefono() : "",
                rolSimplificado, estado, carrera, sector,
                limiteProyectos, promedioEstrellas, proyectosCompletados
        );
    }
}