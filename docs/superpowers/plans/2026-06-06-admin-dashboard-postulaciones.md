# Admin Dashboard & Postulaciones Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement two admin-only endpoints: `GET /api/admin/dashboard/stats` for real-time KPI metrics, and `GET /api/admin/postulaciones` for a paginated, filterable view of all postulaciones.

**Architecture:** New `AdminDashboardService` (in `shared/`) calls COUNT/AVG aggregation queries across multiple repositories. New `PostulacionAdminService` (in `proyectos/`) uses a `PostulacionSpec` (JpaSpecificationExecutor pattern) to compose dynamic filter predicates with fetch-join-safe pagination. Two new controllers follow the existing `Admin*Controller` naming and `hasAuthority('ROLE_ADMIN')` security pattern.

**Tech Stack:** Spring Boot 3.5, Java 21, JPA Criteria API (Specifications), Spring Data JPA (JpaSpecificationExecutor), Lombok, Spring Security (@PreAuthorize)

---

## File Map

### Created
| File | Responsibility |
|---|---|
| `shared/application/dto/DashboardStatsResponse.java` | Record DTO for dashboard stats JSON |
| `shared/application/service/AdminDashboardService.java` | Calls aggregation queries, assembles DashboardStatsResponse |
| `shared/infrastructure/rest/AdminDashboardController.java` | `GET /api/admin/dashboard/stats` |
| `proyectos/application/dto/PostulacionAdminResponse.java` | Record DTO for each postulación row |
| `proyectos/infrastructure/spec/PostulacionSpec.java` | Static factory for `Specification<Postulacion>` — filters + special sort |
| `proyectos/application/service/PostulacionAdminService.java` | Builds spec, paginates, computes tienePreseleccionado |
| `proyectos/infrastructure/rest/AdminPostulacionController.java` | `GET /api/admin/postulaciones` |

### Modified
| File | Change |
|---|---|
| `proyectos/domain/repository/PostulacionRepository.java` | Add `JpaSpecificationExecutor<Postulacion>`, `countByEstado`, `findProyectoIdsConEstado` |
| `usuarios/domain/repository/UsuarioRepository.java` | Add `countByRolNombre` |
| `calificaciones/domain/repository/CalificacionRepository.java` | Add `promedioByCalificadoRol` |
| `proyectos/domain/repository/ProyectoRepository.java` | Add `countByEstadoInAndActivoTrue`, `countGroupByAreaSistemas` |

---

## Task 1: Extend PostulacionRepository

**Files:**
- Modify: `src/main/java/com/mypelink/backend/proyectos/domain/repository/PostulacionRepository.java`

- [ ] **Step 1.1 — Add JpaSpecificationExecutor and two new query methods**

Replace the interface declaration line and add two new methods at the bottom:

```java
package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostulacionRepository
        extends JpaRepository<Postulacion, Long>,
                JpaSpecificationExecutor<Postulacion> {

    // ... all existing methods unchanged ...

    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.estado = :estado")
    long countByEstado(@Param("estado") EstadoPostulacion estado);

    @Query("SELECT DISTINCT p.proyecto.id FROM Postulacion p " +
           "WHERE p.proyecto.id IN :proyectoIds AND p.estado = :estado")
    List<Long> findProyectoIdsConEstado(
            @Param("proyectoIds") List<Long> proyectoIds,
            @Param("estado") EstadoPostulacion estado);
}
```

> Keep all existing methods. Only add `JpaSpecificationExecutor<Postulacion>` to the `extends` clause and append the two new methods.

- [ ] **Step 1.2 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS, no errors.

- [ ] **Step 1.3 — Commit**

```
git add src/main/java/com/mypelink/backend/proyectos/domain/repository/PostulacionRepository.java
git commit -m "feat: extend PostulacionRepository with JpaSpecificationExecutor and admin queries"
```

---

## Task 2: Add aggregation queries to UsuarioRepository, CalificacionRepository, ProyectoRepository

**Files:**
- Modify: `src/main/java/com/mypelink/backend/usuarios/domain/repository/UsuarioRepository.java`
- Modify: `src/main/java/com/mypelink/backend/calificaciones/domain/repository/CalificacionRepository.java`
- Modify: `src/main/java/com/mypelink/backend/proyectos/domain/repository/ProyectoRepository.java`

- [ ] **Step 2.1 — Add countByRolNombre to UsuarioRepository**

Append inside the interface:

```java
@Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.nombre = :rolNombre")
long countByRolNombre(@Param("rolNombre") String rolNombre);
```

Full file after edit:

```java
package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRole(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.nombre = :rolNombre")
    long countByRolNombre(@Param("rolNombre") String rolNombre);
}
```

- [ ] **Step 2.2 — Add promedioByCalificadoRol to CalificacionRepository**

Append inside the interface:

```java
@Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.rol.nombre = :rolNombre")
Double promedioByCalificadoRol(@Param("rolNombre") String rolNombre);
```

Full file after edit:

```java
package com.mypelink.backend.calificaciones.domain.repository;

import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    boolean existsByProyectoIdAndCalificadorIdAndCalificadoId(
            Long proyectoId, Long calificadorId, Long calificadoId);

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.id = :usuarioId")
    Double promedioDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM Calificacion c WHERE c.calificado.id = :usuarioId")
    long cantidadDeUsuario(@Param("usuarioId") Long usuarioId);

    List<Calificacion> findByProyectoIdAndCalificadorId(Long proyectoId, Long calificadorId);

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.rol.nombre = :rolNombre")
    Double promedioByCalificadoRol(@Param("rolNombre") String rolNombre);
}
```

- [ ] **Step 2.3 — Add two new queries to ProyectoRepository**

Append inside the interface:

```java
@Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estado IN :estados AND p.activo = true")
long countByEstadoInAndActivoTrue(@Param("estados") java.util.List<WorkflowEstado> estados);

@Query("SELECT p.areaSistemas, COUNT(p) FROM Proyecto p GROUP BY p.areaSistemas")
java.util.List<Object[]> countGroupByAreaSistemas();
```

> ProyectoRepository already imports `WorkflowEstado` and `List`. Keep all existing methods.

- [ ] **Step 2.4 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 2.5 — Commit**

```
git add src/main/java/com/mypelink/backend/usuarios/domain/repository/UsuarioRepository.java
git add src/main/java/com/mypelink/backend/calificaciones/domain/repository/CalificacionRepository.java
git add src/main/java/com/mypelink/backend/proyectos/domain/repository/ProyectoRepository.java
git commit -m "feat: add admin aggregation queries to repositories"
```

---

## Task 3: Create DashboardStatsResponse and PostulacionAdminResponse DTOs

**Files:**
- Create: `src/main/java/com/mypelink/backend/shared/application/dto/DashboardStatsResponse.java`
- Create: `src/main/java/com/mypelink/backend/proyectos/application/dto/PostulacionAdminResponse.java`

- [ ] **Step 3.1 — Create DashboardStatsResponse**

```java
package com.mypelink.backend.shared.application.dto;

import java.util.Map;

public record DashboardStatsResponse(
        long totalEstudiantes,
        long totalMypes,
        long totalAdmins,
        long proyectosActivos,
        long proyectosCompletados,
        long postulacionesPendientes,
        long certificadosEmitidos,
        Double promedioCalificacionMypes,
        Double promedioCalificacionEstudiantes,
        Map<String, Long> proyectosPorArea
) {}
```

- [ ] **Step 3.2 — Create PostulacionAdminResponse**

```java
package com.mypelink.backend.proyectos.application.dto;

import java.time.LocalDateTime;

public record PostulacionAdminResponse(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        String estudianteEmail,
        Long proyectoId,
        String proyectoTitulo,
        String proyectoArea,
        Long mypeId,
        String mypeNombre,
        LocalDateTime fechaPostulacion,
        String estado,
        LocalDateTime fechaRespuesta,
        LocalDateTime fechaLimiteConfirmacion,
        boolean tienePreseleccionado
) {}
```

- [ ] **Step 3.3 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 3.4 — Commit**

```
git add src/main/java/com/mypelink/backend/shared/application/dto/DashboardStatsResponse.java
git add src/main/java/com/mypelink/backend/proyectos/application/dto/PostulacionAdminResponse.java
git commit -m "feat: add DashboardStatsResponse and PostulacionAdminResponse DTOs"
```

---

## Task 4: Create PostulacionSpec

**Files:**
- Create: `src/main/java/com/mypelink/backend/proyectos/infrastructure/spec/PostulacionSpec.java`

- [ ] **Step 4.1 — Create the Specification class**

```java
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
            // --- Joins (reused as fetch joins on data query) ---
            Join<Postulacion, Proyecto> proyJoin;
            Join<Proyecto, Mype> mypeJoin;
            Join<Postulacion, Estudiante> estJoin;
            Join<Estudiante, Usuario> estUsuJoin;

            if (Long.class != query.getResultType()) {
                // Data query: use fetch joins to load associations eagerly (avoids N+1)
                @SuppressWarnings("unchecked")
                Join<Postulacion, Proyecto> pf =
                        (Join<Postulacion, Proyecto>) root.fetch("proyecto", JoinType.INNER);
                proyJoin = pf;

                @SuppressWarnings("unchecked")
                Join<Proyecto, Mype> mf =
                        (Join<Proyecto, Mype>) proyJoin.fetch("mype", JoinType.INNER);
                mypeJoin = mf;

                @SuppressWarnings("unchecked")
                Join<Postulacion, Estudiante> ef =
                        (Join<Postulacion, Estudiante>) root.fetch("estudiante", JoinType.INNER);
                estJoin = ef;

                @SuppressWarnings("unchecked")
                Join<Estudiante, Usuario> euf =
                        (Join<Estudiante, Usuario>) estJoin.fetch("usuario", JoinType.INNER);
                estUsuJoin = euf;

                query.distinct(true);
            } else {
                // Count query: plain joins, no fetching
                proyJoin = root.join("proyecto", JoinType.INNER);
                mypeJoin = proyJoin.join("mype", JoinType.INNER);
                estJoin = root.join("estudiante", JoinType.INNER);
                estUsuJoin = estJoin.join("usuario", JoinType.INNER);
            }

            // --- Predicates ---
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
                    predicates.add(cb.isNull(root.get("id"))); // returns empty page for bad area
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
```

- [ ] **Step 4.2 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 4.3 — Commit**

```
git add src/main/java/com/mypelink/backend/proyectos/infrastructure/spec/PostulacionSpec.java
git commit -m "feat: add PostulacionSpec with dynamic filter and sinPreseleccionados sort"
```

---

## Task 5: Create AdminDashboardService and AdminDashboardController

**Files:**
- Create: `src/main/java/com/mypelink/backend/shared/application/service/AdminDashboardService.java`
- Create: `src/main/java/com/mypelink/backend/shared/infrastructure/rest/AdminDashboardController.java`

- [ ] **Step 5.1 — Create AdminDashboardService**

```java
package com.mypelink.backend.shared.application.service;

import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.application.dto.DashboardStatsResponse;
import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final CertificadoRepository certificadoRepository;
    private final CalificacionRepository calificacionRepository;

    public DashboardStatsResponse getStats() {
        long totalEstudiantes = usuarioRepository.countByRolNombre("ROLE_ESTUDIANTE");
        long totalMypes       = usuarioRepository.countByRolNombre("ROLE_MYPE");
        long totalAdmins      = usuarioRepository.countByRolNombre("ROLE_ADMIN");

        long proyectosActivos = proyectoRepository.countByEstadoInAndActivoTrue(
                List.of(WorkflowEstado.PENDIENTE, WorkflowEstado.EN_DESARROLLO));
        long proyectosCompletados = proyectoRepository.countByEstadoAndActivoTrue(WorkflowEstado.COMPLETADO);

        long postulacionesPendientes = postulacionRepository.countByEstado(EstadoPostulacion.PENDIENTE);
        long certificadosEmitidos    = certificadoRepository.count();

        Double promedioMypes       = calificacionRepository.promedioByCalificadoRol("ROLE_MYPE");
        Double promedioEstudiantes = calificacionRepository.promedioByCalificadoRol("ROLE_ESTUDIANTE");

        List<Object[]> rows = proyectoRepository.countGroupByAreaSistemas();
        Map<String, Long> proyectosPorArea = new LinkedHashMap<>();
        for (Object[] row : rows) {
            proyectosPorArea.put(((AreaSistemas) row[0]).name(), (Long) row[1]);
        }

        return new DashboardStatsResponse(
                totalEstudiantes, totalMypes, totalAdmins,
                proyectosActivos, proyectosCompletados,
                postulacionesPendientes, certificadosEmitidos,
                promedioMypes, promedioEstudiantes,
                proyectosPorArea
        );
    }
}
```

- [ ] **Step 5.2 — Create AdminDashboardController**

```java
package com.mypelink.backend.shared.infrastructure.rest;

import com.mypelink.backend.shared.application.dto.DashboardStatsResponse;
import com.mypelink.backend.shared.application.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
```

- [ ] **Step 5.3 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5.4 — Commit**

```
git add src/main/java/com/mypelink/backend/shared/application/service/AdminDashboardService.java
git add src/main/java/com/mypelink/backend/shared/infrastructure/rest/AdminDashboardController.java
git commit -m "feat: add AdminDashboardService and controller for GET /api/admin/dashboard/stats"
```

---

## Task 6: Create PostulacionAdminService and AdminPostulacionController

**Files:**
- Create: `src/main/java/com/mypelink/backend/proyectos/application/service/PostulacionAdminService.java`
- Create: `src/main/java/com/mypelink/backend/proyectos/infrastructure/rest/AdminPostulacionController.java`

- [ ] **Step 6.1 — Create PostulacionAdminService**

```java
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
            // Custom ORDER BY via subquery; Pageable must be unsorted to avoid conflict
            spec = spec.and(PostulacionSpec.ordenSinPreseleccionados());
            pageable = PageRequest.of(page, size);
        } else {
            // Default: fechaPostulacion DESC (or any Spring-parseable sort string)
            pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "fechaPostulacion"));
        }

        Page<Postulacion> resultado = postulacionRepository.findAll(spec, pageable);

        // Two-query approach to compute tienePreseleccionado without N+1
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
```

- [ ] **Step 6.2 — Create AdminPostulacionController**

```java
package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.PostulacionAdminResponse;
import com.mypelink.backend.proyectos.application.service.PostulacionAdminService;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/postulaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPostulacionController {

    private final PostulacionAdminService service;

    @GetMapping
    public ResponseEntity<Page<PostulacionAdminResponse>> buscar(
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) List<EstadoPostulacion> estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estudiante,
            @RequestParam(required = false) String mype,
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPostulacion,desc") String sort) {

        return ResponseEntity.ok(
                service.buscar(proyectoId, estado, fechaDesde, fechaHasta,
                        estudiante, mype, area, page, size, sort));
    }
}
```

- [ ] **Step 6.3 — Verify compile**

```
mvn clean compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 6.4 — Commit**

```
git add src/main/java/com/mypelink/backend/proyectos/application/service/PostulacionAdminService.java
git add src/main/java/com/mypelink/backend/proyectos/infrastructure/rest/AdminPostulacionController.java
git commit -m "feat: add PostulacionAdminService and controller for GET /api/admin/postulaciones"
```

---

## Task 7: End-to-end verification

- [ ] **Step 7.1 — Start the application**

```
mvn spring-boot:run
```
Wait until you see `Started BackendApplication` in the console.

- [ ] **Step 7.2 — Obtain an ADMIN JWT token**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<admin_email>","password":"<admin_password>"}' \
  | jq '.token'
```
Copy the token value. Use it as `$TOKEN` in the commands below.

- [ ] **Step 7.3 — Test dashboard stats**

```bash
curl -s http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Expected response shape:
```json
{
  "totalEstudiantes": 12,
  "totalMypes": 5,
  "totalAdmins": 1,
  "proyectosActivos": 3,
  "proyectosCompletados": 2,
  "postulacionesPendientes": 8,
  "certificadosEmitidos": 4,
  "promedioCalificacionMypes": 4.2,
  "promedioCalificacionEstudiantes": 3.8,
  "proyectosPorArea": {
    "DESARROLLO_WEB": 3,
    "SOPORTE_TI": 2
  }
}
```
Verify: all fields present, counts are numbers, `promedioCalificacion*` is `null` if no calificaciones exist.

- [ ] **Step 7.4 — Test postulaciones sin filtros (paginado)**

```bash
curl -s "http://localhost:8080/api/admin/postulaciones?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" | jq '.content[0]'
```

Expected: one `PostulacionAdminResponse` object with all fields: `id`, `estudianteId`, `estudianteNombre`, `estudianteEmail`, `proyectoId`, `proyectoTitulo`, `proyectoArea`, `mypeId`, `mypeNombre`, `fechaPostulacion`, `estado`, `fechaRespuesta`, `fechaLimiteConfirmacion`, `tienePreseleccionado`.

- [ ] **Step 7.5 — Test filtro por estado múltiple**

```bash
curl -s "http://localhost:8080/api/admin/postulaciones?estado=PENDIENTE&estado=PRESELECCIONADO&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq '[.content[].estado] | unique'
```

Expected: `["PENDIENTE","PRESELECCIONADO"]` or a subset — no other estados.

- [ ] **Step 7.6 — Test filtro por texto de estudiante**

```bash
curl -s "http://localhost:8080/api/admin/postulaciones?estudiante=juan" \
  -H "Authorization: Bearer $TOKEN" | jq '[.content[] | {nombre: .estudianteNombre, email: .estudianteEmail}]'
```

Expected: only rows where `estudianteNombre` or `estudianteEmail` contains "juan" (case-insensitive).

- [ ] **Step 7.7 — Test sort especial sinPreseleccionados**

```bash
curl -s "http://localhost:8080/api/admin/postulaciones?sort=sinPreseleccionados&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq '[.content[] | {proyectoId: .proyectoId, tienePreseleccionado: .tienePreseleccionado}]'
```

Expected: rows with `tienePreseleccionado: false` appear before rows with `tienePreseleccionado: true`.

- [ ] **Step 7.8 — Test acceso denegado sin rol ADMIN**

```bash
curl -s http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer $STUDENT_OR_MYPE_TOKEN" | jq .
```

Expected: HTTP 403 response.

- [ ] **Step 7.9 — Final commit**

```
git add .
git commit -m "chore: verify admin dashboard and postulaciones endpoints working"
```

---

## Self-Review Checklist

- [x] `DashboardStatsResponse` covers all 10 required fields from spec
- [x] `PostulacionAdminResponse` covers all 14 required fields from spec
- [x] `PostulacionSpec` handles all 7 filter params (proyectoId, estados, fechaDesde, fechaHasta, estudiante, mype, area)
- [x] Fetch joins skipped on count query (Long.class check) — no Hibernate in-memory pagination warning
- [x] `tienePreseleccionado` computed via 2-query approach — no N+1
- [x] `sinPreseleccionados` sort uses correlated subquery in ORDER BY
- [x] Both controllers protected with `hasAuthority('ROLE_ADMIN')` matching existing pattern
- [x] All COUNT/AVG queries are aggregations — no full-table fetches
- [x] `proyectosPorArea` uses GROUP BY at DB level
- [x] Invalid `area` value returns empty page (not 500)
- [x] `promedioCalificacion*` fields are `Double` (nullable) — returns null when no calificaciones exist
