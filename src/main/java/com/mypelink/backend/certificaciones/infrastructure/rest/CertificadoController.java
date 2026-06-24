package com.mypelink.backend.certificaciones.infrastructure.rest;

import com.mypelink.backend.certificaciones.application.dto.CertificadoAdminResponse;
import com.mypelink.backend.certificaciones.application.dto.CertificadoResponse;
import com.mypelink.backend.certificaciones.application.dto.EmitirCertificadoRequest;
import com.mypelink.backend.certificaciones.application.dto.EnviarConPdfRequest;
import com.mypelink.backend.certificaciones.application.service.CertificadoService;
import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;
    private final CertificadoRepository certificadoRepository;
    private final S3Service s3Service;
    private final JwtService jwtService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<CertificadoResponse>> emitir(
            @Valid @RequestBody EmitirCertificadoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificadoService.emitirCertificados(userDetails.getUsername(), request));
    }

    @GetMapping("/admin/todos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CertificadoAdminResponse>> listarTodos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificadoService.listarTodosCertificados());
    }

    @GetMapping("/mis-certificados")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<List<CertificadoResponse>> misCertificados(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificadoService.listarMisCertificados(userDetails.getUsername()));
    }

    // ✅ ENDPOINT CON TOKEN POR QUERY PARAM
    @GetMapping("/{id}/firma")
    public ResponseEntity<byte[]> obtenerFirmaCertificado(
            @PathVariable Long id,
            @RequestParam(required = false) String token) {

        log.info("🔍 FIRMA: Recibida petición para certificado ID: {}", id);

        try {
            // Validar token
            if (token == null || token.isBlank()) {
                log.warn("❌ FIRMA: Token no proporcionado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email;
            try {
                email = jwtService.extractUsername(token);
                if (email == null || email.isBlank()) {
                    throw new RuntimeException("Email vacío en token");
                }
            } catch (Exception e) {
                log.warn("❌ FIRMA: Token inválido: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("👤 FIRMA: Usuario autenticado: {}", email);

            // Buscar certificado
            Certificado certificado = certificadoRepository.findByIdWithRelations(id).orElse(null);
            if (certificado == null) {
                log.warn("❌ FIRMA: Certificado no encontrado");
                return ResponseEntity.notFound().build();
            }

            // Verificar permisos
            boolean esMype = false;
            boolean esEstudiante = false;

            try {
                if (certificado.getEmitidoPor() != null) {
                    esMype = certificado.getEmitidoPor().getEmail().equals(email);
                }
            } catch (Exception e) {
                log.warn("No se pudo verificar MYPE: {}", e.getMessage());
            }

            try {
                if (certificado.getEstudiante() != null && certificado.getEstudiante().getUsuario() != null) {
                    esEstudiante = certificado.getEstudiante().getUsuario().getEmail().equals(email);
                }
            } catch (Exception e) {
                log.warn("No se pudo verificar estudiante: {}", e.getMessage());
            }

            if (!esMype && !esEstudiante) {
                log.warn("⛔ FIRMA: Usuario {} sin permisos para certificado {}", email, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (certificado.getFirmaUrl() == null || certificado.getFirmaUrl().isBlank()) {
                log.warn("⚠️ FIRMA: Sin firma");
                return ResponseEntity.notFound().build();
            }

            log.info("📸 FIRMA URL: {}", certificado.getFirmaUrl());

            String key = s3Service.extraerKeyDeUrl(certificado.getFirmaUrl());
            byte[] firmaBytes = s3Service.descargarArchivo(key);

            log.info("✅ FIRMA: Descargada {} bytes", firmaBytes.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).getHeaderValue());
            headers.setContentLength(firmaBytes.length);

            return new ResponseEntity<>(firmaBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ FIRMA ERROR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/emitidos")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<CertificadoResponse>> emitidos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificadoService.listarCertificadosEmitidos(userDetails.getUsername()));
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<Void> enviar(
            @PathVariable Long id,
            @RequestBody(required = false) EnviarConPdfRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String pdfBase64 = request != null ? request.pdfBase64() : null;
        certificadoService.enviarCertificado(id, userDetails.getUsername(), pdfBase64);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        certificadoService.eliminarCertificado(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}