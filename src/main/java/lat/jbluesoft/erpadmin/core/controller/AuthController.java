package lat.jbluesoft.erpadmin.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * AuthController
 * Controlador de autenticación
 * Spring Security maneja el POST del login automáticamente
 */
@Controller
public class AuthController {

    /**
     * Mostrar formulario de login
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "passwordCambiada", required = false) String passwordCambiada,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "Código o contraseña incorrectos");
        }

        if (logout != null) {
            model.addAttribute("message", "Sesión cerrada exitosamente");
        }

        if (passwordCambiada != null) {
            model.addAttribute("message",
                    "Contraseña actualizada correctamente. Inicia sesión con tu nueva contraseña.");
        }

        return "auth/login";
    }
}
