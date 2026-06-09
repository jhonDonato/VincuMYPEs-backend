package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.certificaciones.application.service.PdfGeneratorService;
import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceTest {

    @InjectMocks
    private PdfGeneratorService pdfGeneratorService;

    private Certificado certificado;
    private Proyecto proyecto;
    private Mype mype;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Juan Perez").build();
        Estudiante estudiante = Estudiante.builder().id(1L).usuario(usuario).build();
        certificado = Certificado.builder()
                .id(1L).codigo("CERT-001")
                .estudiante(estudiante)
                .fechaEmision(LocalDate.now()).build();
        proyecto = Proyecto.builder()
                .id(1L).titulo("Proyecto Test").descripcion("Desc")
                .areaSistemas(AreaSistemas.DESARROLLO_WEB).build();
        mype = Mype.builder().id(1L).nombreComercial("MYPE Test").build();
    }

    @Test
    void generarCertificadoPDF_Success() {
        byte[] pdf = pdfGeneratorService.generarCertificadoPDF(
                certificado, proyecto, mype, null, "Gerente Test");

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
