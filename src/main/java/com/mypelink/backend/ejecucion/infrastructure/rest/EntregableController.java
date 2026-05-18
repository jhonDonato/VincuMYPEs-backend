package com.mypelink.backend.ejecucion.infrastructure.rest;

import com.mypelink.backend.ejecucion.application.dto.EntregableRequest;
import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.dto.RevisarEntregableRequest;
import com.mypelink.backend.ejecucion.application.service.EntregableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/entregables")
@RequiredArgsConstructor
public class EntregableController {

    private final EntregableService entregableService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<EntregableResponse> subir(
            @PathVariable Long proyectoId,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregableService.subir(proyectoId, titulo, descripcion, archivo, userDetails.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<EntregableResponse>> listar(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.listarPorProyecto(proyectoId, userDetails.getUsername()));
    }

    @PatchMapping("/{entregableId}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<EntregableResponse> revisar(
            @PathVariable Long proyectoId,
            @PathVariable Long entregableId,
            @Valid @RequestBody RevisarEntregableRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.revisar(proyectoId, entregableId, request, userDetails.getUsername()));
    }

    @GetMapping("/mis-entregables")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<List<EntregableResponse>> misEntregables(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.misEntregables(userDetails.getUsername()));
    }
}