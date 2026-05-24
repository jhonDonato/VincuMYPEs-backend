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

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final S3Service s3Service;

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