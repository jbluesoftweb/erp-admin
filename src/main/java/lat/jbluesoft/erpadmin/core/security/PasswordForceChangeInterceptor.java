package lat.jbluesoft.erpadmin.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Si el usuario autenticado todavía tiene la contraseña por defecto
 * ("Peru123"), lo redirige a la pantalla de cambio de contraseña,
 * bloqueando el acceso a cualquier otra pantalla del sistema.
 */
@Component
@RequiredArgsConstructor
public class PasswordForceChangeInterceptor implements HandlerInterceptor {

    private final PasswordEncoder passwordEncoder;

    private static final String RUTA_CAMBIO_PASSWORD = "/asistencia/usuarios/mi-password";
    private static final String PASSWORD_POR_DEFECTO = "Peru123";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();

        // Rutas siempre permitidas, aunque tenga la password por defecto
        if (uri.startsWith(RUTA_CAMBIO_PASSWORD)
                || uri.equals("/login")
                || uri.equals("/logout")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/img/")
                || uri.startsWith("/webjars/")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return true;
        }

        String hashActual = userDetails.getUsuario().getPasswordHash();
        if (hashActual != null && passwordEncoder.matches(PASSWORD_POR_DEFECTO, hashActual)) {
            response.sendRedirect(RUTA_CAMBIO_PASSWORD);
            return false;
        }

        return true;
    }
}
