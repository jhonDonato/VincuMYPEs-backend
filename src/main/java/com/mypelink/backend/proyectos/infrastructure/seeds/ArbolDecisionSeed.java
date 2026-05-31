package com.mypelink.backend.proyectos.infrastructure.seeds;

import com.mypelink.backend.proyectos.domain.model.NodoDecision;
import com.mypelink.backend.proyectos.domain.model.OpcionDecision;
import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.NodoDecisionRepository;
import com.mypelink.backend.proyectos.domain.repository.OpcionDecisionRepository;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(2)
public class ArbolDecisionSeed implements CommandLineRunner {

    private final NodoDecisionRepository nodoRepo;
    private final OpcionDecisionRepository opcionRepo;
    private final TipoProyectoRepository tipoProyectoRepo;

    @Override
    public void run(String... args) {
        NodoDecision inicio = upsertNodo("inicio", "¿Cuál es la principal necesidad actual de tu negocio?", true, true, "O descríbela con tus propias palabras...", 1);
        NodoDecision mostrarInternet = upsertNodo("mostrar_internet", "¿Cuál es tu situación digital actual?", false, false, null, 2);
        NodoDecision organizarInfo = upsertNodo("organizar_info", "¿Qué tipo de información necesitas organizar prioritariamente?", false, false, null, 2);
        NodoDecision excelOpciones = upsertNodo("excel_opciones", "¿Qué te gustaría lograr principalmente con esos datos?", false, false, null, 3);
        NodoDecision experienciaClientes = upsertNodo("experiencia_clientes", "¿Tienes identificado qué aspecto deseas optimizar con tus clientes?", false, false, null, 2);
        NodoDecision infraestructura = upsertNodo("infraestructura", "¿Cuál es la situación más crítica de tu entorno tecnológico?", true, false, "O descríbela con tus propias palabras...", 2);

        // Opciones de inicio
        upsertOpcion(inicio, "Mostrar mi negocio en internet o captar clientes", 1, mostrarInternet, null);
        upsertOpcion(inicio, "Organizar u ordenar la información interna de mi negocio", 2, organizarInfo, null);
        upsertOpcion(inicio, "Mejorar la experiencia digital de mis clientes", 3, experienciaClientes, null);
        upsertOpcion(inicio, "Mejorar mi red local o mi infraestructura tecnológica", 4, infraestructura, null);

        // Opciones hoja para mostrar_internet
        upsertOpcionHoja(mostrarInternet, "No tengo presencia en internet todavía", 1, "1.1");
        upsertOpcionHoja(mostrarInternet, "Tengo redes sociales activas pero no una página web", 2, "1.1");
        upsertOpcionHoja(mostrarInternet, "Tengo una web básica pero quiero mostrar mejor mis productos", 3, "1.2");

        // Opciones para organizar_info
        upsertOpcionHoja(organizarInfo, "Control de clientes, pedidos, citas o reservas", 1, "1.3");
        upsertOpcion(organizarInfo, "Registro de ventas, inventario o datos en Excel", 2, excelOpciones, null);
        upsertOpcionHoja(organizarInfo, "No lo sé con certeza, mi información está muy desorganizada", 3, "2.2");

        // Opciones hoja para excel_opciones
        upsertOpcionHoja(excelOpciones, "Entender patrones ocultos y qué me dicen los datos", 1, "2.3");
        upsertOpcionHoja(excelOpciones, "Visualizarlos en gráficos interactivos fáciles de entender", 2, "1.4");
        upsertOpcionHoja(excelOpciones, "Estructurarlos en una base de datos real y segura", 3, "2.1");

        // Opciones hoja para experiencia_clientes
        upsertOpcionHoja(experienciaClientes, "Sí, sé exactamente qué aplicación o sistema web requiero mapear", 1, "3.1");
        upsertOpcionHoja(experienciaClientes, "Tengo un proceso digital que suele confundir a mis clientes", 2, "3.2");
        upsertOpcionHoja(experienciaClientes, "No sé dónde está el cuello de botella o por qué abandonan mi web", 3, "3.3");

        // Opciones hoja para infraestructura
        upsertOpcionHoja(infraestructura, "Mi red local falla, la conexión va lenta y desconozco el motivo", 1, "4.1");
        upsertOpcionHoja(infraestructura, "Voy a abrir un local nuevo y necesito saber qué equipos instalar", 2, "4.2");
        upsertOpcionHoja(infraestructura, "Quiero saber si las cuentas y datos de mi negocio están protegidos", 3, "5.1");
        upsertOpcionHoja(infraestructura, "Quiero asegurar que nunca perderé mis archivos importantes", 4, "5.2");
    }

    private NodoDecision upsertNodo(String codigo, String pregunta, boolean inputLibre, boolean esRaiz, String placeholder, int orden) {
        NodoDecision n = nodoRepo.findByCodigo(codigo)
                .orElseGet(() -> NodoDecision.builder().codigo(codigo).build());
        n.setPregunta(pregunta);
        n.setTieneInputLibre(inputLibre);
        n.setEsRaiz(esRaiz);
        n.setInputPlaceholder(placeholder);
        n.setOrden(orden);
        n.setActivo(true);
        return nodoRepo.save(n);
    }

    private OpcionDecision upsertOpcion(NodoDecision nodo, String texto, int orden, NodoDecision destino, TipoProyecto tipoProyecto) {
        Optional<OpcionDecision> existente = opcionRepo.findByNodoIdAndOrden(nodo.getId(), orden);
        OpcionDecision o = existente.orElseGet(() -> OpcionDecision.builder().nodo(nodo).orden(orden).build());
        o.setTexto(texto);
        o.setNodoDestino(destino);
        o.setTipoProyecto(tipoProyecto);
        o.setActivo(true);
        return opcionRepo.save(o);
    }

    private void upsertOpcionHoja(NodoDecision nodo, String texto, int orden, String codigoTipo) {
        TipoProyecto tipo = tipoProyectoRepo.findByCodigo(codigoTipo)
                .orElseThrow(() -> new IllegalStateException("Falta TipoProyecto con código " + codigoTipo));
        upsertOpcion(nodo, texto, orden, null, tipo);
    }
}