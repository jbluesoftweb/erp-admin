package lat.jbluesoft.erpadmin.asistencia.controller;

import jakarta.validation.Valid;
import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaCrearDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaEditarDTO;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lat.jbluesoft.erpadmin.core.util.MensajesError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controller: OcurrenciaAdminController
 * Alta y edición de ocurrencias (solo ADMIN_SYS). No hay borrado ni
 * desactivación: enabled nunca se muestra ni se modifica desde aquí.
 */
@Slf4j
@Controller
@RequestMapping("/admin/ocurrencias")
@RequiredArgsConstructor
public class OcurrenciaAdminController {

    private static final String VISTA_LISTA = "admin/ocurrencias";
    private static final String VISTA_FORM = "admin/ocurrencia-form";
    private static final String REDIRECT_LISTA = "redirect:/admin/ocurrencias";

    private final OcurrenciaService ocurrenciaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String listar(Model model) {
        try {
            model.addAttribute("listaOcurrencias", ocurrenciaService.listarParaAdministracion());
        } catch (Exception e) {
            log.error("Error al listar ocurrencias: {}", e.getMessage(), e);
            model.addAttribute("listaOcurrencias", List.of());
            model.addAttribute("error", MensajesError.GENERICO);
        }
        model.addAttribute("pageTitle", "Administrar Ocurrencias");
        model.addAttribute("headerTitle", "Administrar Ocurrencias");
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String nueva(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new OcurrenciaCrearDTO());
        }
        return mostrarFormularioAlta(model);
    }

    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String crear(@Valid @ModelAttribute("form") OcurrenciaCrearDTO form,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostrarFormularioAlta(model);
        }
        try {
            Ocurrencia creada = ocurrenciaService.crear(form);
            redirectAttributes.addFlashAttribute("success",
                    "Ocurrencia " + creada.getCodigo() + " creada correctamente.");
            return REDIRECT_LISTA;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return mostrarFormularioAlta(model);
        } catch (Exception e) {
            log.error("Error al crear ocurrencia: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", MensajesError.GENERICO);
            return "redirect:/admin/ocurrencias/nueva";
        }
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String editar(@PathVariable Integer id,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            Optional<Ocurrencia> ocurrencia = ocurrenciaService.obtenerPorId(id);
            if (ocurrencia.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No existe la ocurrencia solicitada.");
                return REDIRECT_LISTA;
            }
            model.addAttribute("form", OcurrenciaEditarDTO.desde(ocurrencia.get()));
            return mostrarFormularioEdicion(ocurrencia.get(), model);
        } catch (Exception e) {
            log.error("Error al cargar la ocurrencia {} para editar: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", MensajesError.GENERICO);
            return REDIRECT_LISTA;
        }
    }

    @PostMapping("/{id}/actualizar")
    @PreAuthorize("hasRole('ADMIN_SYS')")
    public String actualizar(@PathVariable Integer id,
                             @Valid @ModelAttribute("form") OcurrenciaEditarDTO form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                Optional<Ocurrencia> ocurrencia = ocurrenciaService.obtenerPorId(id);
                if (ocurrencia.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "No existe la ocurrencia solicitada.");
                    return REDIRECT_LISTA;
                }
                return mostrarFormularioEdicion(ocurrencia.get(), model);
            }
            Ocurrencia actualizada = ocurrenciaService.actualizar(id, form);
            redirectAttributes.addFlashAttribute("success",
                    "Ocurrencia " + actualizada.getCodigo() + " actualizada correctamente.");
            return REDIRECT_LISTA;
        } catch (IllegalArgumentException e) {
            Optional<Ocurrencia> ocurrencia = ocurrenciaService.obtenerPorId(id);
            if (ocurrencia.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return REDIRECT_LISTA;
            }
            model.addAttribute("error", e.getMessage());
            return mostrarFormularioEdicion(ocurrencia.get(), model);
        } catch (Exception e) {
            log.error("Error al actualizar la ocurrencia {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", MensajesError.GENERICO);
            return "redirect:/admin/ocurrencias/" + id + "/editar";
        }
    }

    private String mostrarFormularioAlta(Model model) {
        model.addAttribute("esEdicion", false);
        model.addAttribute("pageTitle", "Nueva Ocurrencia");
        model.addAttribute("headerTitle", "Nueva Ocurrencia");
        return VISTA_FORM;
    }

    private String mostrarFormularioEdicion(Ocurrencia ocurrencia, Model model) {
        model.addAttribute("esEdicion", true);
        model.addAttribute("ocurrencia", ocurrencia);
        model.addAttribute("pageTitle", "Editar Ocurrencia");
        model.addAttribute("headerTitle", "Editar Ocurrencia");
        return VISTA_FORM;
    }
}
