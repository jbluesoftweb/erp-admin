package lat.jbluesoft.erpadmin.asistencia.controller;

import lat.jbluesoft.erpadmin.asistencia.dto.AlmuerzoDTO;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.asistencia.service.AlmuerzoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller: AlmuerzoController
 * Maneja las peticiones del módulo de Almuerzo
 *
 * Funcionalidad:
 * - Lista de personal que recibe almuerzo
 * - SOLO personal PRESENTE (ocurrencia = ASISTIÓ)
 * - Por departamento (cada administrativo ve SOLO su departamento)
 */
@Slf4j
@Controller
@RequestMapping("/almuerzo")
@RequiredArgsConstructor
public class AlmuerzoController {

    private final AlmuerzoService almuerzoService;
    private final DepartamentoService departamentoService;

    /**
     * Muestra la página principal de almuerzo
     * Con datepicker, resumen de totales y botón imprimir
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String mostrarAlmuerzo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestParam(required = false) LocalDate fecha,
                                       Model model) {

        log.info("Mostrando almuerzo para usuario: {}", userDetails.getUsername());

        try {
            // Obtener departamento del usuario logueado
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Si no se especifica fecha, usar hoy
            LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();

            // Obtener almuerzo de SU departamento
            AlmuerzoDTO almuerzo =
                    almuerzoService.obtenerAlmuerzoPorDepartamento(fechaConsulta, idDepartamento);

            // Datos para el modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("almuerzo", almuerzo);
            model.addAttribute("fechaSeleccionada", fechaConsulta);

            log.info("Almuerzo cargado - Grupo 1: {}, Grupo 2: {}, Total: {}",
                    almuerzo.getTotalGrupo1(),
                    almuerzo.getTotalGrupo2(),
                    almuerzo.getTotalGeneral());

            return "almuerzo/almuerzo";

        } catch (Exception e) {
            log.error("Error al cargar almuerzo: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el almuerzo: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Página de impresión del almuerzo
     * Se abre automáticamente con window.print()
     */
    @GetMapping("/imprimir")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String imprimirAlmuerzo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @RequestParam LocalDate fecha,
                                        Model model) {

        log.info("Generando impresión de almuerzo para fecha: {}", fecha);

        try {
            // Obtener departamento del usuario logueado
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Obtener almuerzo de SU departamento
            AlmuerzoDTO almuerzo =
                    almuerzoService.obtenerAlmuerzoPorDepartamento(fecha, idDepartamento);

            // Datos para el modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("almuerzo", almuerzo);
            model.addAttribute("fechaSeleccionada", fecha);

            return "almuerzo/almuerzo-imprimir";

        } catch (Exception e) {
            log.error("Error al generar impresión de almuerzo: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar la impresión: " + e.getMessage());
            return "error";
        }
    }
}