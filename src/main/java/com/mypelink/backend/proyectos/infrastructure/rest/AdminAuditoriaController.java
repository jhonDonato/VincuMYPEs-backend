package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.AdminAuditoriaResponse;
import com.mypelink.backend.proyectos.domain.repository.WorkflowHistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAuditoriaController {

    private final WorkflowHistorialRepository workflowHistorialRepository;

    @GetMapping
    public ResponseEntity<List<AdminAuditoriaResponse>> listarLogs() {
        List<AdminAuditoriaResponse> logs = workflowHistorialRepository.findAllWithDetails().stream().map(w -> {
            String id = "LOG-" + String.format("%03d", w.getId());
            String proyecto = w.getProyecto().getTitulo();
            String actor = w.getCambiadoPor().getNombre();
            
            String rolCompleto = w.getCambiadoPor().getRol().getNombre();
            String rolActor = rolCompleto.replace("ROLE_", "");

            String estadoAnterior = w.getEstadoAnterior() != null ? w.getEstadoAnterior().name() : "NUEVO";
            String estadoNuevo = w.getEstadoNuevo().name();
            
            return new AdminAuditoriaResponse(
                    id,
                    proyecto,
                    actor,
                    rolActor,
                    estadoAnterior,
                    estadoNuevo,
                    w.getFechaCambio(),
                    w.getComentario()
            );
        }).toList();

        return ResponseEntity.ok(logs);
    }
}
