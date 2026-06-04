package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.certificaciones.application.dto.*;
import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificadoService {

    private final CertificadoRepository certificadoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PdfGeneratorService pdfGeneratorService;
    private final S3Service s3Service;

    @Transactional
    public List<CertificadoResponse> emitirCertificados(String emailMype, EmitirCertificadoRequest request) {
        Usuario usuarioMype = usuarioRepository.findByEmail(emailMype)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario MYPE no encontrado"));

        Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));

        if (!proyecto.getMype().getUsuario().getId().equals(usuarioMype.getId())) {
            throw new BusinessException("No tienes permiso para emitir certificados para este proyecto");
        }

        if (proyecto.getEstado() != WorkflowEstado.COMPLETADO) {
            throw new BusinessException("Solo se pueden emitir certificados cuando el proyecto está COMPLETADO");
        }

        List<CertificadoResponse> responses = new ArrayList<>();

        for (Long estudianteId : request.estudiantesIds()) {
            Postulacion postulacion = postulacionRepository.findByProyectoIdAndEstudianteId(request.proyectoId(), estudianteId)
                    .orElseThrow(() -> new ResourceNotFoundException("El estudiante no tiene una postulación en este proyecto"));

            if (postulacion.getEstado() != EstadoPostulacion.CONFIRMADO) {
                throw new BusinessException("El estudiante no ha confirmado su participación en el proyecto");
            }

            if (certificadoRepository.existsByProyectoIdAndEstudianteId(request.proyectoId(), estudianteId)) {
                log.warn("El estudiante {} ya tiene un certificado para este proyecto", estudianteId);
                continue;
            }

            String codigo = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Certificado certificado = Certificado.builder()
                    .proyecto(proyecto)
                    .estudiante(postulacion.getEstudiante())
                    .codigo(codigo)
                    .tituloCertificado(request.tituloCertificado())
                    .descripcionCertificado(request.descripcionCertificado())
                    .emitidoPor(usuarioMype)
                    .enviadoEmail(false)
                    .build();

            Certificado guardado = certificadoRepository.save(certificado);

            log.debug("[Certificado] Emitido - estudianteId={}, proyectoId={}, titulo={}",
                    postulacion.getEstudiante().getId(),
                    proyecto.getId(),
                    request.tituloCertificado());

            // Recargar con relaciones
            Certificado full = certificadoRepository.findById(guardado.getId())
                    .orElseThrow(() -> new RuntimeException("Certificado no encontrado después de guardar"));

            // Generar PDF y subir a S3
            byte[] pdfBytes = pdfGeneratorService.generarCertificadoPDF(
                    full, proyecto, proyecto.getMype(),
                    request.firmaBase64(), request.gerenteNombre());

            String pdfUrl = s3Service.subirCertificado(pdfBytes, "certificados/" + codigo + ".pdf");
            full.setUrlCertificado(pdfUrl);
            certificadoRepository.save(full);

            responses.add(mapToResponse(full));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarMisCertificados(String emailEstudiante) {
        return certificadoRepository.findByEstudianteUsuarioEmailEager(emailEstudiante)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarCertificadosEmitidos(String emailMype) {
        return certificadoRepository.findByProyectoMypeUsuarioEmailEager(emailMype)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificadoAdminResponse> listarTodosCertificados() {
        return certificadoRepository.findAll().stream()
                .map(c -> new CertificadoAdminResponse(
                        c.getId(),
                        c.getCodigo(),
                        c.getTituloCertificado(),
                        c.getDescripcionCertificado(),
                        c.getEstudiante().getUsuario().getNombre(),
                        c.getEstudiante().getUsuario().getEmail(),
                        c.getProyecto().getTitulo(),
                        c.getProyecto().getMype().getNombreComercial(),
                        c.getUrlCertificado(),
                        c.getFechaEmision()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void enviarCertificado(Long certificadoId, String emailMype) {
        Certificado certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado no encontrado", certificadoId));

        if (!certificado.getEmitidoPor().getEmail().equals(emailMype)) {
            throw new BusinessException("No tienes permiso para enviar este certificado", HttpStatus.FORBIDDEN);
        }

        if (certificado.getEnviadoEmail() != null && certificado.getEnviadoEmail()) {
            throw new BusinessException("Este certificado ya ha sido enviado al estudiante");
        }

        // Marcar como enviado ANTES de enviar el email
        certificado.setEnviadoEmail(true);
        certificadoRepository.save(certificado);

        // Enviar email
        emailService.enviarCertificado(
                certificado.getEstudiante().getUsuario().getEmail(),
                certificado.getEstudiante().getUsuario().getNombre(),
                certificado.getTituloCertificado(),
                certificado.getProyecto().getMype().getNombreComercial(),
                certificado.getCodigo(),
                certificado.getUrlCertificado()
        );

        log.info("[Certificado] Enviado por email - id={}, estudiante={}",
                certificadoId, certificado.getEstudiante().getUsuario().getEmail());
    }

    // ✅ Eliminar certificado (solo si no ha sido enviado)
    @Transactional
    public void eliminarCertificado(Long certificadoId, String emailMype) {
        Certificado certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado no encontrado", certificadoId));

        // Verificar que la MYPE sea la propietaria
        if (!certificado.getEmitidoPor().getEmail().equals(emailMype)) {
            throw new BusinessException("No tienes permiso para eliminar este certificado", HttpStatus.FORBIDDEN);
        }

        // Verificar que no haya sido enviado al estudiante
        if (certificado.getEnviadoEmail() != null && certificado.getEnviadoEmail()) {
            throw new BusinessException("No se puede eliminar un certificado que ya ha sido enviado al estudiante");
        }

        // Eliminar el archivo PDF de S3 si existe
        if (certificado.getUrlCertificado() != null && !certificado.getUrlCertificado().isBlank()) {
            try {
                // Usar el método de S3Service que ya maneja la extracción de la key
                s3Service.eliminarArchivo(certificado.getUrlCertificado());
                log.info("[Certificado] PDF eliminado de S3: {}", certificado.getUrlCertificado());
            } catch (Exception e) {
                log.warn("[Certificado] Error eliminando PDF de S3: {}", e.getMessage());
                // No lanzamos excepción para poder eliminar el registro de BD
            }
        }

        // Eliminar el certificado de la base de datos
        certificadoRepository.delete(certificado);
        log.info("[Certificado] Eliminado - id={}, codigo={}, estudiante={}",
                certificadoId, certificado.getCodigo(), certificado.getEstudiante().getUsuario().getNombre());
    }

    private CertificadoResponse mapToResponse(Certificado c) {
        return new CertificadoResponse(
                c.getId(),
                c.getCodigo(),
                c.getTituloCertificado(),
                c.getDescripcionCertificado(),
                c.getEstudiante().getUsuario().getNombre(),
                c.getEstudiante().getUsuario().getEmail(),
                c.getProyecto().getTitulo(),
                c.getProyecto().getMype().getNombreComercial(),
                c.getUrlCertificado(),
                c.getFechaEmision(),
                c.getEnviadoEmail() != null && c.getEnviadoEmail()
        );
    }
}