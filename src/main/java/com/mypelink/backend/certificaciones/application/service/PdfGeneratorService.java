package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.usuarios.domain.model.Mype;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class PdfGeneratorService {

    public byte[] generarCertificadoPDF(Certificado certificado, Proyecto proyecto, Mype mype, String firmaBase64, String gerenteNombre) {
        log.debug("[PDF] Generando certificado - estudiante={}, proyecto={}, firma={}",
                certificado.getEstudiante().getUsuario().getNombre(),
                proyecto.getTitulo(),
                firmaBase64 != null ? "presente" : "ausente");

        String html = buildHtml(certificado, proyecto, mype, firmaBase64, gerenteNombre);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF del certificado", e);
        }
    }

    private String buildHtml(Certificado certificado, Proyecto proyecto, Mype mype, String firmaBase64, String gerenteNombre) {
        String nombreEstudiante = certificado.getEstudiante().getUsuario().getNombre();
        String tituloProyecto = proyecto.getTitulo();
        String nombreMype = mype.getNombreComercial();
        String gerente = (gerenteNombre != null && !gerenteNombre.isBlank()) ? gerenteNombre : nombreMype;
        String descripcion = certificado.getDescripcionCertificado() != null ? certificado.getDescripcionCertificado() : "";
        String fecha = certificado.getFechaEmision().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        String codigo = certificado.getCodigo();

        String firmaHtml;
        if (firmaBase64 != null && !firmaBase64.isEmpty()) {
            firmaHtml = "<img src=\"" + firmaBase64 + "\" style=\"height:36px; margin-bottom:4px; display:block; margin-left:auto; margin-right:auto;\" />";
        } else {
            firmaHtml = "<div style=\"height:24px; margin-bottom:4px; font-size:10px; color:#9CA3AF; font-style:italic;\">firma digital</div>";
        }

        String html = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <style type="text/css">
            body {
                font-family: Georgia, serif;
                margin: 0;
                padding: 30px;
                font-size: 12px;
                color: #111827;
            }
            .contenedor {
                width: 720px;
                margin: 0 auto;
                border: 1px solid #E5E7EB;
                padding: 40px;
                position: relative;
            }
            .barra-top {
                height: 5px;
                background-color: #1B6FE8;
                margin-bottom: 28px;
            }
            .header {
                text-align: center;
                margin-bottom: 24px;
            }
            .logo-table {
                width: 100%%;
                margin-bottom: 12px;
            }
            .logo-table td {
                text-align: center;
                vertical-align: middle;
            }
            .brand-nombre {
                font-size: 18px;
                font-weight: bold;
                color: #1E3A5F;
            }
            .brand-accent {
                color: #06B6D4;
            }
            .divisor {
                height: 1px;
                background-color: #1B6FE8;
                width: 60%%;
                margin: 0 auto 12px auto;
            }
            .cert-tipo {
                font-size: 9px;
                font-weight: bold;
                color: #9CA3AF;
                text-transform: uppercase;
                letter-spacing: 2px;
                margin-bottom: 4px;
            }
            .cert-titulo {
                font-size: 24px;
                font-weight: bold;
                color: #111827;
                margin-bottom: 2px;
            }
            .cert-subtitulo {
                font-size: 11px;
                color: #6B7280;
            }
            .cuerpo {
                text-align: center;
                margin-bottom: 28px;
            }
            .cuerpo-intro {
                font-size: 12px;
                color: #6B7280;
                margin-bottom: 6px;
            }
            .nombre-estudiante {
                font-size: 22px;
                font-weight: bold;
                color: #1B6FE8;
                font-style: italic;
                border-bottom: 2px solid #E5E7EB;
                padding-bottom: 6px;
                display: block;
                margin-bottom: 10px;
            }
            .proyecto-titulo {
                font-size: 14px;
                font-weight: bold;
                color: #111827;
                margin-bottom: 8px;
            }
            .descripcion {
                font-size: 11px;
                color: #9CA3AF;
                line-height: 1.6;
            }
            .footer-table {
                width: 100%%;
                border-top: 1px solid #F3F4F6;
                padding-top: 18px;
            }
            .footer-table td {
                text-align: center;
                vertical-align: bottom;
                width: 33%%;
                padding: 0 8px;
            }
            .linea-firma {
                width: 90px;
                height: 1px;
                background-color: #D1D5DB;
                margin: 4px auto 5px auto;
            }
            .firma-nombre {
                font-size: 11px;
                font-weight: bold;
                color: #374151;
            }
            .firma-empresa {
                font-size: 9px;
                color: #9CA3AF;
            }
            .fecha-label {
                font-size: 8px;
                color: #9CA3AF;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            .fecha-valor {
                font-size: 11px;
                font-weight: bold;
                color: #374151;
            }
            .codigo-label {
                font-size: 9px;
                color: #9CA3AF;
            }
            .codigo-valor {
                font-size: 8px;
                color: #D1D5DB;
                font-family: monospace;
            }
            .esquina {
                position: absolute;
                width: 22px;
                height: 22px;
            }
            .esquina-tl {
                top: 12px;
                left: 12px;
                border-top: 2px solid #1B6FE8;
                border-left: 2px solid #1B6FE8;
            }
            .esquina-tr {
                top: 12px;
                right: 12px;
                border-top: 2px solid #1B6FE8;
                border-right: 2px solid #1B6FE8;
            }
            .esquina-bl {
                bottom: 12px;
                left: 12px;
                border-bottom: 2px solid #1B6FE8;
                border-left: 2px solid #1B6FE8;
            }
            .esquina-br {
                bottom: 12px;
                right: 12px;
                border-bottom: 2px solid #1B6FE8;
                border-right: 2px solid #1B6FE8;
            }
        </style>
    </head>
    <body>
    <div class="contenedor">

        <!-- Esquinas decorativas — position:absolute SÍ funciona en Flying Saucer -->
        <div class="esquina esquina-tl"></div>
        <div class="esquina esquina-tr"></div>
        <div class="esquina esquina-bl"></div>
        <div class="esquina esquina-br"></div>

        <!-- Barra superior de color sólido en lugar de gradient -->
        <div class="barra-top"></div>

        <!-- HEADER -->
        <div class="header">
            <table class="logo-table"><tr><td>
                <svg width="28" height="28" viewBox="0 0 100 100">
                    <path d="M20 15 L50 85 L65 85 L35 15 Z" fill="#1B6FE8"/>
                    <path d="M80 15 L50 85 L35 85 L65 15 Z" fill="#06B6D4"/>
                    <circle cx="50" cy="85" r="8" fill="#F97316"/>
                </svg>
                &#160;
                <span class="brand-nombre">Vincu<span class="brand-accent">MYPEs</span></span>
            </td></tr></table>
            <div class="divisor"></div>
            <div class="cert-tipo">Certificado de Participaci&#243;n</div>
            <div class="cert-titulo">VincuMYPEs</div>
            <div class="cert-subtitulo">Plataforma de vinculaci&#243;n acad&#233;mico-empresarial &#183; Cajamarca, Per&#250;</div>
        </div>

        <!-- CUERPO -->
        <div class="cuerpo">
            <div class="cuerpo-intro">Este certificado se otorga a</div>
            <span class="nombre-estudiante">%s</span>
            <div class="cuerpo-intro">por su participaci&#243;n y culminaci&#243;n exitosa del proyecto</div>
            <div class="proyecto-titulo">%s</div>
            <div class="descripcion">%s</div>
        </div>

        <!-- FOOTER — tabla de 3 columnas en lugar de flex -->
        <table style="width:100%; border-collapse:collapse;
                                               border-top:1px solid #F3F4F6; padding-top:18px;">
                                   <tr>
                                     <td style="width:33%; text-align:center; vertical-align:bottom;">
                                       [firma]
                                       <div style="width:90px; height:1px; background:#D1D5DB;
                                                   margin:4px auto;"></div>
                                       <div style="font-size:11px; font-weight:600;">%s</div>
                                       <div style="font-size:9px; color:#9CA3AF;">%s</div>
                                     </td>
                                     <td style="width:33%; text-align:center; vertical-align:bottom;">
                                       <svg width="46" height="46"><circle cx="23" cy="23" r="22"
                                         fill="none" stroke="#1B6FE8" stroke-width="2"/>...</svg>
                                       ...fecha
                                     </td>
                                     <td style="width:33%; text-align:center; vertical-align:bottom;">
                                       ...QR / código
                                     </td>
                                   </tr>
                                 </table>
    </div>
    </body>
    </html>
    """;

        return String.format(html,
                nombreEstudiante,
                tituloProyecto,
                descripcion,
                firmaHtml,
                gerente,
                nombreMype,
                fecha,
                codigo
        );
    }
}
