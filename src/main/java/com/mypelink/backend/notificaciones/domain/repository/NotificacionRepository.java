package com.mypelink.backend.notificaciones.domain.repository;

import com.mypelink.backend.notificaciones.domain.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    
    List<Notificacion> findByUsuarioEmailOrderByFechaCreacionDesc(String email);
    
    List<Notificacion> findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc(String email);
}
