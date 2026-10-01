package lat.jbluesoft.erpadmin.core.advice;

import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * EtiquetaControllerAdvice
 * Expone las etiquetas administrables a todas las vistas Thymeleaf
 */
@ControllerAdvice
@RequiredArgsConstructor
public class EtiquetaControllerAdvice {

    private final EtiquetaService etiquetaService;

    @ModelAttribute("etiquetas")
    public Map<String, String> etiquetas() {
        return etiquetaService.getTodas();
    }
}
