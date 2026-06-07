package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m WHERE p.mype.id = :mypeId ORDER BY p.fechaCreacion DESC")
    List<Proyecto> findByMypeIdConMype(@Param("mypeId") Long mypeId);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m LEFT JOIN FETCH p.publicadoPor WHERE p.estado = :estado AND p.activo = true")
    List<Proyecto> findPublicosConMype(@Param("estado") WorkflowEstado estado);

    @Query(value = "SELECT COUNT(p) FROM Proyecto p WHERE p.estado = :estado AND p.activo = true")
    long countByEstadoAndActivoTrue(@Param("estado") WorkflowEstado estado);

    @Query("SELECT p FROM Proyecto p WHERE p.mype.id = :mypeId")
    List<Proyecto> findByMypeId(@Param("mypeId") Long mypeId);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m JOIN FETCH m.usuario WHERE p.id = :id")
    java.util.Optional<Proyecto> findByIdWithMype(@Param("id") Long id);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m ORDER BY p.fechaCreacion DESC")
    List<Proyecto> findAllConMype();
    List<Proyecto> findByEstadoAndActivoTrue(WorkflowEstado estado);

    @Query("SELECT p FROM Proyecto p WHERE p.mype.id = :mypeId AND p.tipoProyecto.id = :tipoProyectoId AND p.estado IN :estados AND p.activo = true")
    List<Proyecto> findByMypeIdAndTipoProyectoIdAndEstadoIn(
            @Param("mypeId") Long mypeId,
            @Param("tipoProyectoId") Long tipoProyectoId,
            @Param("estados") List<WorkflowEstado> estados);
    List<Proyecto> findByMypeIdAndEstado(Long mypeId, WorkflowEstado estado);

    @Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estado IN :estados AND p.activo = true")
    long countByEstadoInAndActivoTrue(@Param("estados") List<WorkflowEstado> estados);

    @Query("SELECT p.areaSistemas, COUNT(p) FROM Proyecto p GROUP BY p.areaSistemas")
    List<Object[]> countGroupByAreaSistemas();

    // ProyectoRepository.java
    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype ORDER BY p.fechaCreacion DESC")
    Page<Proyecto> findAllConMype(Pageable pageable);
}