package lat.jbluesoft.erpadmin.iam.controller;

import lat.jbluesoft.erpadmin.iam.model.Rol;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import lat.jbluesoft.erpadmin.iam.repository.RolRepository;
import lat.jbluesoft.erpadmin.iam.service.UsuarioService;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.rrhh.service.PersonalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/asistencia/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService  usuarioService;
    private final RolRepository   rolRepository;
    private final PersonalService personalService;
    private final DepartamentoService departamentoService;

    // =============================================
    // LISTAR
    // =============================================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String listar(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        log.info("Listando usuarios");

        List<Usuario> usuarios = usuarioService.listarTodos();

        if (!userDetails.hasRole("ADMIN_SYS")) {
            usuarios = usuarios.stream()
                    .filter(u -> s1PuedeGestionarUsuario(userDetails, u))
                    .toList();
        }

        // Cargar rol de cada usuario en un Map<idUsuario, codigoRol>
        Map<Integer, String> rolesMap = new HashMap<>();
        for (Usuario u : usuarios) {
            List<lat.jbluesoft.erpadmin.iam.model.RegistroSistema> registros =
                    usuarioService.obtenerRegistros(u.getIdUsuario());
            if (!registros.isEmpty()) {
                rolesMap.put(u.getIdUsuario(),
                        registros.get(0).getRol().getCodigo());
            }
        }

        model.addAttribute("usuarios",    usuarios);
        model.addAttribute("rolesMap",    rolesMap);
        model.addAttribute("pageTitle",   "Gestión de Usuarios");
        model.addAttribute("headerTitle", "Gestión de Usuarios");
        return "usuarios/listar";
    }

    // =============================================
    // CREAR
    // =============================================

    @GetMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String mostrarCrear(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<String> rolesExcluidos = List.of("ADMIN_IMINT", "USER_IMINT");
        List<Rol> roles = rolRepository.findByCodigoNotIn(rolesExcluidos);

        if (!userDetails.hasRole("ADMIN_SYS")) {
            roles = roles.stream()
                    .filter(r -> "ADMIN_DPTO".equals(r.getCodigo()))
                    .toList();
        }

        model.addAttribute("roles",       roles);
        model.addAttribute("pageTitle",   "Crear Usuario");
        model.addAttribute("headerTitle", "Crear Usuario");
        return "usuarios/crear";
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    @Transactional
    public String crear(@RequestParam String codigo,
                        @RequestParam(required = false) String email,
                        @RequestParam Integer idRol,
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        RedirectAttributes redirectAttributes) {

        if (!userDetails.hasRole("ADMIN_SYS")) {
            Rol rolSolicitado = rolRepository.findById(idRol).orElse(null);
            if (rolSolicitado == null || !"ADMIN_DPTO".equals(rolSolicitado.getCodigo())) {
                redirectAttributes.addFlashAttribute("error",
                        "Como Admin App, solo puedes crear usuarios con rol ADMIN_DPTO.");
                return "redirect:/asistencia/usuarios/crear";
            }

            Personal personalObjetivo = personalService.buscarPorCodigo(codigo.trim()).orElse(null);
            if (personalObjetivo == null
                    || personalObjetivo.getDepartamento() == null
                    || personalObjetivo.getDepartamento().getEmpresa() == null
                    || !personalObjetivo.getDepartamento().getEmpresa().getIdEmpresa()
                            .equals(resolverIdEmpresaDeActor(userDetails))) {
                redirectAttributes.addFlashAttribute("error",
                        "Ese código no pertenece a personal de tu empresa.");
                return "redirect:/asistencia/usuarios/crear";
            }
        }

        try {
            usuarioService.crear(codigo.trim(), email, "Peru123",
                    idRol, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success",
                    "Usuario creado correctamente para código: " + codigo);
            return "redirect:/asistencia/usuarios";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencia/usuarios/crear";
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al crear usuario: " + e.getMessage());
            return "redirect:/asistencia/usuarios/crear";
        }
    }

    // =============================================
    // EDITAR (email + rol)
    // =============================================

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String mostrarEditar(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuario)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        List<String> rolesExcluidos = List.of("ADMIN_IMINT", "USER_IMINT");
        List<Rol> roles = rolRepository.findByCodigoNotIn(rolesExcluidos);

        if (!userDetails.hasRole("ADMIN_SYS")) {
            roles = roles.stream()
                    .filter(r -> "ADMIN_DPTO".equals(r.getCodigo()))
                    .toList();
        }

        model.addAttribute("usuario",     usuario);
        model.addAttribute("registros",   usuarioService.obtenerRegistros(id));
        model.addAttribute("roles",       roles);
        model.addAttribute("pageTitle",   "Editar Usuario");
        model.addAttribute("headerTitle", "Editar Usuario");
        return "usuarios/editar";
    }

    @PostMapping("/editar/{id}/email")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String actualizarEmail(@PathVariable Integer id,
                                  @RequestParam(required = false) String email,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuario)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        try {
            usuarioService.actualizarEmail(id, email);
            redirectAttributes.addFlashAttribute("success",
                    "Email actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencia/usuarios/editar/" + id;
    }

    @PostMapping("/editar/{id}/rol")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String cambiarRol(@PathVariable Integer id,
                             @RequestParam Integer idRol,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuario)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        if (!userDetails.hasRole("ADMIN_SYS")) {
            Rol rolNuevo = rolRepository.findById(idRol).orElse(null);
            if (rolNuevo == null || !"ADMIN_DPTO".equals(rolNuevo.getCodigo())) {
                redirectAttributes.addFlashAttribute("error",
                        "Como Admin App, solo puedes asignar el rol ADMIN_DPTO.");
                return "redirect:/asistencia/usuarios/editar/" + id;
            }
        }

        try {
            usuarioService.cambiarRol(id, idRol, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success",
                    "Rol actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencia/usuarios/editar/" + id;
    }

    // =============================================
    // RESET PASSWORD (ADMIN_SYS)
    // =============================================

    @GetMapping("/resetear-password/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String mostrarResetPassword(@PathVariable Integer id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuario)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        model.addAttribute("usuario",     usuario);
        model.addAttribute("pageTitle",   "Resetear Contraseña");
        model.addAttribute("headerTitle", "Resetear Contraseña");
        return "usuarios/resetear-password";
    }

    @PostMapping("/resetear-password/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String resetearPassword(@PathVariable Integer id,
                                   @RequestParam String nuevaPassword,
                                   @RequestParam String confirmarPassword,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuario)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            redirectAttributes.addFlashAttribute("error",
                    "Las contraseñas no coinciden.");
            return "redirect:/asistencia/usuarios/resetear-password/" + id;
        }

        try {
            lat.jbluesoft.erpadmin.core.util.PasswordValidator.validar(nuevaPassword);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencia/usuarios/resetear-password/" + id;
        }

        try {
            usuarioService.resetearPassword(id, nuevaPassword);
            redirectAttributes.addFlashAttribute("success",
                    "Contraseña reseteada correctamente.");
            return "redirect:/asistencia/usuarios";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error: " + e.getMessage());
            return "redirect:/asistencia/usuarios/resetear-password/" + id;
        }
    }

    // =============================================
    // CAMBIAR PROPIA PASSWORD (cualquier usuario)
    // =============================================

    @GetMapping("/mi-password")
    public String mostrarCambiarPassword(Model model) {
        model.addAttribute("pageTitle",   "Cambiar Contraseña");
        model.addAttribute("headerTitle", "Cambiar Contraseña");
        return "usuarios/mi-password";
    }

    @PostMapping("/mi-password")
    public String cambiarPassword(@RequestParam String passwordActual,
                                  @RequestParam String nuevaPassword,
                                  @RequestParam String confirmarPassword,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) throws ServletException {

        if (!nuevaPassword.equals(confirmarPassword)) {
            redirectAttributes.addFlashAttribute("error",
                    "Las contraseñas no coinciden.");
            return "redirect:/asistencia/usuarios/mi-password";
        }

        try {
            lat.jbluesoft.erpadmin.core.util.PasswordValidator.validar(nuevaPassword);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencia/usuarios/mi-password";
        }

        try {
            usuarioService.cambiarPassword(
                    userDetails.getUsuario().getIdUsuario(),
                    passwordActual, nuevaPassword);

            request.logout();

            return "redirect:/login?passwordCambiada=true";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencia/usuarios/mi-password";
        }
    }

    // =============================================
    // DESHABILITAR / REACTIVAR
    // =============================================

    @PostMapping("/deshabilitar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String deshabilitar(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Usuario usuarioObjetivo = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuarioObjetivo)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        try {
            usuarioService.deshabilitar(
                    id, userDetails.getUsuario().getIdUsuario());
            redirectAttributes.addFlashAttribute("success",
                    "Usuario deshabilitado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencia/usuarios";
    }

    @PostMapping("/reactivar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    public String reactivar(@PathVariable Integer id,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        Usuario usuarioObjetivo = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!userDetails.hasRole("ADMIN_SYS") && !s1PuedeGestionarUsuario(userDetails, usuarioObjetivo)) {
            redirectAttributes.addFlashAttribute("error",
                    "No tienes permiso para gestionar este usuario.");
            return "redirect:/asistencia/usuarios";
        }

        try {
            usuarioService.reactivar(id);
            redirectAttributes.addFlashAttribute("success",
                    "Usuario reactivado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencia/usuarios";
    }

    // =============================================
    // SEGURIDAD: ALCANCE DE ADMIN_APP
    // =============================================

    /**
     * Resuelve el id de unidad del usuario que está actuando (vía su propia
     * departamento). Lanza excepción si algo no cuadra (no debería pasar nunca
     * en la práctica, ya que todo usuario autenticado tiene departamento válido).
     */
    private Integer resolverIdEmpresaDeActor(CustomUserDetails userDetails) {
        Departamento departamentoActor = departamentoService.buscarPorId(userDetails.getIdDepartamento())
                .orElseThrow(() -> new IllegalStateException(
                        "Departamento del usuario autenticado no encontrado."));
        return departamentoActor.getEmpresa().getIdEmpresa();
    }

    /**
     * Verifica si un ADMIN_APP tiene permiso para gestionar al usuario objetivo:
     * debe tener rol ADMIN_DPTO Y pertenecer a la misma unidad del ADMIN_APP.
     * ADMIN_SYS no pasa por aquí (siempre true para ellos, ver los usos).
     */
    private boolean s1PuedeGestionarUsuario(CustomUserDetails userDetails, Usuario usuarioObjetivo) {
        List<lat.jbluesoft.erpadmin.iam.model.RegistroSistema> registros =
                usuarioService.obtenerRegistros(usuarioObjetivo.getIdUsuario());

        if (registros.isEmpty()) {
            return false;
        }

        String rolObjetivo = registros.get(0).getRol().getCodigo();
        if (!"ADMIN_DPTO".equals(rolObjetivo)) {
            return false;
        }

        if (usuarioObjetivo.getPersonal() == null
                || usuarioObjetivo.getPersonal().getDepartamento() == null
                || usuarioObjetivo.getPersonal().getDepartamento().getEmpresa() == null) {
            return false;
        }

        Integer idEmpresaObjetivo = usuarioObjetivo.getPersonal().getDepartamento().getEmpresa().getIdEmpresa();
        Integer idEmpresaActor = resolverIdEmpresaDeActor(userDetails);

        return idEmpresaObjetivo.equals(idEmpresaActor);
    }
}