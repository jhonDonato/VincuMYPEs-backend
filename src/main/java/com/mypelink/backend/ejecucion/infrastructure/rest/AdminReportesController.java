package com.mypelink.backend.ejecucion.infrastructure.rest;

import com.mypelink.backend.ejecucion.application.dto.AdminReporteResponse;
import com.mypelink.backend.ejecucion.application.service.AdminReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReportesController {

    private final AdminReportesService adminReportesService;

    @GetMapping
    public ResponseEntity<AdminReporteResponse> obtenerReportes() {
        return ResponseEntity.ok(adminReportesService.obtenerReportesYStats());
    }
}
