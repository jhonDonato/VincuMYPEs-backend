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
                                        String firmaUrl, String gerenteNombre) { // ← CAMBIADO firmaBase64 → firmaUrl
        log.debug("[PDF] Generando - estudiante={}, proyecto={}",
                certificado.getEstudiante().getUsuario().getNombre(),
                proyecto.getTitulo());

        String html = buildHtml(certificado, proyecto, mype, firmaUrl, gerenteNombre); // ← CAMBIADO

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
                             String firmaUrl, String gerenteNombre) { // ← CAMBIADO

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

        // ═══════════════════════════════════════════
        // ✅ NUEVO: Manejar firma como URL de S3 o base64
        // ═══════════════════════════════════════════
        String firmaHtml;
        if (firmaUrl != null && !firmaUrl.isBlank()) {
            if (firmaUrl.startsWith("http")) {
                // Es una URL de S3
                firmaHtml = "<img src=\"" + firmaUrl + "\" " +
                        "style=\"display: block; margin: 0 auto; " +
                        "height: 50px; width: auto;\" " +
                        "alt=\"Firma\" />";
            } else if (firmaUrl.startsWith("data:image")) {
                // Es base64 con prefijo
                firmaHtml = "<img src=\"" + firmaUrl + "\" " +
                        "style=\"display: block; margin: 0 auto; " +
                        "height: 50px; width: auto;\" />";
            } else {
                // Es base64 sin prefijo
                firmaHtml = "<img src=\"data:image/png;base64," + firmaUrl + "\" " +
                        "style=\"display: block; margin: 0 auto; " +
                        "height: 50px; width: auto;\" />";
            }
        } else {
            firmaHtml = "<div style=\"height: 50px;\"></div>";
        }

        // ═══════════════════════════════════════════
        // El resto del HTML se mantiene IGUAL
        // ═══════════════════════════════════════════
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\"/>\n"
                + "<style>\n"
                + "  @page {\n"
                + "    size: 297mm 210mm; /* A4 Landscape exacto */\n"
                + "    margin: 0;\n"
                + "  }\n"
                + "  * {\n"
                + "    margin: 0;\n"
                + "    padding: 0;\n"
                + "    box-sizing: border-box;\n"
                + "  }\n"
                + "  body {\n"
                + "    width: 297mm;\n"
                + "    height: 210mm;\n"
                + "    font-family: 'Times New Roman', Georgia, serif;\n"
                + "    background: white;\n"
                + "  }\n"
                + "  .page-container {\n"
                + "    width: 297mm;\n"
                + "    height: 210mm;\n"
                + "    padding: 12.5mm 13.5mm;\n"
                + "    box-sizing: border-box;\n"
                + "  }\n"
                + "  .certificado {\n"
                + "    width: 100%;\n"
                + "    height: 100%;\n"
                + "    border: 2px solid #1B6FE8;\n"
                + "    border-radius: 16px;\n"
                + "    position: relative;\n"
                + "    background: white;\n"
                + "    text-align: center;\n"
                + "  }\n"
                + "  .esquina {\n"
                + "    position: absolute;\n"
                + "    width: 30px;\n"
                + "    height: 30px;\n"
                + "  }\n"
                + "  .esquina-tl { top: 12px; left: 12px; border-top: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; border-radius: 8px 0 0 0; }\n"
                + "  .esquina-tr { top: 12px; right: 12px; border-top: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; border-radius: 0 8px 0 0; }\n"
                + "  .esquina-bl { bottom: 12px; left: 12px; border-bottom: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; border-radius: 0 0 0 8px; }\n"
                + "  .esquina-br { bottom: 12px; right: 12px; border-bottom: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; border-radius: 0 0 8px 0; }\n"
                + "  .header { padding-top: 40px; margin-bottom: 30px; }\n"
                + "  .logo-texto { font-size: 26px; font-weight: bold; color: #1E3A5F; }\n"
                + "  .linea { height: 1px; background: #1B6FE8; width: 50%; margin: 12px auto; }\n"
                + "  .cert-tipo { font-size: 11px; font-weight: bold; color: #9CA3AF; letter-spacing: 2px; text-transform: uppercase; }\n"
                + "  .cert-nombre { font-size: 34px; font-weight: bold; color: #111827; margin: 8px 0; }\n"
                + "  .cert-sub { font-size: 12px; color: #6B7280; }\n"
                + "  .cuerpo { margin-bottom: 40px; }\n"
                + "  .intro { font-size: 14px; color: #6B7280; margin: 10px 0; }\n"
                + "  .estudiante { font-size: 30px; font-weight: bold; color: #1B6FE8; margin: 12px 0; border-bottom: 1px solid #E5E7EB; display: inline-block; padding-bottom: 5px; }\n"
                + "  .proyecto { font-size: 18px; font-weight: bold; color: #111827; background: #F3F4F6; display: inline-block; padding: 8px 24px; border-radius: 20px; margin: 12px 0; }\n"
                + "  .descripcion { font-size: 14px; color: #6B7280; max-width: 80%; margin: 15px auto; line-height: 1.5; }\n"
                + "  .footer { position: absolute; bottom: 35px; width: 100%; }\n"
                + "  .footer-tabla { width: 90%; margin: 0 auto; border-collapse: collapse; table-layout: fixed; }\n"
                + "  .footer-tabla td { text-align: center; width: 33.3%; vertical-align: bottom; }\n"
                + "  .firma-container { min-height: 50px; margin-bottom: 5px; }\n"
                + "  .firma-linea { width: 180px; height: 1px; background: #D1D5DB; margin: 5px auto; }\n"
                + "  .firma-nombre { font-size: 14px; font-weight: bold; color: #374151; }\n"
                + "  .empresa-nombre { font-size: 11px; color: #9CA3AF; }\n"
                + "  .fecha-label, .codigo-label { font-size: 10px; color: #9CA3AF; text-transform: uppercase; letter-spacing: 1px; margin-top: 5px; margin-bottom: 5px; }\n"
                + "  .fecha-valor { font-size: 14px; font-weight: bold; color: #374151; }\n"
                + "  .codigo-valor { font-size: 12px; color: #6B7280; font-family: monospace; background: #F9FAFB; padding: 4px 8px; border-radius: 4px; display: inline-block; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div class=\"page-container\">\n"
                + "  <div class=\"certificado\">\n"
                + "    <div class=\"esquina esquina-tl\"></div>\n"
                + "    <div class=\"esquina esquina-tr\"></div>\n"
                + "    <div class=\"esquina esquina-bl\"></div>\n"
                + "    <div class=\"esquina esquina-br\"></div>\n"
                + "    \n"
                + "    <div class=\"header\">\n"
                + "      <div class=\"logo-texto\">linkuy</div>\n"
                + "      <div class=\"linea\"></div>\n"
                + "      <div class=\"cert-tipo\">CERTIFICADO DE PARTICIPACION</div>\n"
                + "      <div class=\"cert-nombre\">linkuy</div>\n"
                + "      <div class=\"cert-sub\">Plataforma de vinculacion academico-empresarial · Cajamarca, Peru</div>\n"
                + "    </div>\n"
                + "    \n"
                + "    <div class=\"cuerpo\">\n"
                + "      <div class=\"intro\">Este certificado se otorga a</div>\n"
                + "      <div class=\"estudiante\">" + escapeHtml(nombreEstudiante) + "</div>\n"
                + "      <div class=\"intro\">por su participacion y culminacion exitosa del proyecto</div>\n"
                + "      <div class=\"proyecto\">" + escapeHtml(tituloProyecto) + "</div>\n"
                + "      <div class=\"descripcion\">" + escapeHtml(descripcion) + "</div>\n"
                + "    </div>\n"
                + "    \n"
                + "    <div class=\"footer\">\n"
                + "      <table class=\"footer-tabla\" cellspacing=\"0\" cellpadding=\"0\">\n"
                + "        <tr>\n"
                + "          <td>\n"
                + "            <div class=\"firma-container\">" + firmaHtml + "</div>\n"
                + "            <div class=\"firma-linea\"></div>\n"
                + "            <div class=\"firma-nombre\">" + escapeHtml(gerente) + "</div>\n"
                + "            <div class=\"empresa-nombre\">" + escapeHtml(nombreMype) + "</div>\n"
                + "          </td>\n"
                + "          <td>\n"
                + "            <div class=\"fecha-label\">FECHA DE EMISION</div>\n"
                + "            <div class=\"fecha-valor\">" + fecha + "</div>\n"
                + "          </td>\n"
                + "          <td>\n"
                + "            <div class=\"codigo-label\">VERIFICACION DIGITAL</div>\n"
                + "            <div class=\"codigo-valor\">" + codigo + "</div>\n"
                + "          </td>\n"
                + "        </tr>\n"
                + "      </table>\n"
                + "    </div>\n"
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