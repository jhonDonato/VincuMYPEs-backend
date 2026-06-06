package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.proyectos.application.dto.PostulacionAdminResponse;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.infrastructure.spec.PostulacionSpec;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostulacionAdminService {

    private final PostulacionRepository postulacionRepository;

    public Page<PostulacionAdminResponse> buscar(
            Long proyectoId,
            List<EstadoPostulacion> estados,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estudiante,
            String mype,
            String area,
            int page,
            int size,
            String sort) {

        boolean sortEspecial = "sinPreseleccionados".equalsIgnoreCase(sort);

        Specification<Postulacion> spec = PostulacionSpec.build(
                proyectoId, estados, fechaDesde, fechaHasta, estudiante, mype, area);

        PageRequest pageable;
        if (sortEspecial) {
            spec = spec.and(PostulacionSpec.ordenSinPreseleccionados());
            pageable = PageRequest.of(page, size);
        } else {
            pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "fechaPostulacion"));
        }

        Page<Postulacion> resultado = postulacionRepository.findAll(spec, pageable);

        Set<Long> proyectosConPreseleccionado = new HashSet<>();
        if (!resultado.isEmpty()) {
            List<Long> proyIds = resultado.getContent().stream()
                    .map(p -> p.getProyecto().getId())
                    .distinct()
                    .toList();
            proyectosConPreseleccionado = new HashSet<>(
                    postulacionRepository.findProyectoIdsConEstado(
                            proyIds, EstadoPostulacion.PRESELECCIONADO));
        }

        final Set<Long> withPresel = proyectosConPreseleccionado;
        return resultado.map(p -> toResponse(p, withPresel.contains(p.getProyecto().getId())));
    }

    private PostulacionAdminResponse toResponse(Postulacion p, boolean tienePreseleccionado) {
        return new PostulacionAdminResponse(
                p.getId(),
                p.getEstudiante().getId(),
                p.getEstudiante().getUsuario().getNombre(),
                p.getEstudiante().getUsuario().getEmail(),
                p.getProyecto().getId(),
                p.getProyecto().getTitulo(),
                p.getProyecto().getAreaSistemas().name(),
                p.getProyecto().getMype().getId(),
                p.getProyecto().getMype().getNombreComercial(),
                p.getFechaPostulacion(),
                p.getEstado().name(),
                p.getFechaRespuesta(),
                p.getFechaLimiteConfirmacion(),
                tienePreseleccionado
        );
    }
}
