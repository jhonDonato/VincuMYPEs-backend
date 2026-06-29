package com.mypelink.backend.shared.infrastructure;

import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import com.mypelink.backend.proyectos.domain.repository.TipoProyectoRepository;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.RoleRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TipoProyectoRepository tipoProyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Admin Enzo ──
    @Value("${admin.enzo.name}") private String enzoName;
    @Value("${admin.enzo.email}") private String enzoEmail;
    @Value("${admin.enzo.password}") private String enzoPwd;
    @Value("${admin.enzo.dni}") private String enzoDni;
    @Value("${admin.enzo.telefono}") private String enzoTelefono;
    @Value("${admin.enzo.codigo}") private String enzoCodigo;
    @Value("${admin.enzo.carrera}") private String enzoCarrera;
    @Value("${admin.enzo.universidad}") private String enzoUniversidad;

    // ── Admin Marco ──
    @Value("${admin.marco.name}") private String marcoName;
    @Value("${admin.marco.email}") private String marcoEmail;
    @Value("${admin.marco.password}") private String marcoPwd;
    @Value("${admin.marco.dni}") private String marcoDni;
    @Value("${admin.marco.telefono}") private String marcoTelefono;
    @Value("${admin.marco.codigo}") private String marcoCodigo;
    @Value("${admin.marco.carrera}") private String marcoCarrera;
    @Value("${admin.marco.universidad}") private String marcoUniversidad;

    // ── Admin Jhon ──
    @Value("${admin.jhon.name}") private String jhonName;
    @Value("${admin.jhon.email}") private String jhonEmail;
    @Value("${admin.jhon.password}") private String jhonPwd;
    @Value("${admin.jhon.dni}") private String jhonDni;
    @Value("${admin.jhon.telefono}") private String jhonTelefono;
    @Value("${admin.jhon.codigo}") private String jhonCodigo;
    @Value("${admin.jhon.carrera}") private String jhonCarrera;
    @Value("${admin.jhon.universidad}") private String jhonUniversidad;

    // ── Admin Gianpiero ──
    @Value("${admin.gianpiero.name}") private String gianpieroName;
    @Value("${admin.gianpiero.email}") private String gianpieroEmail;
    @Value("${admin.gianpiero.password}") private String gianpieroPwd;
    @Value("${admin.gianpiero.dni}") private String gianpieroDni;
    @Value("${admin.gianpiero.telefono}") private String gianpieroTelefono;
    @Value("${admin.gianpiero.codigo}") private String gianpieroCodigo;
    @Value("${admin.gianpiero.carrera}") private String gianpieroCarrera;
    @Value("${admin.gianpiero.universidad}") private String gianpieroUniversidad;

    // ── Admin Segundo ──
    @Value("${admin.segundo.name}") private String segundoName;
    @Value("${admin.segundo.email}") private String segundoEmail;
    @Value("${admin.segundo.password}") private String segundoPwd;
    @Value("${admin.segundo.dni}") private String segundoDni;
    @Value("${admin.segundo.telefono}") private String segundoTelefono;
    @Value("${admin.segundo.codigo}") private String segundoCodigo;
    @Value("${admin.segundo.carrera}") private String segundoCarrera;
    @Value("${admin.segundo.universidad}") private String segundoUniversidad;

    @Override
    public void run(String... args) {

        // 1. Inicializar Tipos de Proyecto (Solo si la tabla está vacía)
        if (tipoProyectoRepository.count() == 0) {
            tipoProyectoRepository.saveAll(List.of(
                    // ... (todos los tipos de proyecto, sin cambios)
            ));
        }

        // 2. Inicializar Roles del sistema
        Role adminRole = crearRolSiNoExiste("ROLE_ADMIN");
        crearRolSiNoExiste("ROLE_ESTUDIANTE");
        crearRolSiNoExiste("ROLE_MYPE");

        // 3. Inicializar Administradores con todos sus campos
        crearAdminSiNoExiste(enzoName, enzoEmail, enzoPwd, enzoDni, enzoTelefono, enzoCodigo, enzoCarrera, enzoUniversidad, adminRole);
        crearAdminSiNoExiste(marcoName, marcoEmail, marcoPwd, marcoDni, marcoTelefono, marcoCodigo, marcoCarrera, marcoUniversidad, adminRole);
        crearAdminSiNoExiste(jhonName, jhonEmail, jhonPwd, jhonDni, jhonTelefono, jhonCodigo, jhonCarrera, jhonUniversidad, adminRole);
        crearAdminSiNoExiste(gianpieroName, gianpieroEmail, gianpieroPwd, gianpieroDni, gianpieroTelefono, gianpieroCodigo, gianpieroCarrera, gianpieroUniversidad, adminRole);
        crearAdminSiNoExiste(segundoName, segundoEmail, segundoPwd, segundoDni, segundoTelefono, segundoCodigo, segundoCarrera, segundoUniversidad, adminRole);

        System.out.println("DataInitializer: Tipos de proyecto, Roles y Administradores cargados con éxito.");
    }

    private Role crearRolSiNoExiste(String nombreRol) {
        return roleRepository.findByNombre(nombreRol)
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(nombreRol).build()));
    }

    private void crearAdminSiNoExiste(String nombre, String email, String password,
                                      String dni, String telefono, String codigo,
                                      String carrera, String universidad, Role rol) {
        if (!usuarioRepository.existsByEmail(email)) {
            Usuario admin = Usuario.builder()
                    .nombre(nombre)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .dni(dni)
                    .telefono(telefono)
                    .rol(rol)
                    .activo(true)
                    .emailVerified(true)
                    .build();
            usuarioRepository.save(admin);
        }
    }
}