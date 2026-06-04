package com.mypelink.backend.certificaciones.infrastructure.rest;

import com.mypelink.backend.certificaciones.application.dto.CertificadoAdminResponse;
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
            @AuthenticationPrincipal UserDetails userDetails) {
        certificadoService.enviarCertificado(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
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
