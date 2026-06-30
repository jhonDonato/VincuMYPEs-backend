package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.calificaciones.application.dto.CrearCalificacionRequest;
import com.mypelink.backend.calificaciones.application.service.CalificacionService;
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

import java.time.LocalDateTime;
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
    private final CalificacionService calificacionService;

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

            // Busca esta parte en tu CertificadoService.java (dentro del for loop)
            String codigo = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

// ✅ NUEVO: Procesar la firma - Subir a S3
            String firmaUrlS3 = null;
            if (request.firmaBase64() != null && !request.firmaBase64().isBlank()) {
                try {
                    // Limpiar el prefijo data:image/...;base64, si existe
                    String base64Limpio = request.firmaBase64();
                    if (base64Limpio.contains(",")) {
                        base64Limpio = base64Limpio.substring(base64Limpio.indexOf(",") + 1);
                    }

                    // Decodificar base64 a bytes
                    byte[] firmaBytes = java.util.Base64.getDecoder().decode(base64Limpio);

                    // Subir a S3 usando el método existente subirCertificado
                    String nombreArchivo = "firmas/" + codigo + "_firma.png";
                    firmaUrlS3 = s3Service.subirCertificado(firmaBytes, nombreArchivo);

                    log.info("✅ Firma subida a S3: {}", firmaUrlS3);
                } catch (Exception e) {
                    log.error("❌ Error al subir firma a S3: {}", e.getMessage());
                    // Si falla S3, guardamos null (no guardamos base64 en BD)
                    firmaUrlS3 = null;
                }
            }

// Crear el certificado con la URL de S3
            Certificado certificado = Certificado.builder()
                    .proyecto(proyecto)
                    .estudiante(postulacion.getEstudiante())
                    .codigo(codigo)
                    .tituloCertificado(request.tituloCertificado())
                    .descripcionCertificado(request.descripcionCertificado())
                    .gerenteNombre(request.gerenteNombre())
                    .cargoRepresentante(request.cargoRepresentante())
                    .firmaUrl(firmaUrlS3) // ✅ Guarda la URL de S3 (corta)
                    .emitidoPor(usuarioMype)
                    .build();

            Certificado guardado = certificadoRepository.save(certificado);



            log.debug("[Certificado] Emitido - estudianteId={}, proyectoId={}, titulo={}",
                    postulacion.getEstudiante().getId(),
                    proyecto.getId(),
                    request.tituloCertificado());

            Certificado full = certificadoRepository.findById(guardado.getId())
                    .orElseThrow(() -> new RuntimeException("Certificado no encontrado después de guardar"));

            byte[] pdfBytes = pdfGeneratorService.generarCertificadoPDF(
                    full, proyecto, proyecto.getMype(),
                    firmaUrlS3, request.gerenteNombre());  // ✅ USA LA VARIABLE firmaUrlS3

            String pdfUrl = s3Service.subirCertificado(pdfBytes, "certificados/" + codigo + ".pdf");
            full.setUrlCertificado(pdfUrl);
            certificadoRepository.save(full);

            responses.add(mapToResponse(full));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarMisCertificados(String emailEstudiante) {
        return certificadoRepository.findByEstudianteUsuarioEmail(emailEstudiante)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarCertificadosEmitidos(String emailMype) {
        return certificadoRepository.findByProyectoMypeUsuarioEmail(emailMype)
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
                        c.getFechaEmision(),
                        c.getFechaEnvio()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void enviarCertificado(Long certificadoId, String emailMype, String pdfBase64, Integer calificacion) {
        Certificado certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado no encontrado", certificadoId));

        if (!certificado.getEmitidoPor().getEmail().equals(emailMype)) {
            throw new BusinessException("No tienes permiso para enviar este certificado", HttpStatus.FORBIDDEN);
        }

        if (certificado.getFechaEnvio() != null) {
            throw new BusinessException("Este certificado ya ha sido enviado al estudiante");
        }

        // Validar que la calificación esté presente y sea válida
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new BusinessException("La calificación debe estar entre 1 y 5", HttpStatus.BAD_REQUEST);
        }

        // Guardar la calificación (MYPE calificando ESTUDIANTE)
        CrearCalificacionRequest calificacionRequest = new CrearCalificacionRequest(
                certificado.getProyecto().getId(),
                certificado.getEstudiante().getUsuario().getId(),
                calificacion
        );
        
        try {
            calificacionService.crear(calificacionRequest, emailMype);
            log.info("✅ Calificación guardada - Certificado: {}, Calificación: {}", certificadoId, calificacion);
        } catch (Exception e) {
            log.error("❌ Error al guardar calificación para certificado {}: {}", certificadoId, e.getMessage());
            throw new BusinessException("Error al guardar la calificación: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Actualizar el certificado
        if (pdfBase64 != null && !pdfBase64.isBlank()) {
            certificado.setPdfBase64(pdfBase64);
        }
        certificado.setFechaEnvio(LocalDateTime.now());
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
        
        log.info("✅ Certificado enviado - ID: {}, Estudiante: {}", certificadoId, certificado.getEstudiante().getUsuario().getEmail());
    }

    @Transactional
    public void eliminarCertificado(Long certificadoId, String emailMype) {
        Certificado certificado = certificadoRepository.findById(certificadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado no encontrado", certificadoId));

        if (!certificado.getEmitidoPor().getEmail().equals(emailMype)) {
            throw new BusinessException("No tienes permiso para eliminar este certificado", HttpStatus.FORBIDDEN);
        }

        if (certificado.getFechaEnvio() != null) {
            throw new BusinessException("No se puede eliminar un certificado que ya fue enviado al estudiante");
        }

        if (certificado.getUrlCertificado() != null && !certificado.getUrlCertificado().isEmpty()) {
            try {
                s3Service.eliminarArchivo(certificado.getUrlCertificado());
            } catch (Exception e) {
                log.warn("No se pudo eliminar el archivo S3 del certificado {}: {}", certificadoId, e.getMessage());
            }
        }

        certificadoRepository.delete(certificado);
    }

    private CertificadoResponse mapToResponse(Certificado c) {
        return new CertificadoResponse(
                c.getId(),
                c.getCodigo(),
                c.getTituloCertificado(),
                c.getDescripcionCertificado(),
                c.getEstudiante().getUsuario().getNombre(),
                c.getEstudiante().getUsuario().getEmail(),
                c.getEstudiante().getUsuario().getId(),
                c.getProyecto().getTitulo(),
                c.getProyecto().getId(),
                c.getProyecto().getMype().getNombreComercial(),
                c.getProyecto().getMype().getUsuario().getId(),
                c.getUrlCertificado(),
                c.getFechaEmision(),
                c.getFechaEnvio(),
                // ✅ NUEVOS CAMPOS
                c.getGerenteNombre(),
                c.getCargoRepresentante(),
                c.getFirmaUrl(),
                c.getProyecto().getMype().getNombreRepresentante(),
                c.getProyecto().getMype().getRuc(),
                c.getPdfBase64()
        );
    }
}
