# Admin Dashboard & Postulaciones — Design Spec
**Date:** 2026-06-06  
**Status:** Approved

---

## Scope

Two new admin-only endpoints for VincuMYPEs:

1. `GET /api/admin/dashboard/stats` — real-time KPI metrics
2. `GET /api/admin/postulaciones` — paginated, filterable view of all postulaciones

No changes to existing postulación workflow (state transitions remain untouched).

---

## Architecture — Option A (approved)

One new shared service for stats + one service for postulación queries. Two new controllers following the existing `Admin*Controller` naming pattern.

### New files

| File | Location |
|---|---|
| `DashboardStatsResponse.java` | `shared/application/dto/` |
| `AdminDashboardService.java` | `shared/application/service/` |
| `PostulacionAdminResponse.java` | `proyectos/application/dto/` |
| `PostulacionAdminService.java` | `proyectos/application/service/` |
| `PostulacionSpec.java` | `proyectos/infrastructure/spec/` |
| `AdminDashboardController.java` | `shared/infrastructure/rest/` |
| `AdminPostulacionController.java` | `proyectos/infrastructure/rest/` |

### Modified files (minimal)

| File | Change |
|---|---|
| `PostulacionRepository` | Add `extends JpaSpecificationExecutor<Postulacion>` |
| `UsuarioRepository` | Add `countByRolNombre(String)` JPQL query |
| `CalificacionRepository` | Add `promedioByCalificadoRol(String)` JPQL query |
| `ProyectoRepository` | Add `countByEstadoIn(...)` and `countByAreaSistemas()` |

---

## Endpoint 1: Dashboard Stats

### `GET /api/admin/dashboard/stats`
**Security:** `@PreAuthorize("hasRole('ADMIN')")` on controller class  
**Response:** `DashboardStatsResponse` (JSON object)

### DashboardStatsResponse fields

| Field | Type | Source |
|---|---|---|
| `totalEstudiantes` | `long` | `COUNT(u) WHERE u.rol.nombre = 'ROLE_ESTUDIANTE'` |
| `totalMypes` | `long` | `COUNT(u) WHERE u.rol.nombre = 'ROLE_MYPE'` |
| `totalAdmins` | `long` | `COUNT(u) WHERE u.rol.nombre = 'ROLE_ADMIN'` |
| `proyectosActivos` | `long` | `COUNT(p) WHERE p.estado IN (PENDIENTE, EN_DESARROLLO) AND p.activo = true` |
| `proyectosCompletados` | `long` | `COUNT(p) WHERE p.estado = COMPLETADO AND p.activo = true` |
| `postulacionesPendientes` | `long` | `COUNT(p) WHERE p.estado = PENDIENTE` |
| `certificadosEmitidos` | `long` | `certificadoRepository.count()` |
| `promedioCalificacionMypes` | `Double` (nullable) | `AVG(puntuacion) WHERE calificado.rol.nombre = 'ROLE_MYPE'` |
| `promedioCalificacionEstudiantes` | `Double` (nullable) | `AVG(puntuacion) WHERE calificado.rol.nombre = 'ROLE_ESTUDIANTE'` |
| `proyectosPorArea` | `Map<String, Long>` | `GROUP BY areaSistemas` on Proyecto |

All queries are COUNT/AVG aggregations — no full-table fetches.

### New repository methods needed

**UsuarioRepository:**
```jpql
@Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.nombre = :rolNombre")
long countByRolNombre(@Param("rolNombre") String rolNombre);
```

**CalificacionRepository:**
```jpql
@Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.rol.nombre = :rolNombre")
Double promedioByCalificadoRol(@Param("rolNombre") String rolNombre);
```

**ProyectoRepository:**
```jpql
@Query("SELECT COUNT(p) FROM Proyecto p WHERE p.estado IN :estados AND p.activo = true")
long countByEstadoInAndActivoTrue(@Param("estados") List<WorkflowEstado> estados);

@Query("SELECT p.areaSistemas, COUNT(p) FROM Proyecto p GROUP BY p.areaSistemas")
List<Object[]> countGroupByAreaSistemas();
```

---

## Endpoint 2: Postulaciones with Filters

### `GET /api/admin/postulaciones`
**Security:** `@PreAuthorize("hasRole('ADMIN')")` on controller class  
**Response:** `Page<PostulacionAdminResponse>`

### Query parameters

| Param | Type | Behavior |
|---|---|---|
| `proyectoId` | `Long` | Exact match |
| `estado` | `List<EstadoPostulacion>` | IN filter; ignored if empty |
| `fechaDesde` | `LocalDate` | `fechaPostulacion >= fechaDesde 00:00:00` |
| `fechaHasta` | `LocalDate` | `fechaPostulacion <= fechaHasta 23:59:59` |
| `estudiante` | `String` | LIKE on `estudiante.usuario.nombre` OR `.email` (case-insensitive) |
| `mype` | `String` | LIKE on `proyecto.mype.nombreComercial` (case-insensitive) |
| `area` | `String` | Exact match on `proyecto.areaSistemas` enum value |
| `page` | `int` | Default 0 |
| `size` | `int` | Default 10 |
| `sort` | `String` | Default `fechaPostulacion,desc`; special value `sinPreseleccionados` |

### PostulacionAdminResponse fields

| Field | Source |
|---|---|
| `id` | `postulacion.id` |
| `estudianteId` | `postulacion.estudiante.id` |
| `estudianteNombre` | `postulacion.estudiante.usuario.nombre` |
| `estudianteEmail` | `postulacion.estudiante.usuario.email` |
| `proyectoId` | `postulacion.proyecto.id` |
| `proyectoTitulo` | `postulacion.proyecto.titulo` |
| `proyectoArea` | `postulacion.proyecto.areaSistemas.name()` |
| `mypeId` | `postulacion.proyecto.mype.id` |
| `mypeNombre` | `postulacion.proyecto.mype.nombreComercial` |
| `fechaPostulacion` | `postulacion.fechaPostulacion` |
| `estado` | `postulacion.estado.name()` |
| `fechaRespuesta` | `postulacion.fechaRespuesta` (nullable) |
| `fechaLimiteConfirmacion` | `postulacion.fechaLimiteConfirmacion` (nullable) |
| `tienePreseleccionado` | `boolean` — computed (see below) |

### Specification design (`PostulacionSpec.java`)

Static factory method `PostulacionSpec.build(filters...)` returns a composed `Specification<Postulacion>`.

Key rules:
- Each filter is only added to predicates list if its value is non-null/non-empty.
- FETCH JOINs for `proyecto → mype → usuario` and `estudiante → usuario` are added **only when `query.getResultType() != Long.class`** to avoid breaking the count query Spring generates for pagination.
- JoinType.INNER for required associations (`proyecto`, `estudiante`); they are never null.
- JoinType.LEFT is not needed since all associations are non-nullable.

### N+1 avoidance for `tienePreseleccionado`

After fetching the page of `Postulacion` entities:
1. Collect distinct `proyectoIds` from the page.
2. Execute one query: `SELECT DISTINCT p.proyecto.id FROM Postulacion p WHERE p.proyecto.id IN :ids AND p.estado = PRESELECCIONADO`.
3. Build a `Set<Long>` of proyectoIds that have preseleccionados.
4. Map each `Postulacion` → `PostulacionAdminResponse` setting `tienePreseleccionado = set.contains(postulacion.getProyecto().getId())`.

This is 2 total DB queries per page (data fetch + preseleccionado check), regardless of page size.

### Special sort: `sinPreseleccionados`

When `sort=sinPreseleccionados` is detected, the service builds a `Pageable` without sort and injects custom `Order` into the `CriteriaQuery` via the Specification:

```sql
ORDER BY (
  SELECT COUNT(*) FROM postulaciones p2
  WHERE p2.proyecto_id = p.proyecto_id
  AND p2.estado = 'PRESELECCIONADO'
) ASC, p.fecha_postulacion DESC
```

This is a correlated subquery in ORDER BY, supported efficiently by MySQL with an index on `(proyecto_id, estado)`. For normal `fechaPostulacion,desc` sort, Spring's Pageable sort is used without modification.

---

## Security

Both controllers are annotated:
```java
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```
This matches the existing pattern in `AdminReportesController` and `AdminProyectoController` exactly.

---

## Error handling

- Invalid `area` enum value → caught and returned as 400 via `GlobalExceptionHandler`.
- Invalid `estado` enum value → same.
- No special handling needed for empty results (returns empty `Page`).

---

## Out of scope

- Mutating postulación state (already exists in other controllers).
- Caching of stats (not requested; real-time is the requirement).
- Export to CSV/PDF.
