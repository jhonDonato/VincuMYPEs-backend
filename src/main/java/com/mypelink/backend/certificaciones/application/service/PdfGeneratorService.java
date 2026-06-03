package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.usuarios.domain.model.Mype;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfGeneratorService {

    public byte[] generarCertificadoPDF(Certificado certificado, Proyecto proyecto, Mype mype, String firmaBase64) {
        System.out.println("=== DEPURACIÓN PDF ===");
        System.out.println("Estudiante: " + certificado.getEstudiante().getUsuario().getNombre());
        System.out.println("Proyecto: " + proyecto.getTitulo());
        System.out.println("Descripción: " + certificado.getDescripcionCertificado());
        System.out.println("Título certificado: " + certificado.getTituloCertificado());
        System.out.println("MYPE: " + mype.getNombreComercial());
        System.out.println("Firma recibida: " + (firmaBase64 != null ? "Sí (longitud " + firmaBase64.length() + ")" : "No"));

        String html = buildHtml(certificado, proyecto, mype, firmaBase64);
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

    private String buildHtml(Certificado certificado, Proyecto proyecto, Mype mype, String firmaBase64) {
        String nombreEstudiante = certificado.getEstudiante().getUsuario().getNombre();
        String tituloProyecto = proyecto.getTitulo();
        String nombreMype = mype.getNombreComercial();
        String gerente = mype.getNombreComercial(); // o un campo específico de gerente
        String descripcion = certificado.getDescripcionCertificado() != null ? certificado.getDescripcionCertificado() : "";
        String fecha = certificado.getFechaEmision().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        String codigo = certificado.getCodigo();

        // Generar HTML de la firma
        String firmaHtml;
        if (firmaBase64 != null && !firmaBase64.isEmpty()) {
            firmaHtml = "<img src=\"" + firmaBase64 + "\" style=\"height: 36px; margin-bottom: 4px; object-fit: contain;\" />";
        } else {
            firmaHtml = "<div style=\"height: 24px; margin-bottom: 4px; font-size: 10px; color: #9CA3AF; font-style: italic;\">firma digital</div>";
        }

        String html = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"/></head>
        <body style="font-family: 'Outfit', Georgia, serif; margin: 0; padding: 40px;">
        <div style="max-width: 800px; margin: 0 auto; border: 1px solid #E5E7EB; border-radius: 12px; padding: 40px; position: relative;">
            <div style="position: absolute; inset: 8px; border: 2px solid #1B6FE8; border-radius: 8px; opacity: 0.12;"></div>
            <div style="position: absolute; top: 14px; left: 14px; width: 24px; height: 24px; border-top: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; border-radius: 4px 0 0 0;"></div>
            <div style="position: absolute; top: 14px; right: 14px; width: 24px; height: 24px; border-top: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; border-radius: 0 4px 0 0;"></div>
            <div style="position: absolute; bottom: 14px; left: 14px; width: 24px; height: 24px; border-bottom: 2px solid #1B6FE8; border-left: 2px solid #1B6FE8; border-radius: 0 0 0 4px;"></div>
            <div style="position: absolute; bottom: 14px; right: 14px; width: 24px; height: 24px; border-bottom: 2px solid #1B6FE8; border-right: 2px solid #1B6FE8; border-radius: 0 0 4px 0;"></div>

            <div style="text-align: center; margin-bottom: 24px;">
                <div style="display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 14px;">
                    <svg width="32" height="32" viewBox="0 0 100 100"><path d="M20 15 L50 85 L65 85 L35 15 Z" fill="#1B6FE8"/><path d="M80 15 L50 85 L35 85 L65 15 Z" fill="#06B6D4" opacity="0.9"/><circle cx="50" cy="85" r="8" fill="#F97316"/></svg>
                    <span style="font-size: 18px; font-weight: 700; color: #1E3A5F;">Vincu<span style="color:#06B6D4;">MYPEs</span></span>
                </div>
                <div style="height: 1px; background: linear-gradient(90deg,transparent,#1B6FE8,transparent); width: 70%%; margin: 0 auto 14px;"></div>
                <div style="font-size: 10px; font-weight: 700; color: #9CA3AF; text-transform: uppercase; letter-spacing: 2px;">Certificado de participación</div>
                <div style="font-size: 26px; font-weight: 700; color: #111827;">VincuMYPEs</div>
                <div style="font-size: 12px; color: #6B7280;">Plataforma de vinculación académico-empresarial · Cajamarca, Perú</div>
            </div>

            <div style="text-align: center; margin-bottom: 24px;">
                <p style="font-size: 13px; color: #6B7280; margin-bottom: 6px;">Este certificado se otorga a</p>
                <div style="font-size: 24px; font-weight: 700; color: #1B6FE8; font-style: italic; border-bottom: 2px solid #E5E7EB; padding-bottom: 6px; display: inline-block; margin-bottom: 10px;">%s</div>
                <p style="font-size: 13px; color: #6B7280; margin-bottom: 6px;">por su participación y culminación exitosa del proyecto</p>
                <div style="font-size: 15px; font-weight: 600; color: #111827; margin-bottom: 8px;">%s</div>
                <p style="font-size: 12px; color: #9CA3AF; max-width: 480px; margin: 0 auto; line-height: 1.6;">%s</p>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: flex-end; padding-top: 18px; border-top: 1px solid #F3F4F6;">
                <div style="text-align: center; flex: 1;">
                    %s
                    <div style="width: 100px; height: 1px; background: #D1D5DB; margin: 0 auto 6px;"></div>
                    <div style="font-size: 12px; font-weight: 600; color: #374151;">%s</div>
                    <div style="font-size: 10px; color: #9CA3AF;">%s</div>
                </div>
                <div style="display: flex; flex-direction: column; align-items: center; gap: 6px;">
                    <div style="width: 50px; height: 50px; border-radius: 50%%; border: 2px solid #1B6FE8; display: flex; align-items: center; justify-content: center;">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#1B6FE8" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
                    </div>
                    <div style="text-align: center;">
                        <div style="font-size: 9px; color: #9CA3AF; text-transform: uppercase; letter-spacing: 1px;">Fecha de emisión</div>
                        <div style="font-size: 12px; font-weight: 600; color: #374151;">%s</div>
                    </div>
                </div>
                <div style="flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px;">
                    <div style="width: 44px; height: 44px; background: #F3F4F6; border: 1px solid #E5E7EB; border-radius: 4px; display: flex; align-items: center; justify-content: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#D1D5DB"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>
                    </div>
                    <div style="font-size: 10px; color: #9CA3AF;">Verificación digital</div>
                    <div style="font-size: 8px; color: #D1D5DB; font-family: monospace;">%s</div>
                </div>
            </div>
        </div>
        </body>
        </html>
        """;

        return String.format(html,
                nombreEstudiante,       // %s
                tituloProyecto,         // %s
                descripcion,            // %s
                firmaHtml,              // %s (nuevo)
                gerente,                // %s
                nombreMype,             // %s
                fecha,                  // %s
                codigo                  // %s
        );
    }
}
