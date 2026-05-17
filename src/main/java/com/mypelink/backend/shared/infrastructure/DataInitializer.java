package com.mypelink.backend.shared.infrastructure;

import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TipoProyectoRepository tipoProyectoRepository;

    @Override
    public void run(String... args) {
        if (tipoProyectoRepository.count() > 0) return;

        tipoProyectoRepository.saveAll(List.of(
                TipoProyecto.builder().codigo("LANDING_PAGE").nombre("Pagina web de presentacion")
                        .descripcionMype("Una pagina web donde tus clientes pueden conocer tu negocio y contactarte desde el celular")
                        .descripcionEstudiante("Landing page estatica responsiva con formulario de contacto funcional")
                        .rama("WEB").cicloMinimo(7).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("CATALOGO_DIGITAL").nombre("Catalogo de productos en linea")
                        .descripcionMype("Una web donde muestras tus productos o servicios con fotos y precios, visible desde cualquier celular")
                        .descripcionEstudiante("MVP de catalogo navegable, puede ser web estatica o Glide/Notion")
                        .rama("WEB").cicloMinimo(7).incluyePresupuesto(true).incluyeCronograma(true).build(),

                TipoProyecto.builder().codigo("REGISTRO_CLIENTES").nombre("Sistema basico de registro")
                        .descripcionMype("Un formulario donde registras clientes, pedidos o reservas y los ves organizados en una tabla")
                        .descripcionEstudiante("Formulario web funcional conectado a Google Sheets o Airtable")
                        .rama("WEB").cicloMinimo(7).incluyePresupuesto(true).incluyeCronograma(true).build(),

                TipoProyecto.builder().codigo("DASHBOARD").nombre("Tablero de visualizacion de datos")
                        .descripcionMype("Graficos y resumenes automaticos de tus ventas o inventario a partir de tus datos en Excel")
                        .descripcionEstudiante("Dashboard con Chart.js, Looker Studio o Power BI consumiendo Google Sheets")
                        .rama("BD").cicloMinimo(8).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("DISENO_BD").nombre("Base de datos organizada")
                        .descripcionMype("Tus datos estructurados correctamente en una base de datos con consultas listas para usar")
                        .descripcionEstudiante("Modelo ER documentado + BD MySQL con datos de muestra e insercion real")
                        .rama("BD").cicloMinimo(8).incluyePresupuesto(true).incluyeCronograma(true).build(),

                TipoProyecto.builder().codigo("LIMPIEZA_DATOS").nombre("Organizacion y limpieza de datos")
                        .descripcionMype("Tus datos de Excel desordenados quedan limpios, sin duplicados y faciles de consultar")
                        .descripcionEstudiante("ETL basico: limpieza, normalizacion y estructuracion en Excel o BD")
                        .rama("BD").cicloMinimo(7).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("ANALISIS_EXPLORATORIO").nombre("Analisis de datos de tu negocio")
                        .descripcionMype("Un reporte con los hallazgos mas importantes de tus datos: que se vende mas, en que horarios, que clientes regresan")
                        .descripcionEstudiante("EDA con Python/pandas o Excel avanzado, entregable PDF con graficos y recomendaciones")
                        .rama("BD").cicloMinimo(8).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("PROTOTIPO_FIGMA").nombre("Diseno de aplicacion o sistema")
                        .descripcionMype("El diseno visual completo de la app o sistema que necesitas, navegable en pantalla antes de desarrollarlo")
                        .descripcionEstudiante("Prototipo clickeable en Figma con flujo principal y pantallas documentadas")
                        .rama("UX").cicloMinimo(7).incluyePresupuesto(true).incluyeCronograma(true).build(),

                TipoProyecto.builder().codigo("REDISENO_UX").nombre("Mejora de proceso existente")
                        .descripcionMype("Un rediseno de un proceso de tu negocio que hoy confunde o demora a tus clientes")
                        .descripcionEstudiante("Analisis de flujo actual + wireframes del flujo mejorado documentados")
                        .rama("UX").cicloMinimo(7).incluyePresupuesto(true).incluyeCronograma(true).build(),

                TipoProyecto.builder().codigo("MAPA_CLIENTE").nombre("Mapa de experiencia del cliente")
                        .descripcionMype("Un documento visual que muestra como vive tu cliente cada etapa de su compra y donde hay oportunidades de mejora")
                        .descripcionEstudiante("Customer journey map con entrevistas basicas, analisis de pain points y propuesta de mejora")
                        .rama("UX").cicloMinimo(7).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("DIAGNOSTICO_RED").nombre("Diagnostico de red local")
                        .descripcionMype("Un reporte detallado del estado de tu red con problemas identificados y recomendaciones concretas")
                        .descripcionEstudiante("Auditoria de red: topologia actual, vulnerabilidades, recomendaciones priorizadas")
                        .rama("REDES").cicloMinimo(9).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("DISENO_RED").nombre("Diseno de red para local nuevo")
                        .descripcionMype("Un plano completo de que equipos de red necesitas y como instalarlos en tu nuevo local")
                        .descripcionEstudiante("Topologia recomendada, lista de equipos con precios referenciales, configuracion basica documentada")
                        .rama("REDES").cicloMinimo(9).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("AUDITORIA_SEGURIDAD").nombre("Revision de seguridad digital")
                        .descripcionMype("Un reporte de que tan protegido esta tu negocio digitalmente y que hacer para mejorar")
                        .descripcionEstudiante("Auditoria de contrasenas, configuraciones, redes y correo. Reporte con plan de accion priorizado")
                        .rama("SEGURIDAD").cicloMinimo(10).incluyePresupuesto(false).incluyeCronograma(false).build(),

                TipoProyecto.builder().codigo("PLAN_BACKUP").nombre("Plan de respaldo de datos")
                        .descripcionMype("Un sistema configurado para que nunca pierdas tus archivos importantes del negocio")
                        .descripcionEstudiante("Diseno e implementacion de backup automatizado con Google Drive/OneDrive + procedimiento documentado")
                        .rama("SEGURIDAD").cicloMinimo(10).incluyePresupuesto(false).incluyeCronograma(false).build()
        ));
    }
}