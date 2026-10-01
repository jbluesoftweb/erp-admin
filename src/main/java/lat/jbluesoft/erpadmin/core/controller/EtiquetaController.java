package lat.jbluesoft.erpadmin.core.controller;

import jakarta.validation.Valid;
import lat.jbluesoft.erpadmin.core.dto.EtiquetaUpdateDTO;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller: EtiquetaController
 * Acciones de administración sobre las etiquetas personalizables (core.etiqueta)
 */
@Slf4j
@Controller
@RequestMapping("/admin/etiquetas")
@RequiredArgsConstructor
public class EtiquetaController {

    private final EtiquetaService etiquetaService;

    /**
     * Pantalla de administración de etiquetas.
     * El listado va en "listaEtiquetas" porque "etiquetas" ya lo publica
     * EtiquetaControllerAdvice (Map clave -> valor vigente) y lo usa el layout.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String listar(Model model) {
        model.addAttribute("listaEtiquetas", etiquetaService.listarTodas());
        model.addAttribute("pageTitle",   "Administrar Etiquetas");
        model.addAttribute("headerTitle", "Administrar Etiquetas");
        return "admin/etiquetas";
    }

    /**
     * Actualiza el valor personalizado de una etiqueta (un formulario por fila).
     * Solo se bindean clave y valorPersonalizado vía EtiquetaUpdateDTO.
     */
    @PostMapping("/actualizar")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String actualizar(@Valid @ModelAttribute EtiquetaUpdateDTO dto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String mensaje = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("error", "Valor inválido: " + mensaje);
            return "redirect:/admin/etiquetas";
        }
        try {
            etiquetaService.actualizar(dto.getClave(), dto.getValorPersonalizado(), userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "Etiqueta actualizada correctamente.");
        } catch (Exception e) {
            log.error("Error al actualizar etiqueta {}: {}", dto.getClave(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar la etiqueta: " + e.getMessage());
        }
        return "redirect:/admin/etiquetas";
    }

    /**
     * Recarga el caché de etiquetas desde la BD, para aplicar cambios
     * en core.etiqueta sin reiniciar el servidor
     */
    @PostMapping("/refrescar")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String refrescar(RedirectAttributes redirectAttributes) {
        try {
            etiquetaService.refrescarCache();
            log.info("Cache de etiquetas refrescado manualmente");
            redirectAttributes.addFlashAttribute("success", "Etiquetas actualizadas correctamente.");
        } catch (Exception e) {
            log.error("Error al refrescar cache de etiquetas: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar las etiquetas: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}
