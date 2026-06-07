package com.mypelink.backend.shared.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.shared.domain.repository.ConfiguracionSistemaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private final ConfiguracionSistemaRepository configRepository;
    private final ConfiguracionService configuracionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Endpoints que siempre deben funcionar (login, refresh, etc.)
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login")
                || path.equals("/api/configuracion/estado")
                || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Obtener configuración (modo mantenimiento)
        boolean modoMantenimiento = configRepository.findFirstByOrderByIdAsc()
                .map(c -> c.getModoMantenimiento() != null && c.getModoMantenimiento())
                .orElse(false);

        if (!modoMantenimiento) {
            filterChain.doFilter(request, response);
            return;
        }

        // Si modo mantenimiento activo, verificar rol ADMIN
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bloquear acceso
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        Map<String, String> errorResponse = Map.of(
                "error", "MAINTENANCE_MODE",
                "message", "Sistema en mantenimiento. Solo administradores pueden ingresar."
        );
        new ObjectMapper().writeValue(response.getWriter(), errorResponse);
    }
}