package com.mypelink.backend.comunicacion.domain.repository;

import com.mypelink.backend.comunicacion.domain.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("SELECT m FROM Mensaje m JOIN FETCH m.remitente WHERE m.conversacion.id = :conversacionId ORDER BY m.fechaEnvio ASC")
    List<Mensaje> findByConversacionId(@Param("conversacionId") Long conversacionId);

    long countByConversacionIdAndLeidoFalseAndRemitenteIdNot(Long conversacionId, Long remitenteId);
}