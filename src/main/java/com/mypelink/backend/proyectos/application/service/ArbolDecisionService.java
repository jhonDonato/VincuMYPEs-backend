package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArbolDecisionService {

    private final NodoDecisionRepository nodoRepo;
    private final OpcionDecisionRepository opcionRepo;
    private final EntregableTipoRepository entregableTipoRepo;

    @Transactional(readOnly = true)
    public ArbolDecisionResponse obtenerArbolActivo(String plazo) {
        List<NodoDecision> nodos = nodoRepo.findByActivoTrueOrderByOrdenAsc();
        NodoDecision raiz = nodos.stream().filter(NodoDecision::getEsRaiz).findFirst()
                .orElseThrow(() -> new RuntimeException("No hay nodo raíz"));

        Map<String, NodoDto> nodosMap = new HashMap<>();
        Map<String, ResultadoDto> resultadosMap = new HashMap<>();

        for (NodoDecision n : nodos) {
            List<OpcionDecision> opciones = opcionRepo.findByNodoIdWithTipoProyecto(n.getId());
            List<OpcionDto> opcionesDto = opciones.stream().map(o -> {
                String siguiente = o.getNodoDestino() != null ? o.getNodoDestino().getCodigo() : null;
                String resultado = o.getTipoProyecto() != null ? o.getTipoProyecto().getCodigo() : null;
                return new OpcionDto(o.getTexto(), siguiente, resultado);
            }).toList();
            nodosMap.put(n.getCodigo(), new NodoDto(n.getPregunta(), n.getTieneInputLibre(), n.getInputPlaceholder(), opcionesDto));

            // Para opciones hoja, cargar el resultado
            for (OpcionDecision o : opciones) {
                if (o.getTipoProyecto() != null) {
                    TipoProyecto tp = o.getTipoProyecto();
                    if (!resultadosMap.containsKey(tp.getCodigo())) {
                        List<EntregableTipo> entregables = entregableTipoRepo.findByTipoProyectoIdOrderByOrdenAsc(tp.getId());
                        List<EntregableTipoResponse> entregablesDto = entregables.stream()
                                .map(e -> new EntregableTipoResponse(e.getId(), tp.getId(), e.getTitulo(), e.getDescripcion(), e.getOrden()))
                                .toList();
                        resultadosMap.put(tp.getCodigo(), new ResultadoDto(
                                tp.getCodigo(), tp.getNombre(),
                                tp.getAreaSistemas() != null ? tp.getAreaSistemas().name() : null,
                                tp.getCuposMin(), tp.getCuposMax(),
                                tp.getDiasMin(), tp.getDiasSugerido(),
                                tp.getDescripcionMype(),
                                entregablesDto,
                                tp.getId()
                        ));
                    }
                }
            }
        }

        return new ArbolDecisionResponse(raiz.getCodigo(), nodosMap, resultadosMap);
    }
}