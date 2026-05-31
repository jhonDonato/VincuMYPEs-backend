package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.domain.model.EntregableTipo;
import com.mypelink.backend.proyectos.domain.model.InsumoTipo;
import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.EntregableTipoRepository;
import com.mypelink.backend.proyectos.domain.repository.InsumoProyectoRepository;
import com.mypelink.backend.proyectos.domain.repository.InsumoTipoRepository;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-proyecto")
@RequiredArgsConstructor
public class TipoProyectoController {

    private final TipoProyectoRepository tipoProyectoRepository;
    private final EntregableTipoRepository entregableTipoRepository;
    private final InsumoTipoRepository insumoTipoRepository;
    private final InsumoProyectoRepository insumoProyectoRepository;


    /**
     * Lista tipos de proyecto.
     * Por defecto solo devuelve los activos (caso MYPE).
     * Si `incluirInactivos=true`, devuelve todos (caso admin).
     */
    @GetMapping
    public ResponseEntity<List<TipoProyectoResponse>> listar(
            @RequestParam(name = "incluirInactivos", defaultValue = "false") boolean incluirInactivos) {

        List<TipoProyecto> tipos = incluirInactivos
                ? tipoProyectoRepository.findAll()
                : tipoProyectoRepository.findByActivoTrue();

        return ResponseEntity.ok(
                tipos.stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoProyectoResponse> obtener(@PathVariable Long id) {
        return tipoProyectoRepository.findById(id)
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TipoProyectoResponse> crear(@Valid @RequestBody TipoProyectoRequest request) {
        TipoProyecto tipo = TipoProyecto.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .descripcionMype(request.descripcionMype())
                .descripcionEstudiante(request.descripcionEstudiante())
                .rama(request.rama())
                .cicloMinimo(request.cicloMinimo())
                .incluyePresupuesto(false)
                .areaSistemas(request.areaSistemas() != null ? AreaSistemas.valueOf(request.areaSistemas()) : null)
                .cuposMin(request.cuposMin())
                .cuposMax(request.cuposMax())
                .diasMin(request.diasMin())
                .diasSugerido(request.diasSugerido())
                // ▼ Antes faltaban estos cuatro; el front los enviaba pero el back los ignoraba en create.
                .complejidad(request.complejidad())
                .esfuerzoHPers(request.esfuerzoHPers())
                .alcanceIncluye(request.alcanceIncluye())
                .alcanceNoIncluye(request.alcanceNoIncluye())
                // ▲
                .activo(true)
                .build();
        tipo = tipoProyectoRepository.save(tipo);
        return ResponseEntity.ok(toResponse(tipo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TipoProyectoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody TipoProyectoRequest request) {
        TipoProyecto tipo = tipoProyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoProyecto", id));
        tipo.setCodigo(request.codigo());
        tipo.setNombre(request.nombre());
        tipo.setDescripcionMype(request.descripcionMype());
        tipo.setDescripcionEstudiante(request.descripcionEstudiante());
        tipo.setRama(request.rama());
        tipo.setCicloMinimo(request.cicloMinimo());
        tipo.setIncluyePresupuesto(false);
        tipo.setAreaSistemas(request.areaSistemas() != null ? AreaSistemas.valueOf(request.areaSistemas()) : null);
        tipo.setCuposMin(request.cuposMin());
        tipo.setCuposMax(request.cuposMax());
        tipo.setDiasMin(request.diasMin());
        tipo.setDiasSugerido(request.diasSugerido());
        tipo.setComplejidad(request.complejidad());
        // ▼ Antes faltaban estos dos; el front los enviaba pero el back los ignoraba en update.
        tipo.setEsfuerzoHPers(request.esfuerzoHPers());
        tipo.setAlcanceIncluye(request.alcanceIncluye());
        tipo.setAlcanceNoIncluye(request.alcanceNoIncluye());
        // ▲
        tipo = tipoProyectoRepository.save(tipo);
        return ResponseEntity.ok(toResponse(tipo));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TipoProyectoResponse> toggleActivo(@PathVariable Long id) {
        TipoProyecto tipo = tipoProyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoProyecto", id));
        tipo.setActivo(!tipo.getActivo());
        tipo = tipoProyectoRepository.save(tipo);
        return ResponseEntity.ok(toResponse(tipo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoProyectoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mapper centralizado. Antes había tres copias inline del mismo mapping
     * (en listar, obtener, y toResponse). Ahora todo pasa por este método —
     * cualquier cambio de DTO se hace en un solo lugar.
     */
    private TipoProyectoResponse toResponse(TipoProyecto t) {
        return new TipoProyectoResponse(
                t.getId(),
                t.getCodigo(),
                t.getNombre(),
                t.getDescripcionMype(),
                t.getDescripcionEstudiante(),
                t.getRama(),
                t.getCicloMinimo(),
                t.getAreaSistemas() != null ? t.getAreaSistemas().name() : null,
                t.getCuposMin(),
                t.getCuposMax(),
                t.getDiasMin(),
                t.getDiasSugerido(),
                t.getComplejidad(),
                t.getEsfuerzoHPers(),
                t.getAlcanceIncluye(),
                t.getAlcanceNoIncluye(),
                t.getActivo()
        );
    }


    // ═══════════════════════════════════════════════════════════════
    // ENTREGABLES (sin cambios respecto al original)
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{id}/entregables")
    public List<EntregableTipoResponse> listarEntregables(@PathVariable Long id) {
        return entregableTipoRepository.findByTipoProyectoIdOrderByOrdenAsc(id)
                .stream()
                .map(e -> new EntregableTipoResponse(e.getId(), e.getTipoProyecto().getId(),
                        e.getTitulo(), e.getDescripcion(), e.getOrden()))
                .toList();
    }

    @PostMapping("/{id}/entregables")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public EntregableTipoResponse crearEntregable(@PathVariable Long id,
                                                  @Valid @RequestBody EntregableTipoRequest request) {
        TipoProyecto tipo = tipoProyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoProyecto", id));
        EntregableTipo e = entregableTipoRepository.save(EntregableTipo.builder()
                .tipoProyecto(tipo)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .orden(request.orden() != null ? request.orden() : 0)
                .build());
        return new EntregableTipoResponse(e.getId(), id, e.getTitulo(), e.getDescripcion(), e.getOrden());
    }

    @PutMapping("/{id}/entregables/{entregableId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public EntregableTipoResponse actualizarEntregable(@PathVariable Long id,
                                                       @PathVariable Long entregableId,
                                                       @Valid @RequestBody EntregableTipoRequest request) {
        EntregableTipo e = entregableTipoRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("EntregableTipo", entregableId));
        e.setTitulo(request.titulo());
        e.setDescripcion(request.descripcion());
        if (request.orden() != null) e.setOrden(request.orden());
        e = entregableTipoRepository.save(e);
        return new EntregableTipoResponse(e.getId(), id, e.getTitulo(), e.getDescripcion(), e.getOrden());
    }

    @DeleteMapping("/{id}/entregables/{entregableId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarEntregable(@PathVariable Long id,
                                                   @PathVariable Long entregableId) {
        entregableTipoRepository.deleteById(entregableId);
        return ResponseEntity.noContent().build();
    }


    // ═══════════════════════════════════════════════════════════════
    // INSUMOS (sin cambios respecto al original)
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/{id}/insumos")
    public List<InsumoTipoResponse> listarInsumos(@PathVariable Long id) {
        return insumoTipoRepository.findByTipoProyectoIdOrderByOrdenAsc(id)
                .stream()
                .map(i -> new InsumoTipoResponse(i.getId(), i.getTipoProyecto().getId(),
                        i.getNombre(), i.getDescripcion(), i.getFormato(), i.getObligatorio(), i.getOrden()))
                .toList();
    }

    @PostMapping("/{id}/insumos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public InsumoTipoResponse crearInsumo(@PathVariable Long id,
                                          @Valid @RequestBody InsumoTipoRequest request) {
        TipoProyecto tipo = tipoProyectoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoProyecto", id));
        InsumoTipo i = insumoTipoRepository.save(InsumoTipo.builder()
                .tipoProyecto(tipo)
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .formato(request.formato())
                .obligatorio(request.obligatorio() != null ? request.obligatorio() : false)
                .orden(request.orden() != null ? request.orden() : 0)
                .build());
        return new InsumoTipoResponse(i.getId(), id, i.getNombre(), i.getDescripcion(),
                i.getFormato(), i.getObligatorio(), i.getOrden());
    }

    @PutMapping("/{id}/insumos/{insumoId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public InsumoTipoResponse actualizarInsumo(@PathVariable Long id,
                                               @PathVariable Long insumoId,
                                               @Valid @RequestBody InsumoTipoRequest request) {
        InsumoTipo i = insumoTipoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("InsumoTipo", insumoId));
        i.setNombre(request.nombre());
        i.setDescripcion(request.descripcion());
        i.setFormato(request.formato());
        i.setObligatorio(request.obligatorio() != null ? request.obligatorio() : false);
        if (request.orden() != null) i.setOrden(request.orden());
        i = insumoTipoRepository.save(i);
        return new InsumoTipoResponse(i.getId(), id, i.getNombre(), i.getDescripcion(),
                i.getFormato(), i.getObligatorio(), i.getOrden());
    }

    @DeleteMapping("/{id}/insumos/{insumoId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarInsumo(@PathVariable Long id,
                                               @PathVariable Long insumoId) {
        insumoTipoRepository.deleteById(insumoId);
        return ResponseEntity.noContent().build();
    }
}