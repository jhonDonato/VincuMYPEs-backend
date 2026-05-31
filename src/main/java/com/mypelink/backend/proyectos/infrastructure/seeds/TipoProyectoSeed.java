package com.mypelink.backend.proyectos.infrastructure.seeds;

import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(1)
public class TipoProyectoSeed implements CommandLineRunner {

    private final TipoProyectoRepository tipoProyectoRepository;

    @Override
    public void run(String... args) {
        upsert("1.1", "Sitio web de una página (landing) con captación de clientes",
                "Presencia digital y captación", AreaSistemas.DESARROLLO_WEB, "Ligero", 7, 2, 2, 30, 10, 14,
                "Una web sencilla y publicada para mostrar tu negocio y captar contactos.",
                "Una página responsive (inicio, servicios, contacto), formulario al WhatsApp/correo, despliegue en hosting gratuito.",
                "Catálogo dinámico, tienda en línea o pasarela de pagos.");

        upsert("1.2", "Catálogo digital de productos administrable con buscador",
                "Presencia digital y captación", AreaSistemas.DESARROLLO_WEB, "Medio", 8, 3, 4, 50, 14, 21,
                "Un catálogo en línea con buscador que tú misma puedes actualizar.",
                "Catálogo con categorías, buscador/filtro, fichas con imagen y precio, y panel para cargar/editar productos.",
                "Pagos en línea, carrito de compras real o inventario en tiempo real.");

        upsert("1.3", "Prototipo funcional de registro de clientes y pedidos",
                "Presencia digital y captación", AreaSistemas.DESARROLLO_SOFTWARE, "Alto", 8, 4, 5, 80, 21, 28,
                "Un sistema simple para registrar y consultar clientes, pedidos o citas.",
                "Formulario de captura, panel de consulta/filtrado, datos persistidos en BD y despliegue en plan gratuito.",
                "Pagos, facturación electrónica o aplicación móvil.");

        upsert("1.4", "Tablero de indicadores del negocio (dashboard BI)",
                "Datos e inteligencia de negocio", AreaSistemas.ANALISIS_DATOS, "Medio", 7, 3, 4, 60, 14, 21,
                "Un tablero visual con tus indicadores clave, hecho en una herramienta gratuita que puedes seguir usando.",
                "Definición de KPIs, conexión a tu fuente (Excel/Sheets) y tablero con filtros en Looker Studio o Power BI.",
                "Backend a medida o datos en tiempo real desde un punto de venta.");

        upsert("2.1", "Diseño e implementación de base de datos del negocio",
                "Datos e inteligencia de negocio", AreaSistemas.BASE_DE_DATOS, "Alto", 7, 4, 5, 90, 21, 28,
                "La estructura ordenada y segura para guardar la información de tu negocio.",
                "Levantamiento de requerimientos, modelo conceptual/lógico/físico, scripts SQL y base poblada con datos de ejemplo.",
                "La aplicación encima de la base ni despliegue en servidor de pago.");

        upsert("2.2", "Limpieza, estandarización y estructura de datos",
                "Datos e inteligencia de negocio", AreaSistemas.BASE_DE_DATOS, "Medio-alto", 7, 3, 4, 65, 18, 24,
                "Tus datos actuales limpios, sin duplicados y listos para usar.",
                "Diagnóstico de datos, deduplicación, estandarización de formatos y entrega de datos limpios con reglas para mantenerlos.",
                "Construir la base de datos completa (eso es 2.1) ni automatización avanzada.");

        upsert("2.3", "Diagnóstico de negocio con análisis de datos (BI)",
                "Datos e inteligencia de negocio", AreaSistemas.ANALISIS_DATOS, "Medio-alto", 7, 3, 4, 70, 18, 24,
                "Un análisis de tus datos con hallazgos y recomendaciones para decidir mejor.",
                "Análisis descriptivo de ventas/clientes/inventario, hallazgos, segmentación simple y recomendaciones accionables.",
                "Modelos predictivos / machine learning ni ingeniería de datos pesada.");

        upsert("3.1", "Diseño de interfaz y prototipo navegable (UI/UX)",
                "Experiencia de usuario", AreaSistemas.DESARROLLO_SOFTWARE, "Medio-ligero", 7, 2, 3, 45, 14, 21,
                "El diseño visual y navegable de tu app o sistema, antes de programarlo.",
                "Wireframes, prototipo de alta fidelidad navegable y guía de estilo.",
                "La implementación o el código.");

        upsert("3.2", "Auditoría y rediseño de un proceso digital (UX)",
                "Experiencia de usuario", AreaSistemas.DESARROLLO_SOFTWARE, "Medio-ligero", 7, 2, 3, 45, 14, 21,
                "Una mejora del proceso digital que hoy confunde a tus clientes.",
                "Auditoría de usabilidad de tu web/proceso actual, propuesta de flujos simplificados y prototipo comparativo.",
                "La implementación de los cambios.");

        upsert("3.3", "Mapa de experiencia del cliente (journey map)",
                "Experiencia de usuario", AreaSistemas.DESARROLLO_SOFTWARE, "Ligero", 7, 2, 2, 35, 14, 18,
                "Un mapa de todo el recorrido de tu cliente con sus puntos de fricción.",
                "Mapa del viaje del cliente, identificación de puntos de fricción y matriz de oportunidades por impacto.",
                "Implementar las soluciones propuestas.");

        upsert("4.1", "Diagnóstico de red local y plan de mejora",
                "Infraestructura y redes", AreaSistemas.SOPORTE_TI, "Medio", 7, 2, 3, 50, 14, 21,
                "Un diagnóstico de por qué tu red falla y cómo mejorarla a tu presupuesto.",
                "Relevamiento de la red, medición de rendimiento/cobertura, topología y plan de mejora con equipos al presupuesto.",
                "Compra o instalación de equipos.");

        upsert("4.2", "Diseño técnico de red para un local nuevo",
                "Infraestructura y redes", AreaSistemas.SOPORTE_TI, "Medio", 8, 3, 4, 58, 18, 24,
                "El diseño técnico de la red para tu nuevo local antes de instalarla.",
                "Plano lógico/físico, ubicación de cableado y puntos Wi-Fi, especificaciones de hardware y seguridad perimetral inicial.",
                "La ejecución de obra o la instalación física.");

        upsert("5.1", "Auditoría preventiva básica de seguridad digital",
                "Seguridad y continuidad", AreaSistemas.SOPORTE_TI, "Medio", 8, 2, 3, 45, 14, 21,
                "Una revisión de qué tan protegidas están tus cuentas y datos.",
                "Revisión de cuentas y contraseñas (sin pedir credenciales), permisos/roles, riesgos y manual preventivo.",
                "Pentesting ofensivo o implementación de soluciones complejas.");

        upsert("5.2", "Plan e implementación de respaldo (backup) en nube gratuita",
                "Seguridad y continuidad", AreaSistemas.SOPORTE_TI, "Medio", 8, 2, 3, 45, 14, 21,
                "Asegurar que nunca pierdas tus archivos importantes, con respaldo automático.",
                "Política y cronograma de backups, respaldo automático en nube gratuita, manual de recuperación y prueba de restauración.",
                "Infraestructura de pago o alta disponibilidad.");
    }

    private void upsert(String codigo, String nombre, String rama, AreaSistemas area,
                        String complejidad, int cicloMinimo, int cuposMin, int cuposMax,
                        int esfuerzoHPers, int diasMin, int diasSugerido,
                        String descripcionMype, String alcanceIncluye, String alcanceNoIncluye) {
        Optional<TipoProyecto> existente = tipoProyectoRepository.findByCodigo(codigo);
        TipoProyecto tp = existente.orElseGet(() -> TipoProyecto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .rama(rama)
                .cicloMinimo(cicloMinimo)
                .incluyePresupuesto(false)
                .activo(true)
                .build());

        tp.setAreaSistemas(area);
        tp.setCuposMin(cuposMin);
        tp.setCuposMax(cuposMax);
        tp.setDiasMin(diasMin);
        tp.setDiasSugerido(diasSugerido);
        tp.setComplejidad(complejidad);
        tp.setEsfuerzoHPers(esfuerzoHPers);
        tp.setAlcanceIncluye(alcanceIncluye);
        tp.setAlcanceNoIncluye(alcanceNoIncluye);
        if (tp.getDescripcionMype() == null || tp.getDescripcionMype().isBlank()) {
            tp.setDescripcionMype(descripcionMype);
        }
        tipoProyectoRepository.save(tp);
    }
}