package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.certificaciones.application.dto.CertificadoResponse;
import com.mypelink.backend.certificaciones.application.dto.EmitirCertificadoRequest;
import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificadoService {

    private final CertificadoRepository certificadoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Transactional
    public CertificadoResponse emitirCertificado(String emailMype, EmitirCertificadoRequest request) {
        Usuario usuarioMype = usuarioRepository.findByEmail(emailMype)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario MYPE no encontrado"));

        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        // Verificar que el proyecto pertenece a la MYPE que emite
        if (!proyecto.getMype().getUsuario().getId().equals(usuarioMype.getId())) {
            throw new BusinessException("No tienes permiso para emitir certificados para este proyecto");
        }

        // Verificar que el estudiante postuló y fue aceptado
        Postulacion postulacion = postulacionRepository.findByProyectoIdAndEstudianteId(request.proyectoId(), request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException("El estudiante no tiene una postulación en este proyecto"));

        if (postulacion.getEstado() != EstadoPostulacion.ACEPTADO && postulacion.getEstado() != EstadoPostulacion.CONFIRMADO) {
            throw new BusinessException("Solo se pueden emitir certificados a estudiantes con postulación ACEPTADA o CONFIRMADA");
        }

        // Verificar si ya existe un certificado
        if (certificadoRepository.existsByProyectoIdAndEstudianteId(request.proyectoId(), request.estudianteId())) {
            throw new BusinessException("Ya existe un certificado para este estudiante en este proyecto");
        }

        // Generar código único para el certificado
        String codigo = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Certificado certificado = Certificado.builder()
                .proyecto(proyecto)
                .estudiante(postulacion.getEstudiante())
                .codigo(codigo)
                .tituloCertificado(request.tituloCertificado())
                .descripcionCertificado(request.descripcionCertificado())
                .urlCertificado(request.urlCertificado())
                .emitidoPor(usuarioMype)
                .build();

        Certificado guardado = certificadoRepository.save(certificado);

        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarMisCertificados(String emailEstudiante) {
        return certificadoRepository.findByEstudianteUsuarioEmail(emailEstudiante)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarCertificadosEmitidos(String emailMype) {
        return certificadoRepository.findByProyectoMypeUsuarioEmail(emailMype)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CertificadoResponse mapToResponse(Certificado certificado) {
        return new CertificadoResponse(
                certificado.getId(),
                certificado.getProyecto().getId(),
                certificado.getProyecto().getTitulo(),
                certificado.getEstudiante().getId(),
                certificado.getEstudiante().getUsuario().getNombre(),
                certificado.getCodigo(),
                certificado.getTituloCertificado(),
                certificado.getDescripcionCertificado(),
                certificado.getFechaEmision(),
                certificado.getUrlCertificado()
        );
    }

    @Transactional
    public void enviarCertificado(Long certificadoId, String emailMype) {
        var certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado", certificadoId));

        // Verificar que la MYPE es la emisora
        if (!certificado.getEmitidoPor().getEmail().equals(emailMype)) {
            throw new BusinessException("No tienes permiso para enviar este certificado",
                    HttpStatus.FORBIDDEN);
        }

        var estudianteEmail = certificado.getEstudiante().getUsuario().getEmail();
        var estudianteNombre = certificado.getEstudiante().getUsuario().getNombre();
        var empresaNombre = certificado.getEmitidoPor().getNombre();

        emailService.enviarCertificado(
                estudianteEmail,
                estudianteNombre,
                certificado.getTituloCertificado(),
                empresaNombre,
                certificado.getCodigo()
        );
    }
}
