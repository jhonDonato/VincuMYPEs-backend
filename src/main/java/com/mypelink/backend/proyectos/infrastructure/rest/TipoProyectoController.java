package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.TipoProyectoResponse;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-proyecto")
@RequiredArgsConstructor
public class TipoProyectoController {

    private final TipoProyectoRepository tipoProyectoRepository;

    @GetMapping
    public ResponseEntity<List<TipoProyectoResponse>> listar() {
        return ResponseEntity.ok(
                tipoProyectoRepository.findByActivoTrue()
                        .stream()
                        .map(t -> new TipoProyectoResponse(
                                t.getId(), t.getCodigo(), t.getNombre(),
                                t.getDescripcionMype(), t.getDescripcionEstudiante(),
                                t.getRama(), t.getCicloMinimo(),
                                t.getIncluyePresupuesto(), t.getIncluyeCronograma()))
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoProyectoResponse> obtener(@PathVariable Long id) {
        return tipoProyectoRepository.findById(id)
                .map(t -> ResponseEntity.ok(new TipoProyectoResponse(
                        t.getId(), t.getCodigo(), t.getNombre(),
                        t.getDescripcionMype(), t.getDescripcionEstudiante(),
                        t.getRama(), t.getCicloMinimo(),
                        t.getIncluyePresupuesto(), t.getIncluyeCronograma())))
                .orElse(ResponseEntity.notFound().build());
    }
}