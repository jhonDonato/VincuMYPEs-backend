package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.EstudianteProfileResponse;
import com.mypelink.backend.usuarios.application.dto.UpdateEstudianteRequest;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.application.dto.EstudiantePublicoResponse;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final S3Service s3Service;
    private final PostulacionRepository postulacionRepository;
    private final MypeRepository mypeRepository;

    @Transactional(readOnly = true)
    public EstudianteProfileResponse getProfile(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));

        return mapToProfileResponse(estudiante);
    }

    @Transactional
    public EstudianteProfileResponse updateProfile(String email, UpdateEstudianteRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));

        // Actualizar campos existentes
        if (request.bio() != null) estudiante.setBio(request.bio());
        if (request.skills() != null) estudiante.setSkills(request.skills());
        if (request.portafolioUrl() != null) estudiante.setPortafolioUrl(request.portafolioUrl());
        if (request.linkedinUrl() != null) estudiante.setLinkedinUrl(request.linkedinUrl());
        if (request.carrera() != null) estudiante.setCarrera(request.carrera());
        if (request.universidad() != null) estudiante.setUniversidad(request.universidad());
        if (request.telefono() != null) usuario.setTelefono(request.telefono());

        // 📍 ACTUALIZAR UBICACIÓN
        if (request.ciudad() != null) estudiante.setCiudad(request.ciudad());
        if (request.pais() != null) estudiante.setPais(request.pais());
        if (request.sector() != null) estudiante.setSector(request.sector());
        if (request.barrio() != null) estudiante.setBarrio(request.barrio());
        if (request.lat() != null) estudiante.setLat(request.lat());
        if (request.lng() != null) estudiante.setLng(request.lng());

        estudianteRepository.save(estudiante);
        usuarioRepository.save(usuario);

        return mapToProfileResponse(estudiante);
    }

    @Transactional
    public EstudianteProfileResponse subirCv(String email, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de estudiante no encontrado"));

        String cvUrl = s3Service.subirCvPdf(archivo);
        estudiante.setCvUrl(cvUrl);
        estudianteRepository.save(estudiante);

        return mapToProfileResponse(estudiante);
    }
    @Transactional(readOnly = true)
    public EstudiantePublicoResponse obtenerPerfilPublico(Long estudianteId, String emailViewer) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

        Usuario viewer = usuarioRepository.findByEmailWithRole(emailViewer)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        String rol = viewer.getRol().getNombre();
        boolean isAdmin = rol.equals("ROLE_ADMIN") || rol.equals("ADMIN");
        boolean isMype = rol.equals("ROLE_MYPE") || rol.equals("MYPE");
        boolean isEstudiante = rol.equals("ROLE_ESTUDIANTE") || rol.equals("ESTUDIANTE");

        // ADMIN: acceso total
        if (isAdmin) {
            return mapToPerfilPublico(estudiante, true);
        }

        // MYPE: solo si el estudiante postuló a algún proyecto de su MYPE
        if (isMype) {
            var mype = mypeRepository.findByUsuarioId(viewer.getId())
                    .orElseThrow(() -> new BusinessException("Perfil de MYPE no encontrado"));
            boolean tieneRelacion = postulacionRepository
                    .existsPostulacionDeEstudianteEnProyectoDeMype(estudianteId, mype.getId());
            if (!tieneRelacion) {
                throw new BusinessException(
                        "No tienes acceso al perfil de este estudiante",
                        HttpStatus.FORBIDDEN
                );
            }
            return mapToPerfilPublico(estudiante, false);
        }

        // ESTUDIANTE: solo si comparten proyecto EN_DESARROLLO, o si es su propio perfil
        if (isEstudiante) {
            var estudianteViewer = estudianteRepository.findByUsuarioId(viewer.getId())
                    .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

            // Caso "verme a mí mismo": preview de la vista pública
            if (estudianteViewer.getId().equals(estudianteId)) {
                return mapToPerfilPublico(estudiante, false);
            }

            boolean comparten = postulacionRepository.compartenProyectoEnDesarrollo(
                    estudianteViewer.getId(),
                    estudianteId,
                    EstadoPostulacion.CONFIRMADO,
                    WorkflowEstado.EN_DESARROLLO
            );
            if (!comparten) {
                throw new BusinessException(
                        "No tienes acceso al perfil de este estudiante",
                        HttpStatus.FORBIDDEN
                );
            }
            return mapToPerfilPublico(estudiante, false);
        }

        // Cualquier otro rol
        throw new BusinessException(
                "No tienes acceso al perfil de este estudiante",
                HttpStatus.FORBIDDEN
        );
    }

    private EstudiantePublicoResponse mapToPerfilPublico(Estudiante estudiante, boolean revelarSensibles) {
        Usuario usuario = estudiante.getUsuario();
        return new EstudiantePublicoResponse(
                estudiante.getId(),
                usuario.getNombre(),
                usuario.getFotoPerfil(),
                estudiante.getUniversidad(),
                estudiante.getCarrera(),
                estudiante.getBio(),
                estudiante.getSkills(),
                estudiante.getPortafolioUrl(),
                estudiante.getLinkedinUrl(),
                estudiante.getCvUrl(),
                estudiante.getCiudad(),
                estudiante.getSector(),
                estudiante.getPais(),
                // ─── Solo si el viewer es admin ───
                revelarSensibles ? usuario.getEmail() : null,
                revelarSensibles ? usuario.getTelefono() : null,
                revelarSensibles ? estudiante.getCodigoEstudiante() : null,
                revelarSensibles ? estudiante.getBarrio() : null,
                revelarSensibles ? estudiante.getLat() : null,
                revelarSensibles ? estudiante.getLng() : null
        );
    }

    private EstudianteProfileResponse mapToProfileResponse(Estudiante estudiante) {
        Usuario usuario = estudiante.getUsuario();
        return new EstudianteProfileResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFotoPerfil(),
                estudiante.getCodigoEstudiante(),
                estudiante.getUniversidad(),
                estudiante.getCarrera(),
                estudiante.getBio(),
                estudiante.getSkills(),
                estudiante.getPortafolioUrl(),
                estudiante.getLinkedinUrl(),
                estudiante.getCvUrl(),
                estudiante.getLimiteProyectos(),
                // 📍 MAPEAR UBICACIÓN
                estudiante.getCiudad(),
                estudiante.getPais(),
                estudiante.getSector(),
                estudiante.getBarrio(),
                estudiante.getLat(),
                estudiante.getLng()
        );
    }
}