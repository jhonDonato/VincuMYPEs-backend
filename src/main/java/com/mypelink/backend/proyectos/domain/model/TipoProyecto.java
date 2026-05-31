package com.mypelink.backend.proyectos.domain.model;

import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion_mype", columnDefinition = "TEXT")
    private String descripcionMype;

    @Column(name = "descripcion_estudiante", columnDefinition = "TEXT")
    private String descripcionEstudiante;

    @Column(nullable = false, length = 50)
    private String rama;

    @Column(name = "ciclo_minimo", nullable = false)
    private Integer cicloMinimo;

    @Column(name = "incluye_presupuesto", nullable = false)
    @Builder.Default
    private Boolean incluyePresupuesto = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ✅ Nuevos campos (todos nullable)
    @Enumerated(EnumType.STRING)
    @Column(name = "area_sistemas")
    private AreaSistemas areaSistemas;

    @Column(name = "cupos_min")
    private Integer cuposMin;

    @Column(name = "cupos_max")
    private Integer cuposMax;

    @Column(name = "dias_min")
    private Integer diasMin;

    @Column(name = "dias_sugerido")
    private Integer diasSugerido;

    @Column(length = 50)
    private String complejidad;

    @Column(name = "esfuerzo_h_pers")
    private Integer esfuerzoHPers;

    @Column(name = "alcance_incluye", columnDefinition = "TEXT")
    private String alcanceIncluye;

    @Column(name = "alcance_no_incluye", columnDefinition = "TEXT")
    private String alcanceNoIncluye;
}