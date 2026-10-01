package lat.jbluesoft.erpadmin.core.controller;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller: HomeController
 * Maneja las páginas principales del sistema
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DepartamentoService departamentoService;

    /**
     * Página principal - Dashboard
     * Muestra información general del sistema y accesos rápidos
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        log.info("Cargando dashboard para usuario: {}", userDetails.getUsername());

        try {
            // Obtener departamento del usuario
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Traducir roles a texto legible
            List<String> rolesLegibles = new ArrayList<>();
            for (GrantedAuthority authority : userDetails.getAuthorities()) {
                String rol = authority.getAuthority();
                String rolLegible = traducirRol(rol);
                if (rolLegible != null) {
                    rolesLegibles.add(rolLegible);
                }
            }

            // Agregar datos al modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("roles", rolesLegibles);
            model.addAttribute("headerTitle", "Dashboard");

            log.info("Dashboard cargado exitosamente - Roles: {}", rolesLegibles);

            return "dashboard/index";

        } catch (Exception e) {
            log.error("Error al cargar dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el dashboard");
            return "error";
        }
    }

    /**
     * Traduce los roles técnicos a descripciones legibles
     */
    private String traducirRol(String rol) {
        return switch (rol) {
            case "ROLE_ADMIN_DPTO" -> "Administrador de Departamento";
            case "ROLE_ADMIN_APP" -> "Administrador de Aplicación";
            case "ROLE_ADMIN_SYS" -> "Administrador del Sistema";
            default -> null; // Ignorar roles que no queremos mostrar (como FACTOR_PASSWORD)
        };
    }
}