package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.application.service.AdminArbolDecisionService;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminArbolDecisionServiceTest {

    @Mock private NodoDecisionRepository nodoRepo;
    @Mock private OpcionDecisionRepository opcionRepo;
    @Mock private TipoProyectoRepository tipoProyectoRepo;

    @InjectMocks
    private AdminArbolDecisionService adminArbolDecisionService;

    private NodoDecision raiz;
    private NodoDecision nodo2;
    private OpcionDecision opcion;
    private TipoProyecto tp;

    @BeforeEach
    void setUp() {
        raiz = NodoDecision.builder().id(1L).codigo("P1").pregunta("¿Qué?").tieneInputLibre(false)
                .esRaiz(true).orden(1).activo(true).build();
        nodo2 = NodoDecision.builder().id(2L).codigo("P2").pregunta("¿Algo?").tieneInputLibre(false)
                .esRaiz(false).orden(2).activo(true).build();
        tp = TipoProyecto.builder().id(1L).codigo("WEB").nombre("Web App").activo(true).build();
        opcion = OpcionDecision.builder().id(1L).texto("Sí").orden(1)
                .nodo(nodo2).nodoDestino(nodo2).activo(true).build();
    }

    @Test
    void listarTodo_Success() {
        when(nodoRepo.findAll()).thenReturn(List.of(raiz, nodo2));
        when(opcionRepo.findByNodoIdWithTipoProyecto(1L)).thenReturn(List.of());
        when(opcionRepo.findByNodoIdWithTipoProyecto(2L)).thenReturn(List.of());

        AdminArbolResponse response = adminArbolDecisionService.listarTodo();

        assertEquals(2, response.nodos().size());
    }

    @Test
    void obtenerNodo_Success() {
        when(nodoRepo.findById(1L)).thenReturn(Optional.of(raiz));
        when(opcionRepo.findByNodoIdWithTipoProyecto(1L)).thenReturn(List.of());

        AdminNodoResponse response = adminArbolDecisionService.obtenerNodo(1L);

        assertEquals("P1", response.codigo());
        assertEquals("¿Qué?", response.pregunta());
    }

    @Test
    void obtenerNodo_ShouldFail_WhenNotFound() {
        when(nodoRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> adminArbolDecisionService.obtenerNodo(99L));
    }

    @Test
    void crearNodo_Success() {
        when(nodoRepo.findByCodigo("P3")).thenReturn(Optional.empty());
        when(nodoRepo.save(any(NodoDecision.class))).thenAnswer(i -> i.getArgument(0));
        when(opcionRepo.findByNodoIdWithTipoProyecto(any())).thenReturn(List.of());

        AdminNodoResponse response = adminArbolDecisionService.crearNodo(
                new NodoRequest("P3", "¿Nuevo?", false, null, false, 3, true));

        assertEquals("P3", response.codigo());
    }

    @Test
    void crearNodo_ShouldFail_WhenDuplicateCode() {
        when(nodoRepo.findByCodigo("P1")).thenReturn(Optional.of(raiz));
        assertThrows(BusinessException.class,
                () -> adminArbolDecisionService.crearNodo(
                        new NodoRequest("P1", "¿Dup?", false, null, false, 1, true)));
    }

    @Test
    void actualizarNodo_Success() {
        when(nodoRepo.findById(1L)).thenReturn(Optional.of(raiz));
        when(nodoRepo.save(any(NodoDecision.class))).thenAnswer(i -> i.getArgument(0));
        when(opcionRepo.findByNodoIdWithTipoProyecto(1L)).thenReturn(List.of());

        AdminNodoResponse response = adminArbolDecisionService.actualizarNodo(1L,
                new NodoRequest("P1", "¿Updated?", true, "placeholder", true, 1, true));

        assertEquals("¿Updated?", response.pregunta());
    }

    @Test
    void eliminarNodo_Success() {
        when(nodoRepo.findById(2L)).thenReturn(Optional.of(nodo2));
        when(nodoRepo.save(any(NodoDecision.class))).thenAnswer(i -> i.getArgument(0));

        adminArbolDecisionService.eliminarNodo(2L);

        assertFalse(nodo2.getActivo());
    }

    @Test
    void eliminarNodo_ShouldFail_WhenRoot() {
        when(nodoRepo.findById(1L)).thenReturn(Optional.of(raiz));
        assertThrows(BusinessException.class,
                () -> adminArbolDecisionService.eliminarNodo(1L));
    }

    @Test
    void crearOpcion_Success() {
        NodoDecision nodoDest = NodoDecision.builder().id(2L).codigo("P2").activo(true).build();
        when(nodoRepo.findById(1L)).thenReturn(Optional.of(raiz));
        when(nodoRepo.findById(2L)).thenReturn(Optional.of(nodoDest));
        when(nodoRepo.existsById(2L)).thenReturn(true);
        when(opcionRepo.save(any(OpcionDecision.class))).thenAnswer(i -> i.getArgument(0));

        AdminOpcionResponse response = adminArbolDecisionService.crearOpcion(1L,
                new OpcionRequest("Opción 1", 1, 2L, null, true));

        assertEquals("Opción 1", response.texto());
    }

    @Test
    void crearOpcion_ShouldFail_WhenBothDestinations() {
        when(nodoRepo.findById(1L)).thenReturn(Optional.of(raiz));
        assertThrows(BusinessException.class,
                () -> adminArbolDecisionService.crearOpcion(1L,
                        new OpcionRequest("Op", 1, 1L, 1L, true)));
    }

    @Test
    void actualizarOpcion_Success() {
        OpcionDecision op = OpcionDecision.builder().id(1L).texto("Old").orden(1).activo(true)
                .nodo(raiz).nodoDestino(null).tipoProyecto(tp).build();
        when(opcionRepo.findById(1L)).thenReturn(Optional.of(op));
        when(opcionRepo.save(any(OpcionDecision.class))).thenAnswer(i -> i.getArgument(0));
        when(tipoProyectoRepo.existsById(1L)).thenReturn(true);

        AdminOpcionResponse response = adminArbolDecisionService.actualizarOpcion(1L,
                new OpcionRequest("New Text", 2, null, 1L, true));

        assertEquals("New Text", response.texto());
        assertEquals(2, response.orden());
    }

    @Test
    void eliminarOpcion_Success() {
        when(opcionRepo.findById(1L)).thenReturn(Optional.of(opcion));
        when(opcionRepo.save(any(OpcionDecision.class))).thenAnswer(i -> i.getArgument(0));

        adminArbolDecisionService.eliminarOpcion(1L);

        assertFalse(opcion.getActivo());
    }

    @Test
    void validar_Success_NoErrors() {
        raiz.setEsRaiz(true);
        opcion.setNodo(raiz);
        opcion.setNodoDestino(null);
        opcion.setTipoProyecto(tp);
        when(nodoRepo.findAll()).thenReturn(List.of(raiz));
        when(opcionRepo.findAll()).thenReturn(List.of(opcion));

        ValidacionResponse response = adminArbolDecisionService.validar();

        assertTrue(response.errores().isEmpty());
    }

    @Test
    void validar_ShouldDetect_NoRoot() {
        raiz.setEsRaiz(false);
        when(nodoRepo.findAll()).thenReturn(List.of(raiz));
        when(opcionRepo.findAll()).thenReturn(List.of());

        ValidacionResponse response = adminArbolDecisionService.validar();

        assertFalse(response.errores().isEmpty());
        assertTrue(response.errores().stream().anyMatch(e -> e.tipo().equals("SIN_RAIZ")));
    }
}
