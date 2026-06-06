package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.PostulacionAdminResponse;
import com.mypelink.backend.proyectos.application.service.PostulacionAdminService;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/postulaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPostulacionController {

    private final PostulacionAdminService service;

    @GetMapping
    public ResponseEntity<Page<PostulacionAdminResponse>> buscar(
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) List<EstadoPostulacion> estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estudiante,
            @RequestParam(required = false) String mype,
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPostulacion,desc") String sort) {

        return ResponseEntity.ok(
                service.buscar(proyectoId, estado, fechaDesde, fechaHasta,
                        estudiante, mype, area, page, size, sort));
    }
}
