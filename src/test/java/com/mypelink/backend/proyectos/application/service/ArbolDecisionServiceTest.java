package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.ArbolDecisionResponse;
import com.mypelink.backend.proyectos.application.service.ArbolDecisionService;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArbolDecisionServiceTest {

    @Mock private NodoDecisionRepository nodoRepo;
    @Mock private OpcionDecisionRepository opcionRepo;
    @Mock private EntregableTipoRepository entregableTipoRepo;

    @InjectMocks
    private ArbolDecisionService arbolDecisionService;

    @Test
    void obtenerArbolActivo_Success() {
        NodoDecision raiz = NodoDecision.builder()
                .id(1L).codigo("P1").pregunta("¿Qué deseas hacer?")
                .esRaiz(true).activo(true).orden(1).build();
        NodoDecision nodo2 = NodoDecision.builder()
                .id(2L).codigo("P2").pregunta("¿Algo más?")
                .esRaiz(false).activo(true).orden(2).build();
        TipoProyecto tp = TipoProyecto.builder().id(1L).codigo("WEB").nombre("Web App")
                .activo(true).build();

        when(nodoRepo.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of(raiz, nodo2));

        when(opcionRepo.findByNodoIdWithTipoProyecto(1L)).thenReturn(List.of(
                OpcionDecision.builder().id(1L).texto("Sí").orden(1)
                        .nodoDestino(nodo2).build()
        ));
        when(opcionRepo.findByNodoIdWithTipoProyecto(2L)).thenReturn(List.of(
                OpcionDecision.builder().id(2L).texto("Web").orden(1)
                        .tipoProyecto(tp).build()
        ));

        when(entregableTipoRepo.findByTipoProyectoIdOrderByOrdenAsc(1L)).thenReturn(List.of(
                EntregableTipo.builder().id(1L).titulo("Repo").descripcion("Repo").orden(1).build()
        ));

        ArbolDecisionResponse response = arbolDecisionService.obtenerArbolActivo("corto");

        assertEquals("P1", response.nodoRaizCodigo());
        assertEquals(2, response.nodos().size());
        assertTrue(response.nodos().containsKey("P1"));
        assertTrue(response.nodos().containsKey("P2"));
        assertEquals(1, response.resultados().size());
        assertTrue(response.resultados().containsKey("WEB"));
    }

    @Test
    void obtenerArbolActivo_ShouldFail_WhenNoRoot() {
        when(nodoRepo.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> arbolDecisionService.obtenerArbolActivo("largo"));
    }
}
