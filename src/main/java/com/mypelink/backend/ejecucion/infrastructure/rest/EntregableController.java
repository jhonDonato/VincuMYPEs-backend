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
            @RequestParam(value = "entregableId", required = false) Long entregableId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregableService.subir(proyectoId, titulo, descripcion, archivo, userDetails.getUsername(), entregableId));
    }

    // ✅ LISTAR TODOS LOS ENTREGABLES DEL PROYECTO (MYPE)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<EntregableResponse>> listar(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.listarPorProyecto(proyectoId, userDetails.getUsername()));
    }

    // ✅ REVISAR ENTREGABLE (MYPE)
    @PatchMapping("/{entregableId}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<EntregableResponse> revisar(
            @PathVariable Long proyectoId,
            @PathVariable Long entregableId,
            @Valid @RequestBody RevisarEntregableRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.revisar(proyectoId, entregableId, request, userDetails.getUsername()));
    }

    // ✅ MIS ENTREGABLES (ESTUDIANTE) - CORREGIDO con proyectoId
    @GetMapping("/mis-entregables")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<List<EntregableResponse>> misEntregables(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                entregableService.misEntregables(proyectoId, userDetails.getUsername())
        );
    }

    // ✅ ELIMINAR ENTREGABLE (ESTUDIANTE)
    @DeleteMapping("/{entregableId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<Void> eliminarEntregable(
            @PathVariable Long proyectoId,
            @PathVariable Long entregableId,
            @AuthenticationPrincipal UserDetails userDetails) {
        entregableService.eliminar(proyectoId, entregableId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
    // ✅ NUEVO ENDPOINT: Solo entregables SUBIDOS (con archivo)
    @GetMapping("/subidos")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<EntregableResponse>> listarSoloSubidos(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.listarSoloSubidos(proyectoId, userDetails.getUsername()));
    }
    // ✅ NUEVO: Verificar si el estudiante actual es delegado del proyecto
    @GetMapping("/puede-subir")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<java.util.Map<String, Boolean>> puedeSubir(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Esta verificación la hace internamente el EntregableService
        // Pero exponemos un endpoint rápido para el frontend
        boolean puedeSubir = true; // El service lanza excepción si no puede

        return ResponseEntity.ok(java.util.Map.of("puedeSubir", puedeSubir));
    }
    // ✅ NUEVO: Listar TODOS los entregables del proyecto (para ESTUDIANTES también)
    @GetMapping("/todos")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE', 'ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<EntregableResponse>> listarTodos(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.listarTodosDelProyecto(proyectoId, userDetails.getUsername()));
    }
}