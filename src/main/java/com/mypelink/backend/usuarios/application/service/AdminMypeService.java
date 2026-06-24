package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.MypePendienteResponse;
import com.mypelink.backend.usuarios.domain.model.EstadoMype;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMypeService {

    private final MypeRepository mypeRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<MypePendienteResponse> listarPorEstado(EstadoMype estado) {
        return mypeRepository.findByEstado(estado)
                .stream()
                .map(this::toPendienteResponse)
                .toList();
    }

    @Transactional
    public void aprobarMype(Long mypeId) {
        Mype mype = mypeRepository.findById(mypeId)
                .orElseThrow(() -> new ResourceNotFoundException("MYPE no encontrado", mypeId));

        if (mype.getEstado() != EstadoMype.PENDIENTE) {
            throw new BusinessException("La MYPE no está en estado pendiente", HttpStatus.BAD_REQUEST);
        }

        mype.setEstado(EstadoMype.APROBADO);
        mypeRepository.save(mype);

        enviarCorreoAprobacion(mype);
    }

    @Transactional
    public void rechazarMype(Long mypeId) {
        Mype mype = mypeRepository.findById(mypeId)
                .orElseThrow(() -> new ResourceNotFoundException("MYPE no encontrado", mypeId));

        if (mype.getEstado() != EstadoMype.PENDIENTE) {
            throw new BusinessException("La MYPE no está en estado pendiente", HttpStatus.BAD_REQUEST);
        }

        mype.setEstado(EstadoMype.RECHAZADO);
        mypeRepository.save(mype);

        enviarCorreoRechazo(mype);
    }

    private void enviarCorreoAprobacion(Mype mype) {
        try {
            String email = mype.getUsuario().getEmail();
            String nombre = mype.getUsuario().getNombre();
            String nombreComercial = mype.getNombreComercial();

            emailService.enviarCorreoNotificacion(
                    email,
                    "Cuenta aprobada — " + nombreComercial,
                    "Tu cuenta ha sido aprobada. Ya puedes iniciar sesión en Linkuy y empezar a publicar proyectos.",
                    nombre);
            log.info("Correo de aprobación enviado a {} ({})", email, nombreComercial);
        } catch (Exception e) {
            log.error("Error al enviar correo de aprobación: {}", e.getMessage());
        }
    }

    private void enviarCorreoRechazo(Mype mype) {
        try {
            String email = mype.getUsuario().getEmail();
            String nombre = mype.getUsuario().getNombre();
            String nombreComercial = mype.getNombreComercial();

            emailService.enviarCorreoNotificacion(
                    email,
                    "Cuenta rechazada — " + nombreComercial,
                    "Tu cuenta no ha sido aprobada. Si crees que esto es un error, contáctanos.",
                    nombre);
            log.info("Correo de rechazo enviado a {} ({})", email, nombreComercial);
        } catch (Exception e) {
            log.error("Error al enviar correo de rechazo: {}", e.getMessage());
        }
    }

    private MypePendienteResponse toPendienteResponse(Mype mype) {
        return new MypePendienteResponse(
                mype.getId(),
                mype.getUsuario().getId(),
                mype.getUsuario().getNombre(),
                mype.getUsuario().getEmail(),
                mype.getNombreComercial(),
                mype.getRazonSocial(),
                mype.getRuc(),
                mype.getRubro(),
                mype.getEstado().name(),
                mype.getFechaRegistro());
    }
}
