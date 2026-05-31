package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminArbolDecisionService {

    private final NodoDecisionRepository nodoRepo;
    private final OpcionDecisionRepository opcionRepo;
    private final TipoProyectoRepository tipoProyectoRepo;

    // ══════════════════════════════════════════════════════════════
    // GET /admin/arbol → todos los nodos (activos e inactivos)
    // ══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public AdminArbolResponse listarTodo() {
        List<NodoDecision> nodos = nodoRepo.findAll();
        List<AdminNodoResponse> nodosResponse = new ArrayList<>();
        for (NodoDecision nodo : nodos) {
            nodosResponse.add(toAdminNodoResponse(nodo));
        }
        return new AdminArbolResponse(nodosResponse);
    }

    // ══════════════════════════════════════════════════════════════
    // GET /admin/arbol/nodos/{id}
    // ══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public AdminNodoResponse obtenerNodo(Long id) {
        NodoDecision nodo = nodoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NodoDecision", id));
        return toAdminNodoResponse(nodo);
    }

    // ══════════════════════════════════════════════════════════════
    // POST /admin/arbol/nodos
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public AdminNodoResponse crearNodo(NodoRequest request) {
        // Validar código único
        if (nodoRepo.findByCodigo(request.codigo()).isPresent()) {
            throw new BusinessException("Ya existe un nodo con el código " + request.codigo(), HttpStatus.CONFLICT);
        }
        // Validar conflicto de raíz
        if (Boolean.TRUE.equals(request.esRaiz())) {
            validarConflictoRaiz(null);
        }
        NodoDecision nodo = NodoDecision.builder()
                .codigo(request.codigo())
                .pregunta(request.pregunta())
                .tieneInputLibre(request.tieneInputLibre())
                .inputPlaceholder(request.inputPlaceholder())
                .esRaiz(request.esRaiz())
                .orden(request.orden())
                .activo(request.activo())
                .build();
        try {
            nodo = nodoRepo.save(nodo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Ya existe un nodo con el código " + request.codigo(), HttpStatus.CONFLICT);
        }
        return toAdminNodoResponse(nodo);
    }

    // ══════════════════════════════════════════════════════════════
    // PUT /admin/arbol/nodos/{id}
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public AdminNodoResponse actualizarNodo(Long id, NodoRequest request) {
        NodoDecision nodo = nodoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NodoDecision", id));
        // Validar código único (si cambió)
        if (!nodo.getCodigo().equals(request.codigo()) && nodoRepo.findByCodigo(request.codigo()).isPresent()) {
            throw new BusinessException("Ya existe un nodo con el código " + request.codigo(), HttpStatus.CONFLICT);
        }
        // Validar conflicto de raíz
        if (Boolean.TRUE.equals(request.esRaiz())) {
            validarConflictoRaiz(id);
        }
        nodo.setCodigo(request.codigo());
        nodo.setPregunta(request.pregunta());
        nodo.setTieneInputLibre(request.tieneInputLibre());
        nodo.setInputPlaceholder(request.inputPlaceholder());
        nodo.setEsRaiz(request.esRaiz());
        nodo.setOrden(request.orden());
        nodo.setActivo(request.activo());
        try {
            nodo = nodoRepo.save(nodo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Ya existe un nodo con el código " + request.codigo(), HttpStatus.CONFLICT);
        }
        return toAdminNodoResponse(nodo);
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /admin/arbol/nodos/{id} → soft delete
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void eliminarNodo(Long id) {
        NodoDecision nodo = nodoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NodoDecision", id));
        if (Boolean.TRUE.equals(nodo.getEsRaiz())) {
            throw new BusinessException("No se puede desactivar el nodo raíz. Primero cambia el nodo raíz desde editar.", HttpStatus.BAD_REQUEST);
        }
        nodo.setActivo(false);
        nodoRepo.save(nodo);
    }

    // ══════════════════════════════════════════════════════════════
    // POST /admin/arbol/nodos/{nodoId}/opciones
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public AdminOpcionResponse crearOpcion(Long nodoId, OpcionRequest request) {
        NodoDecision nodo = nodoRepo.findById(nodoId)
                .orElseThrow(() -> new ResourceNotFoundException("NodoDecision", nodoId));
        validarXorDestinos(request.nodoDestinoId(), request.tipoProyectoId());
        validarFkDestino(request.nodoDestinoId(), request.tipoProyectoId());
        if (request.nodoDestinoId() != null) {
            validarCiclo(nodoId, request.nodoDestinoId(), null);
        }
        NodoDecision nodoDestino = request.nodoDestinoId() != null ? nodoRepo.findById(request.nodoDestinoId()).orElse(null) : null;
        TipoProyecto tipoProyecto = request.tipoProyectoId() != null ? tipoProyectoRepo.findById(request.tipoProyectoId()).orElse(null) : null;
        OpcionDecision opcion = OpcionDecision.builder()
                .nodo(nodo)
                .texto(request.texto())
                .orden(request.orden())
                .nodoDestino(nodoDestino)
                .tipoProyecto(tipoProyecto)
                .activo(request.activo())
                .build();
        opcion = opcionRepo.save(opcion);
        return toAdminOpcionResponse(opcion);
    }

    // ══════════════════════════════════════════════════════════════
    // PUT /admin/arbol/opciones/{id}
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public AdminOpcionResponse actualizarOpcion(Long id, OpcionRequest request) {
        OpcionDecision opcion = opcionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OpcionDecision", id));
        validarXorDestinos(request.nodoDestinoId(), request.tipoProyectoId());
        validarFkDestino(request.nodoDestinoId(), request.tipoProyectoId());
        if (request.nodoDestinoId() != null) {
            validarCiclo(opcion.getNodo().getId(), request.nodoDestinoId(), id);
        }
        NodoDecision nodoDestino = request.nodoDestinoId() != null ? nodoRepo.findById(request.nodoDestinoId()).orElse(null) : null;
        TipoProyecto tipoProyecto = request.tipoProyectoId() != null ? tipoProyectoRepo.findById(request.tipoProyectoId()).orElse(null) : null;
        opcion.setTexto(request.texto());
        opcion.setOrden(request.orden());
        opcion.setNodoDestino(nodoDestino);
        opcion.setTipoProyecto(tipoProyecto);
        opcion.setActivo(request.activo());
        opcion = opcionRepo.save(opcion);
        return toAdminOpcionResponse(opcion);
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE /admin/arbol/opciones/{id} → soft delete
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void eliminarOpcion(Long id) {
        OpcionDecision opcion = opcionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OpcionDecision", id));
        opcion.setActivo(false);
        opcionRepo.save(opcion);
    }

    // ══════════════════════════════════════════════════════════════
    // POST /admin/arbol/validar
    // ══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ValidacionResponse validar() {
        List<ValidacionItem> errores = new ArrayList<>();
        List<ValidacionItem> warnings = new ArrayList<>();

        List<NodoDecision> todosNodos = nodoRepo.findAll();
        Map<Long, NodoDecision> nodosPorId = todosNodos.stream().collect(Collectors.toMap(NodoDecision::getId, n -> n));
        List<OpcionDecision> todasOpciones = opcionRepo.findAll();

        // Filtrar activos
        List<NodoDecision> nodosActivos = todosNodos.stream().filter(NodoDecision::getActivo).toList();
        List<OpcionDecision> opcionesActivas = todasOpciones.stream().filter(OpcionDecision::getActivo).toList();

        // Mapa de opciones por nodo
        Map<Long, List<OpcionDecision>> opcionesPorNodo = opcionesActivas.stream()
                .collect(Collectors.groupingBy(o -> o.getNodo().getId()));

        // 1. Sin raíz
        long raices = nodosActivos.stream().filter(NodoDecision::getEsRaiz).count();
        if (raices == 0) {
            errores.add(new ValidacionItem("SIN_RAIZ", "No hay ningún nodo raíz activo. El árbol no es navegable.", null, null));
        }
        // 2. Múltiples raíces
        if (raices > 1) {
            errores.add(new ValidacionItem("MULTIPLE_RAIZ", "Hay más de un nodo raíz activo. Solo debe haber uno.", null, null));
        }

        // 3. Nodo sin opciones (dead-end)
        for (NodoDecision n : nodosActivos) {
            List<OpcionDecision> ops = opcionesPorNodo.getOrDefault(n.getId(), Collections.emptyList());
            if (ops.isEmpty()) {
                errores.add(new ValidacionItem("NODO_SIN_OPCIONES", "El nodo '" + n.getCodigo() + "' está activo pero no tiene opciones activas.", n.getId(), null));
            }
            // Warning: input libre sin opciones
            if (ops.isEmpty() && Boolean.TRUE.equals(n.getTieneInputLibre())) {
                warnings.add(new ValidacionItem("INPUT_LIBRE_SIN_OPCIONES", "El nodo '" + n.getCodigo() + "' tiene input libre pero no tiene opciones.", n.getId(), null));
            }
        }

        // 4. FK rotas
        for (OpcionDecision o : opcionesActivas) {
            if (o.getNodoDestino() != null && !o.getNodoDestino().getActivo()) {
                errores.add(new ValidacionItem("FK_ROTA_NODO", "La opción '" + o.getTexto() + "' del nodo '" + o.getNodo().getCodigo() + "' apunta a un nodo inactivo.", o.getNodo().getId(), o.getId()));
            }
            if (o.getTipoProyecto() != null && !o.getTipoProyecto().getActivo()) {
                errores.add(new ValidacionItem("FK_ROTA_TIPO", "La opción '" + o.getTexto() + "' del nodo '" + o.getNodo().getCodigo() + "' apunta a un tipo de proyecto inactivo.", o.getNodo().getId(), o.getId()));
            }
        }

        // 5. Ciclos
        if (raices == 1) {
            NodoDecision raiz = nodosActivos.stream().filter(NodoDecision::getEsRaiz).findFirst().orElse(null);
            if (raiz != null) {
                String ciclo = detectarCiclo(raiz.getId(), opcionesPorNodo, nodosPorId);
                if (ciclo != null) {
                    errores.add(new ValidacionItem("CICLO", "Existe un ciclo en el árbol: " + ciclo, null, null));
                }
            }
        }

        // 6. Nodos inalcanzables (warning)
        if (raices == 1) {
            NodoDecision raiz = nodosActivos.stream().filter(NodoDecision::getEsRaiz).findFirst().orElse(null);
            if (raiz != null) {
                Set<Long> alcanzables = new HashSet<>();
                bfs(raiz.getId(), opcionesPorNodo, alcanzables);
                for (NodoDecision n : nodosActivos) {
                    if (!alcanzables.contains(n.getId())) {
                        warnings.add(new ValidacionItem("NODO_INALCANZABLE", "El nodo '" + n.getCodigo() + "' no es alcanzable desde el nodo raíz.", n.getId(), null));
                    }
                }
            }
        }

        return new ValidacionResponse(errores, warnings);
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════

    private AdminNodoResponse toAdminNodoResponse(NodoDecision n) {
        List<OpcionDecision> ops = opcionRepo.findByNodoIdAndActivoTrueOrderByOrden(n.getId());
        // También incluir inactivas
        List<OpcionDecision> todasOps = opcionRepo.findByNodoIdWithTipoProyecto(n.getId());
        // Para inactivas, hacemos una query adicional sencilla
        List<AdminOpcionResponse> opsResponse = todasOps.stream().map(this::toAdminOpcionResponse).toList();
        return new AdminNodoResponse(n.getId(), n.getCodigo(), n.getPregunta(), n.getTieneInputLibre(),
                n.getInputPlaceholder(), n.getEsRaiz(), n.getOrden(), n.getActivo(), opsResponse);
    }

    private AdminOpcionResponse toAdminOpcionResponse(OpcionDecision o) {
        return new AdminOpcionResponse(o.getId(), o.getTexto(), o.getOrden(),
                o.getNodoDestino() != null ? o.getNodoDestino().getId() : null,
                o.getNodoDestino() != null ? o.getNodoDestino().getCodigo() : null,
                o.getTipoProyecto() != null ? o.getTipoProyecto().getId() : null,
                o.getTipoProyecto() != null ? o.getTipoProyecto().getCodigo() : null,
                o.getActivo());
    }

    private void validarConflictoRaiz(Long nodoEditandoId) {
        List<NodoDecision> raices = nodoRepo.findByEsRaizTrueAndActivoTrue();
        for (NodoDecision r : raices) {
            if (!r.getId().equals(nodoEditandoId)) {
                throw new BusinessException("Ya existe un nodo raíz activo. Desactiva el actual antes de marcar otro como raíz.", HttpStatus.CONFLICT);
            }
        }
    }

    private void validarXorDestinos(Long nodoDestinoId, Long tipoProyectoId) {
        if (nodoDestinoId != null && tipoProyectoId != null) {
            throw new BusinessException("Una opción debe llevar a otra pregunta O a un tipo de proyecto, no ambos.", HttpStatus.BAD_REQUEST);
        }
        if (nodoDestinoId == null && tipoProyectoId == null) {
            throw new BusinessException("Una opción debe tener un destino: otra pregunta o un tipo de proyecto.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarFkDestino(Long nodoDestinoId, Long tipoProyectoId) {
        if (nodoDestinoId != null && !nodoRepo.existsById(nodoDestinoId)) {
            throw new BusinessException("El nodo destino no existe.", HttpStatus.BAD_REQUEST);
        }
        if (tipoProyectoId != null && !tipoProyectoRepo.existsById(tipoProyectoId)) {
            throw new BusinessException("El tipo de proyecto no existe.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarCiclo(Long nodoOrigenId, Long nodoDestinoId, Long opcionEditandoId) {
        // Cargar todas las opciones activas con nodoDestino no-null
        List<OpcionDecision> todasOps = opcionRepo.findAll();
        Map<Long, List<Long>> grafo = new HashMap<>();
        for (OpcionDecision o : todasOps) {
            if (o.getActivo() && o.getNodoDestino() != null && (opcionEditandoId == null || !o.getId().equals(opcionEditandoId))) {
                grafo.computeIfAbsent(o.getNodo().getId(), k -> new ArrayList<>()).add(o.getNodoDestino().getId());
            }
        }
        // Agregar la nueva conexión
        grafo.computeIfAbsent(nodoOrigenId, k -> new ArrayList<>()).add(nodoDestinoId);
        // BFS desde nodoDestinoId buscando nodoOrigenId
        Set<Long> visitados = new HashSet<>();
        Queue<Long> cola = new LinkedList<>();
        cola.add(nodoDestinoId);
        while (!cola.isEmpty()) {
            Long actual = cola.poll();
            if (actual.equals(nodoOrigenId)) {
                throw new BusinessException("Esta opción crearía un ciclo en el árbol.", HttpStatus.BAD_REQUEST);
            }
            if (visitados.add(actual)) {
                List<Long> vecinos = grafo.getOrDefault(actual, Collections.emptyList());
                cola.addAll(vecinos);
            }
        }
    }

    private String detectarCiclo(Long raizId, Map<Long, List<OpcionDecision>> opcionesPorNodo, Map<Long, NodoDecision> nodosPorId) {
        Set<Long> visitados = new HashSet<>();
        Set<Long> enPila = new HashSet<>();
        List<String> ruta = new ArrayList<>();
        if (dfsCiclo(raizId, opcionesPorNodo, nodosPorId, visitados, enPila, ruta)) {
            return String.join(" → ", ruta);
        }
        return null;
    }

    private boolean dfsCiclo(Long nodoId, Map<Long, List<OpcionDecision>> opcionesPorNodo, Map<Long, NodoDecision> nodosPorId,
                             Set<Long> visitados, Set<Long> enPila, List<String> ruta) {
        visitados.add(nodoId);
        enPila.add(nodoId);
        NodoDecision n = nodosPorId.get(nodoId);
        ruta.add(n != null ? n.getCodigo() : "?");
        List<OpcionDecision> ops = opcionesPorNodo.getOrDefault(nodoId, Collections.emptyList());
        for (OpcionDecision o : ops) {
            if (o.getNodoDestino() != null) {
                Long dest = o.getNodoDestino().getId();
                if (!visitados.contains(dest)) {
                    if (dfsCiclo(dest, opcionesPorNodo, nodosPorId, visitados, enPila, ruta)) return true;
                } else if (enPila.contains(dest)) {
                    ruta.add(nodosPorId.get(dest) != null ? nodosPorId.get(dest).getCodigo() : "?");
                    return true;
                }
            }
        }
        enPila.remove(nodoId);
        ruta.remove(ruta.size() - 1);
        return false;
    }

    private void bfs(Long inicioId, Map<Long, List<OpcionDecision>> opcionesPorNodo, Set<Long> alcanzables) {
        Queue<Long> cola = new LinkedList<>();
        cola.add(inicioId);
        while (!cola.isEmpty()) {
            Long actual = cola.poll();
            if (alcanzables.add(actual)) {
                List<OpcionDecision> ops = opcionesPorNodo.getOrDefault(actual, Collections.emptyList());
                for (OpcionDecision o : ops) {
                    if (o.getNodoDestino() != null) cola.add(o.getNodoDestino().getId());
                }
            }
        }
    }
}