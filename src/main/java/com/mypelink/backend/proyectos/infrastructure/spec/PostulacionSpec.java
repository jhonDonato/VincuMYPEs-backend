package com.mypelink.backend.proyectos.infrastructure.spec;

import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PostulacionSpec {

    private PostulacionSpec() {}

    /**
     * Builds a composed Specification for dynamic filtering of Postulacion.
     * Fetch joins are added only on the data query (not the count query Spring
     * generates for pagination), keyed on query.getResultType() != Long.class.
     */
    public static Specification<Postulacion> build(
            Long proyectoId,
            List<EstadoPostulacion> estados,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estudiante,
            String mype,
            String area) {

        return (root, query, cb) -> {
            Join<Postulacion, Proyecto> proyJoin;
            Join<Proyecto, Mype> mypeJoin;
            Join<Postulacion, Estudiante> estJoin;
            Join<Estudiante, Usuario> estUsuJoin;

            if (Long.class != query.getResultType()) {
                @SuppressWarnings("unchecked")
                Join<Postulacion, Proyecto> pf =
                        (Join<Postulacion, Proyecto>)(Object) root.fetch("proyecto", JoinType.INNER);
                proyJoin = pf;

                @SuppressWarnings("unchecked")
                Join<Proyecto, Mype> mf =
                        (Join<Proyecto, Mype>)(Object) proyJoin.fetch("mype", JoinType.INNER);
                mypeJoin = mf;

                @SuppressWarnings("unchecked")
                Join<Postulacion, Estudiante> ef =
                        (Join<Postulacion, Estudiante>)(Object) root.fetch("estudiante", JoinType.INNER);
                estJoin = ef;

                @SuppressWarnings("unchecked")
                Join<Estudiante, Usuario> euf =
                        (Join<Estudiante, Usuario>)(Object) estJoin.fetch("usuario", JoinType.INNER);
                estUsuJoin = euf;

                query.distinct(true);
            } else {
                proyJoin = root.join("proyecto", JoinType.INNER);
                mypeJoin = proyJoin.join("mype", JoinType.INNER);
                estJoin = root.join("estudiante", JoinType.INNER);
                estUsuJoin = estJoin.join("usuario", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (proyectoId != null) {
                predicates.add(cb.equal(proyJoin.get("id"), proyectoId));
            }
            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fechaPostulacion"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("fechaPostulacion"), fechaHasta.atTime(23, 59, 59)));
            }
            if (estudiante != null && !estudiante.isBlank()) {
                String pattern = "%" + estudiante.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(estUsuJoin.get("nombre")), pattern),
                        cb.like(cb.lower(estUsuJoin.get("email")), pattern)
                ));
            }
            if (mype != null && !mype.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(mypeJoin.get("nombreComercial")),
                        "%" + mype.toLowerCase() + "%"));
            }
            if (area != null && !area.isBlank()) {
                try {
                    predicates.add(cb.equal(
                            proyJoin.get("areaSistemas"), AreaSistemas.valueOf(area)));
                } catch (IllegalArgumentException e) {
                    predicates.add(cb.isNull(root.get("id")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Adds ORDER BY: projects without any PRESELECCIONADO postulacion first,
     * then by fechaPostulacion DESC. Applies only on data query (not count).
     */
    public static Specification<Postulacion> ordenSinPreseleccionados() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                Subquery<Long> subq = query.subquery(Long.class);
                Root<Postulacion> sub = subq.from(Postulacion.class);
                subq.select(cb.count(sub))
                        .where(
                                cb.equal(sub.get("proyecto"), root.get("proyecto")),
                                cb.equal(sub.get("estado"), EstadoPostulacion.PRESELECCIONADO)
                        );
                query.orderBy(
                        cb.asc(subq),
                        cb.desc(root.get("fechaPostulacion"))
                );
            }
            return cb.conjunction();
        };
    }
}
