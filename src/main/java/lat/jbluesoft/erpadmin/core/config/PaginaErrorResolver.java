package lat.jbluesoft.erpadmin.core.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.Set;

/**
 * Elige la página de error según el código de estado:
 * 403, 404 y 500 tienen página propia (templates/error/{codigo}.html) y
 * cualquier otro código usa la genérica (templates/error/error.html).
 *
 * Sin este resolutor, Spring solo busca error/{codigo} y error/4xx|5xx;
 * para los demás códigos mostraría su página en blanco por defecto.
 */
@Component
public class PaginaErrorResolver implements ErrorViewResolver, Ordered {

    private static final Set<Integer> CON_PAGINA_PROPIA = Set.of(403, 404, 500);

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        String vista = CON_PAGINA_PROPIA.contains(status.value())
                ? "error/" + status.value()
                : "error/error";
        return new ModelAndView(vista, model, status);
    }

    @Override
    public int getOrder() {
        // Antes que el resolutor por defecto de Spring Boot
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
