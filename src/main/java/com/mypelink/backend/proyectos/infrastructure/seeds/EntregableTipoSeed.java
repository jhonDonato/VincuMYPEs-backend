package com.mypelink.backend.proyectos.infrastructure.seeds;

import com.mypelink.backend.proyectos.domain.model.EntregableTipo;
import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.EntregableTipoRepository;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(3) // después de ArbolDecisionSeed
public class EntregableTipoSeed implements CommandLineRunner {

    private final EntregableTipoRepository entregableTipoRepo;
    private final TipoProyectoRepository tipoProyectoRepo;

    @Override
    public void run(String... args) {
        // 1.1 - Landing page
        upsert("1.1", 1, "Diseño visual previo de la estructura de la página");
        upsert("1.1", 2, "Código fuente completo alojado en GitHub o GitLab");
        upsert("1.1", 3, "Página web publicada y accesible desde internet");
        upsert("1.1", 4, "Formulario de contacto vinculado a WhatsApp o correo");
        upsert("1.1", 5, "Manual de usuario para modificar los textos");

        // 1.2 - Catálogo digital
        upsert("1.2", 1, "Estructura organizada de categorías de productos");
        upsert("1.2", 2, "Web con buscador interactivo y visualización de imágenes");
        upsert("1.2", 3, "Código fuente completo en Git");
        upsert("1.2", 4, "Manual de administración para registrar y editar productos");

        // 1.3 - Registro de clientes
        upsert("1.3", 1, "Formulario web interactivo para capturar datos de clientes");
        upsert("1.3", 2, "Panel interno para consultar y filtrar pedidos o citas");
        upsert("1.3", 3, "Código fuente con manejo seguro de sesiones");
        upsert("1.3", 4, "Guía de operación del sistema para el personal");

        // 1.4 - Dashboard BI
        upsert("1.4", 1, "Maquetación del panel de control gráfico");
        upsert("1.4", 2, "Dashboard con gráficos estadísticos (barras, líneas, KPI)");
        upsert("1.4", 3, "Módulo de importación de datos desde Excel/CSV");
        upsert("1.4", 4, "Manual interpretativo para análisis de métricas");

        // 2.1 - Base de datos
        upsert("2.1", 1, "Diagrama Entidad-Relación conceptual y lógico");
        upsert("2.1", 2, "Scripts SQL estructurados y listos para ejecutar");
        upsert("2.1", 3, "Diccionario de datos de cada tabla y columna");
        upsert("2.1", 4, "Reporte de pruebas de conectividad y optimización");

        // 2.2 - Limpieza de datos
        upsert("2.2", 1, "Informe diagnóstico de errores e inconsistencias");
        upsert("2.2", 2, "Archivos o tablas limpias, sin duplicados");
        upsert("2.2", 3, "Scripts de transformación de datos");
        upsert("2.2", 4, "Documentación del nuevo formato estandarizado");

        // 2.3 - Análisis exploratorio
        upsert("2.3", 1, "Informe ejecutivo de analítica descriptiva con hallazgos clave");
        upsert("2.3", 2, "Gráficos de tendencias de venta, horarios pico e inventario");
        upsert("2.3", 3, "Segmentación de clientes basada en comportamiento histórico");
        upsert("2.3", 4, "Presentación con conclusiones para la toma de decisiones");

        // 3.1 - Diseño UI/UX
        upsert("3.1", 1, "Wireframes del flujo de navegación inicial");
        upsert("3.1", 2, "Prototipo de alta fidelidad interactivo en Figma");
        upsert("3.1", 3, "Guía de estilo con colores, tipografías e íconos");
        upsert("3.1", 4, "Recursos visuales exportados para desarrollo");

        // 3.2 - Rediseño UX
        upsert("3.2", 1, "Informe de auditoría de usabilidad de la plataforma actual");
        upsert("3.2", 2, "Propuesta visual con flujos simplificados");
        upsert("3.2", 3, "Prototipo comparativo demostrando las mejoras");
        upsert("3.2", 4, "Especificaciones y estándares UX recomendados");

        // 3.3 - Journey map
        upsert("3.3", 1, "Mapa visual del viaje del cliente interactivo");
        upsert("3.3", 2, "Identificación de puntos de fricción y cuellos de botella");
        upsert("3.3", 3, "Matriz de oportunidades de mejora por impacto");
        upsert("3.3", 4, "Informe estratégico con tácticas aplicables");

        // 4.1 - Diagnóstico de red
        upsert("4.1", 1, "Informe del estado, rendimiento y cobertura actual de tu red");
        upsert("4.1", 2, "Diagrama de topología física con fallas identificadas");
        upsert("4.1", 3, "Plan de acción con configuraciones optimizadas");
        upsert("4.1", 4, "Lista de equipos recomendados alineada al presupuesto");

        // 4.2 - Diseño de red
        upsert("4.2", 1, "Plano constructivo y lógico de conexiones de red");
        upsert("4.2", 2, "Ubicación de cableado y puntos de acceso Wi-Fi");
        upsert("4.2", 3, "Especificaciones de hardware recomendadas");
        upsert("4.2", 4, "Arquitectura de seguridad perimetral inicial");

        // 5.1 - Auditoría de seguridad
        upsert("5.1", 1, "Informe de riesgos en cuentas de correo y contraseñas");
        upsert("5.1", 2, "Reporte de permisos, accesos y roles del personal");
        upsert("5.1", 3, "Manual preventivo de ciberseguridad");
        upsert("5.1", 4, "Plan de acción para el blindaje de credenciales críticas");

        // 5.2 - Plan de backup
        upsert("5.2", 1, "Política y cronograma de copias de seguridad");
        upsert("5.2", 2, "Scripts o software de backup automático en la nube");
        upsert("5.2", 3, "Manual de recuperación de archivos ante emergencias");
        upsert("5.2", 4, "Reporte de pruebas exitosas de restauración");
    }

    private void upsert(String codigoTipo, int orden, String titulo) {
        TipoProyecto tipo = tipoProyectoRepo.findByCodigo(codigoTipo)
                .orElseThrow(() -> new IllegalStateException("Falta TipoProyecto con código " + codigoTipo));
        entregableTipoRepo.findByTipoProyectoIdAndOrden(tipo.getId(), orden)
                .ifPresentOrElse(
                        e -> { e.setTitulo(titulo); entregableTipoRepo.save(e); },
                        () -> {
                            EntregableTipo e = EntregableTipo.builder()
                                    .tipoProyecto(tipo)
                                    .titulo(titulo)
                                    .orden(orden)
                                    .build();
                            entregableTipoRepo.save(e);
                        });
    }
}