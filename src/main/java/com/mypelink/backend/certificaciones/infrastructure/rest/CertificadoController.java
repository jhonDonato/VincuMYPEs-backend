package com.mypelink.backend.certificaciones.infrastructure.rest;

import com.mypelink.backend.certificaciones.application.dto.CertificadoResponse;
import com.mypelink.backend.certificaciones.application.dto.EmitirCertificadoRequest;
import com.mypelink.backend.certificaciones.application.service.CertificadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    @PostMapping
    @PreAuthorize("hasRole('MYPE')")
    public ResponseEntity<CertificadoResponse> emitir(
            @Valid @RequestBody EmitirCertificadoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificadoService.emitirCertificado(userDetails.getUsername(), request));
    }

    @GetMapping("/mis-certificados")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<CertificadoResponse>> misCertificados(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificadoService.listarMisCertificados(userDetails.getUsername()));
    }

    @GetMapping("/emitidos")
    @PreAuthorize("hasRole('MYPE')")
    public ResponseEntity<List<CertificadoResponse>> emitidos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificadoService.listarCertificadosEmitidos(userDetails.getUsername()));
    }
}
