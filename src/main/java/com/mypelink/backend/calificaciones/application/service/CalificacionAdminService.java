// com.mypelink.backend.calificaciones.application.service.CalificacionAdminService.java
package com.mypelink.backend.calificaciones.application.service;

import com.mypelink.backend.calificaciones.application.dto.CalificacionAdminResponse;
import com.mypelink.backend.calificaciones.application.dto.EditarCalificacionRequest;
import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalificacionAdminService {

    private final CalificacionRepository calificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<CalificacionAdminResponse> listarTodasCalificaciones(Pageable pageable, String emailAdmin) {
        validarAdmin(emailAdmin);

        return calificacionRepository.findAll(pageable).map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public CalificacionAdminResponse obtenerCalificacion(Long id, String emailAdmin) {
        validarAdmin(emailAdmin);

        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada", id));
        return toAdminResponse(calificacion);
    }

    @Transactional
    public CalificacionAdminResponse editarCalificacion(
            Long id,
            EditarCalificacionRequest request,
            String emailAdmin,
            String ipOrigen) {

        validarAdmin(emailAdmin);

        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada", id));

        Integer puntuacionAnterior = calificacion.getPuntuacion();
        calificacion.setPuntuacion(request.nuevaPuntuacion());
        calificacion = calificacionRepository.save(calificacion);

        log.info("Admin {} editó calificación ID {} de {} a {}. Motivo: {}",
                emailAdmin, id, puntuacionAnterior, request.nuevaPuntuacion(),
                request.motivoEdicion() != null ? request.motivoEdicion() : "Sin motivo");

        return toAdminResponse(calificacion);
    }

    @Transactional
    public void eliminarCalificacion(Long id, String emailAdmin, String ipOrigen) {
        validarAdmin(emailAdmin);

        Calificacion calificacion = calificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada", id));

        calificacionRepository.delete(calificacion);

        log.warn("Admin {} eliminó calificación ID {} (proyecto: {}, calificador: {}, calificado: {})",
                emailAdmin, id, calificacion.getProyecto().getId(),
                calificacion.getCalificador().getId(), calificacion.getCalificado().getId());
    }

    @Transactional(readOnly = true)
    public CalificacionAdminResponse obtenerCalificacionPorProyectoYUsuarios(
            Long proyectoId, Long calificadorId, Long calificadoId, String emailAdmin) {
        validarAdmin(emailAdmin);

        Calificacion calificacion = calificacionRepository
                .findByProyectoIdAndCalificadorIdAndCalificadoId(proyectoId, calificadorId, calificadoId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Calificación no encontrada"));

        return toAdminResponse(calificacion);
    }

    private void validarAdmin(String email) {
        Usuario admin = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (!admin.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("Acceso denegado. Se requiere rol ADMIN", HttpStatus.FORBIDDEN);
        }
    }

    private CalificacionAdminResponse toAdminResponse(Calificacion c) {
        return new CalificacionAdminResponse(
                c.getId(),
                c.getProyecto().getId(),
                c.getProyecto().getTitulo(),
                c.getCalificador().getId(),
                c.getCalificador().getNombre(),
                c.getCalificador().getRol().getNombre().replace("ROLE_", ""),
                c.getCalificado().getId(),
                c.getCalificado().getNombre(),
                c.getCalificado().getRol().getNombre().replace("ROLE_", ""),
                c.getPuntuacion(),
                c.getCreatedAt(),
                "/api/proyectos/" + c.getProyecto().getId()
        );
    }
}