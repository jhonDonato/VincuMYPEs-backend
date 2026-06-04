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

    public byte[] generarCertificadoPDF(Certificado certificado, Proyecto proyecto, Mype mype,
                                        String firmaBase64, String gerenteNombre) {
        log.debug("[PDF] Generando - estudiante={}, proyecto={}",
                certificado.getEstudiante().getUsuario().getNombre(),
                proyecto.getTitulo());

        String html = buildHtml(certificado, proyecto, mype, firmaBase64, gerenteNombre);

        // Log para debug - ver el HTML generado
        log.debug("[PDF] HTML generado (primeros 500 chars): {}",
                html.length() > 500 ? html.substring(0, 500) : html);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("[PDF] Error generando certificado: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF del certificado: " + e.getMessage(), e);
        }
    }

    private String buildHtml(Certificado certificado, Proyecto proyecto, Mype mype,
                             String firmaBase64, String gerenteNombre) {

        String nombreEstudiante = certificado.getEstudiante().getUsuario().getNombre();
        String tituloProyecto   = proyecto.getTitulo();
        String nombreMype       = mype.getNombreComercial();
        String gerente          = (gerenteNombre != null && !gerenteNombre.isBlank())
                ? gerenteNombre : nombreMype;
        String descripcion      = certificado.getDescripcionCertificado() != null
                ? certificado.getDescripcionCertificado() : "";
        String fecha            = certificado.getFechaEmision()
                .format(DateTimeFormatter.ofPattern(
                        "dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        String codigo           = certificado.getCodigo();

        // Firma simplificada
        String firmaHtml;
        if (firmaBase64 != null && !firmaBase64.isBlank()) {
            String src = firmaBase64.startsWith("data:") ? firmaBase64
                    : "data:image/png;base64," + firmaBase64;
            firmaHtml = "<img src=\"" + src + "\" style=\"height:40px;\" />";
        } else {
            firmaHtml = "<div style=\"height:40px;\">_________________</div>";
        }

        // HTML simplificado - sin SVG complejos para evitar errores
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\"/>\n"
                + "<style>\n"
                + "  @page { size: A4 landscape; margin: 0.5in; }\n"
                + "  body { font-family: 'Times New Roman', Georgia, serif; margin: 0; padding: 0; background: #fff; }\n"
                + "  .certificado { border: 2px solid #1B6FE8; border-radius: 12px; padding: 20px 30px; position: relative; background: #fff; }\n"
                + "  .esquina { position: absolute; width: 30px; height: 30px; }\n"
                + "  .esquina-tl { top: 10px; left: 10px; border-top: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; }\n"
                + "  .esquina-tr { top: 10px; right: 10px; border-top: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; }\n"
                + "  .esquina-bl { bottom: 10px; left: 10px; border-bottom: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; }\n"
                + "  .esquina-br { bottom: 10px; right: 10px; border-bottom: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; }\n"
                + "  .header { text-align: center; margin-bottom: 20px; }\n"
                + "  .logo-texto { font-size: 24px; font-weight: bold; color: #1E3A5F; }\n"
                + "  .logo-accent { color: #06B6D4; }\n"
                + "  .linea { height: 1px; background: #1B6FE8; width: 60%; margin: 10px auto; }\n"
                + "  .cert-tipo { font-size: 10px; font-weight: bold; color: #9CA3AF; letter-spacing: 2px; text-transform: uppercase; }\n"
                + "  .cert-nombre { font-size: 28px; font-weight: bold; color: #111827; margin: 5px 0; }\n"
                + "  .cert-sub { font-size: 11px; color: #6B7280; }\n"
                + "  .cuerpo { text-align: center; margin: 25px 0; }\n"
                + "  .intro { font-size: 13px; color: #6B7280; margin: 5px 0; }\n"
                + "  .estudiante { font-size: 24px; font-weight: bold; color: #1B6FE8; margin: 8px 0; border-bottom: 1px solid #E5E7EB; display: inline-block; padding-bottom: 5px; }\n"
                + "  .proyecto { font-size: 15px; font-weight: bold; color: #111827; background: #F3F4F6; display: inline-block; padding: 5px 20px; border-radius: 20px; margin: 10px 0; }\n"
                + "  .descripcion { font-size: 12px; color: #6B7280; max-width: 80%; margin: 10px auto; line-height: 1.4; }\n"
                + "  .footer { margin-top: 20px; }\n"
                + "  .footer-tabla { width: 100%; border-collapse: collapse; }\n"
                + "  .footer-tabla td { text-align: center; width: 33%; padding-top: 10px; vertical-align: top; }\n"
                + "  .firma-linea { width: 150px; height: 1px; background: #D1D5DB; margin: 5px auto; }\n"
                + "  .firma-nombre { font-size: 12px; font-weight: bold; color: #374151; }\n"
                + "  .empresa-nombre { font-size: 10px; color: #9CA3AF; }\n"
                + "  .fecha-label { font-size: 9px; color: #9CA3AF; text-transform: uppercase; letter-spacing: 1px; }\n"
                + "  .fecha-valor { font-size: 12px; font-weight: bold; color: #374151; }\n"
                + "  .codigo-label { font-size: 9px; color: #9CA3AF; }\n"
                + "  .codigo-valor { font-size: 9px; color: #6B7280; font-family: monospace; background: #F9FAFB; padding: 2px 6px; border-radius: 4px; }\n"
                + "  .icono { font-size: 24px; margin-bottom: 5px; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"certificado\">\n"
                + "  <div class=\"esquina esquina-tl\"></div>\n"
                + "  <div class=\"esquina esquina-tr\"></div>\n"
                + "  <div class=\"esquina esquina-bl\"></div>\n"
                + "  <div class=\"esquina esquina-br\"></div>\n"
                + "  \n"
                + "  <div class=\"header\">\n"
                + "    <div class=\"logo-texto\">linkuy</div>\n"
                + "    <div class=\"linea\"></div>\n"
                + "    <div class=\"cert-tipo\">CERTIFICADO DE PARTICIPACION</div>\n"
                + "    <div class=\"cert-nombre\">linkuy</div>\n"
                + "    <div class=\"cert-sub\">Plataforma de vinculacion academico-empresarial · Cajamarca, Peru</div>\n"
                + "  </div>\n"
                + "  \n"
                + "  <div class=\"cuerpo\">\n"
                + "    <div class=\"intro\">Este certificado se otorga a</div>\n"
                + "    <div class=\"estudiante\">" + escapeHtml(nombreEstudiante) + "</div>\n"
                + "    <div class=\"intro\">por su participacion y culminacion exitosa del proyecto</div>\n"
                + "    <div class=\"proyecto\">" + escapeHtml(tituloProyecto) + "</div>\n"
                + "    <div class=\"descripcion\">" + escapeHtml(descripcion) + "</div>\n"
                + "  </div>\n"
                + "  \n"
                + "  <div class=\"footer\">\n"
                + "    <table class=\"footer-tabla\" cellspacing=\"0\" cellpadding=\"0\">\n"
                + "      <tr>\n"
                + "        <td>\n"
                + "          " + firmaHtml + "\n"
                + "          <div class=\"firma-linea\"></div>\n"
                + "          <div class=\"firma-nombre\">" + escapeHtml(gerente) + "</div>\n"
                + "          <div class=\"empresa-nombre\">" + escapeHtml(nombreMype) + "</div>\n"
                + "        </td>\n"
                + "        <td>\n"
                + "          <div class=\"icono\">📅</div>\n"
                + "          <div class=\"fecha-label\">FECHA DE EMISION</div>\n"
                + "          <div class=\"fecha-valor\">" + fecha + "</div>\n"
                + "        </td>\n"
                + "        <td>\n"
                + "          <div class=\"icono\">🔒</div>\n"
                + "          <div class=\"codigo-label\">VERIFICACION DIGITAL</div>\n"
                + "          <div class=\"codigo-valor\">" + codigo + "</div>\n"
                + "        </td>\n"
                + "      </tr>\n"
                + "    </table>\n"
                + "  </div>\n"
                + "</div>\n"
                + "</body>\n"
                + "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("á", "&aacute;")
                .replace("é", "&eacute;")
                .replace("í", "&iacute;")
                .replace("ó", "&oacute;")
                .replace("ú", "&uacute;")
                .replace("ñ", "&ntilde;")
                .replace("Á", "&Aacute;")
                .replace("É", "&Eacute;")
                .replace("Í", "&Iacute;")
                .replace("Ó", "&Oacute;")
                .replace("Ú", "&Uacute;")
                .replace("Ñ", "&Ntilde;");
    }
}