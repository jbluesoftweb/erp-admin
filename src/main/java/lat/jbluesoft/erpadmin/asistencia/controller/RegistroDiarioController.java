package lat.jbluesoft.erpadmin.asistencia.controller;

import lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.AsistenciaRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.ResumenRegistroDTO;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lat.jbluesoft.erpadmin.asistencia.service.RegistroDiarioService;
import lat.jbluesoft.erpadmin.rrhh.service.PersonalService;
import lat.jbluesoft.erpadmin.asistencia.util.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/asistencia")
@RequiredArgsConstructor
public class RegistroDiarioController {

    private final RegistroDiarioService registroDiarioService;
    private final PersonalService personalService;
    private final OcurrenciaService ocurrenciaService;
    private final DepartamentoService departamentoService;
    private final SpringTemplateEngine templateEngine;
    private final PdfGeneratorService pdfGeneratorService;

    /**
     * Muestra la página de registro de asistencia
     * ACTUALIZADO: Usa prepararRegistroDiario() que compara efectivo vs último registro
     */
    @GetMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    public String mostrarRegistro(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam(required = false) LocalDate fecha,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        log.info("Mostrando formulario de registro de asistencia para usuario: {}", userDetails.getUsername());

        try {
            // Obtener el departamento del usuario logueado
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Si no se especifica fecha, usar la fecha actual
            LocalDate fechaRegistro = (fecha != null) ? fecha : LocalDate.now();

            if (!userDetails.hasRole("ADMIN_SYS")
                    && !registroDiarioService.puedeRegistrarAsistencia(idDepartamento, fechaRegistro)) {
                LocalDate diaHabilAnterior = registroDiarioService.obtenerDiaHabilAnterior(fechaRegistro);

                // Evitar bucle infinito: si ya estamos viendo el día que falta y aun así
                // no se puede registrar, algo más está pasando — no redirigir de nuevo.
                if (!fechaRegistro.equals(diaHabilAnterior)) {
                    redirectAttributes.addFlashAttribute("info",
                            "Tienes registros pendientes. Antes de registrar el "
                            + fechaRegistro + ", debes registrar la asistencia del "
                            + diaHabilAnterior + ".");
                    return "redirect:/asistencia/registrar?fecha=" + diaHabilAnterior;
                }
            }

            if (!userDetails.hasRole("ADMIN_SYS")) {
                LocalDate diaHabilSiguiente = registroDiarioService.obtenerDiaHabilSiguiente(LocalDate.now());
                if (fechaRegistro.isAfter(diaHabilSiguiente)) {
                    model.addAttribute("error",
                            "No puedes registrar asistencia con tanta anticipación. "
                            + "Como máximo puedes adelantar el registro del " + diaHabilSiguiente + ".");
                    return "error";
                }
            }

            // ========== NUEVO: Usar prepararRegistroDiario() ==========
            // Este método compara el efectivo actual vs el último registro
            // y separa en: Grupo 1, Grupo 2 y Ocurrencias
            AsistenciaRegistroDTO dto = registroDiarioService.prepararRegistroDiario(idDepartamento, fechaRegistro);

            // Obtener todas las ocurrencias disponibles para los selects
            List<Ocurrencia> ocurrencias = ocurrenciaService.listarTodas();

            // Datos para el modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("fechaRegistro", dto.getFechaRegistro());
            model.addAttribute("grupo1", dto.getGrupo1());
            model.addAttribute("grupo2", dto.getGrupo2());
            model.addAttribute("ocurrenciasPreLlenadas", dto.getOcurrencias()); // Pre-llenadas del último registro
            model.addAttribute("ocurrencias", ocurrencias); // Todas las ocurrencias disponibles
            model.addAttribute("totalPersonal", dto.getTotalEfectivo());
            model.addAttribute("existeRegistroAnterior", dto.getExisteRegistroAnterior());
            model.addAttribute("fechaUltimoRegistro", dto.getFechaUltimoRegistro());

            log.info("Personal cargado - Grupo 1: {}, Grupo 2: {}, Ocurrencias: {}",
                    dto.getGrupo1().size(),
                    dto.getGrupo2().size(),
                    dto.getOcurrencias().size());

            return "asistencia/registrar";

        } catch (Exception e) {
            log.error("Error al cargar formulario de registro: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Guarda el registro de asistencia en bloque - recibe parámetros HTTP directamente
     */
    @PostMapping("/guardar-bloque")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    public String guardarRegistroBloque(HttpServletRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {

        try {
            // Extraer fecha del registro
            String fechaRegistroStr = request.getParameter("fechaRegistro");
            LocalDate fechaRegistro = LocalDate.parse(fechaRegistroStr);

            log.info("Iniciando guardado en bloque de registro de asistencia para fecha: {}", fechaRegistro);

            // Obtener datos del usuario
            Integer idDepartamento = userDetails.getIdDepartamento();
            String codigoUsuario = userDetails.getUsername();

            if (!userDetails.hasRole("ADMIN_SYS")
                    && !registroDiarioService.puedeRegistrarAsistencia(idDepartamento, fechaRegistro)) {
                LocalDate diaHabilAnterior = registroDiarioService.obtenerDiaHabilAnterior(fechaRegistro);
                redirectAttributes.addFlashAttribute("info",
                        "Tienes registros pendientes. Antes de registrar el "
                        + fechaRegistro + ", debes registrar la asistencia del "
                        + diaHabilAnterior + ".");
                return "redirect:/asistencia/registrar?fecha=" + diaHabilAnterior;
            }

            if (!userDetails.hasRole("ADMIN_SYS")) {
                LocalDate diaHabilSiguiente = registroDiarioService.obtenerDiaHabilSiguiente(LocalDate.now());
                if (fechaRegistro.isAfter(diaHabilSiguiente)) {
                    redirectAttributes.addFlashAttribute("error",
                            "No puedes registrar asistencia con tanta anticipación. "
                            + "Como máximo puedes adelantar el registro del " + diaHabilSiguiente + ".");
                    return "redirect:/asistencia/registrar";
                }
            }

            // Verificar que no exista ya un registro para esta fecha
            boolean existeRegistro = registroDiarioService.existeRegistroPorDepartamentoYFecha(idDepartamento, fechaRegistro);

            if (existeRegistro) {
                log.warn("Ya existe un registro para la fecha: {}", fechaRegistro);
                redirectAttributes.addFlashAttribute("error",
                        "Ya existe un registro para la fecha " + fechaRegistro +
                                ". Use la opción ACTUALIZAR para modificarlo.");
                return "redirect:/asistencia/registrar";
            }

            // Obtener todos los parámetros del request
            Map<String, String[]> paramMap = request.getParameterMap();

            // Construir lista de registros
            List<RegistroDiario> registrosParaGuardar = new ArrayList<>();

            // Contar cuántos registros hay buscando el patrón registros[N].idPersonal
            int maxIndex = -1;
            for (String key : paramMap.keySet()) {
                if (key.startsWith("registros[") && key.contains("].idPersonal")) {
                    String indexStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                    int index = Integer.parseInt(indexStr);
                    if (index > maxIndex) maxIndex = index;
                }
            }

            log.info("Total registros detectados: {}", maxIndex + 1);

            // Procesar cada registro
            for (int i = 0; i <= maxIndex; i++) {
                String idPersonalStr = request.getParameter("registros[" + i + "].idPersonal");
                String idOcurrenciaStr = request.getParameter("registros[" + i + "].idOcurrencia");

                if (idPersonalStr == null || idOcurrenciaStr == null) {
                    continue; // Saltar si no hay datos
                }

                Integer idPersonal = Integer.parseInt(idPersonalStr);
                Integer idOcurrencia = Integer.parseInt(idOcurrenciaStr);

                // Obtener personal
                Personal personal = personalService.obtenerPorId(idPersonal)
                        .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + idPersonal));

                // Obtener ocurrencia
                Ocurrencia ocurrencia = ocurrenciaService.obtenerPorId(idOcurrencia)
                        .orElseThrow(() -> new RuntimeException("Ocurrencia no encontrada: " + idOcurrencia));

                // Crear entidad RegistroDiario
                RegistroDiario registro = new RegistroDiario();

                // Campos básicos
                registro.setFechaRegistro(fechaRegistro);
                registro.setPersonal(personal);
                registro.setCodigo(personal.getCodigo());
                registro.setAntiguedad(personal.getAntiguedad());
                registro.setDepartamento(personal.getDepartamento());

                // Snapshots históricos
                registro.setNivel(personal.getNivel());
                registro.setNivelSnapshot(personal.getNivel().getDescripcionCorta());
                registro.setEspecialidad(personal.getEspecialidad());
                registro.setEspecialidadSnapshot(personal.getEspecialidad().getDescripcionCorta());
                registro.setFullName(personal.getApPat() + " " +
                        personal.getApMat() + " " +
                        personal.getNombres());

                // Ocurrencia
                registro.setOcurrencia(ocurrencia);

                // Auditoría
                registro.setCreatedBy(codigoUsuario);

                // Solo asignar fechas y detalle si la ocurrencia NO es "Asistió" (id = 1)
                if (idOcurrencia != 1) {
                    String fechaInicioStr = request.getParameter("registros[" + i + "].fechaInicio");
                    String fechaTerminoStr = request.getParameter("registros[" + i + "].fechaTermino");
                    String detalle = request.getParameter("registros[" + i + "].detalle");

                    if (fechaInicioStr != null && !fechaInicioStr.isEmpty()) {
                        registro.setFechaInicio(LocalDate.parse(fechaInicioStr));
                    }
                    if (fechaTerminoStr != null && !fechaTerminoStr.isEmpty()) {
                        registro.setFechaTermino(LocalDate.parse(fechaTerminoStr));
                    }
                    if (detalle != null && !detalle.isEmpty()) {
                        registro.setDetalle(detalle);
                    }
                } else {
                    // Para "Asistió", dejar fechas y detalle en null
                    registro.setFechaInicio(null);
                    registro.setFechaTermino(null);
                    registro.setDetalle(null);
                }

                registrosParaGuardar.add(registro);
            }

            // Guardar TODO en una sola transacción
            log.info("Guardando {} registros de asistencia...", registrosParaGuardar.size());
            registroDiarioService.guardarRegistrosEnBloque(registrosParaGuardar);

            log.info("Registro de asistencia guardado exitosamente para la fecha: {}", fechaRegistro);
            redirectAttributes.addFlashAttribute("success",
                    "Registro de asistencia guardado exitosamente. Total registros: " + registrosParaGuardar.size());

            // Redirigir a la página de impresión
            return "redirect:/asistencia/imprimir";

        } catch (Exception e) {
            log.error("Error al guardar registro de asistencia en bloque: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al guardar el registro: " + e.getMessage());
            return "redirect:/asistencia/registrar";
        }
    }

    /**
     * Muestra los registros para una fecha específica
     */
    @GetMapping("/ver")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_APP', 'ADMIN_SYS')")
    public String verRegistros(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam(required = false) LocalDate fecha,
                            Model model) {

        log.info("Consultando registros para usuario: {}", userDetails.getUsername());

        try {
            Integer idDepartamento = userDetails.getIdDepartamento();
            LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();

            // Obtener departamento
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Obtener registros del día
            List<RegistroDiario> registros = registroDiarioService.listarPorDepartamentoYFecha(idDepartamento, fechaConsulta);

            // Agrupar por ocurrencia
            Map<String, List<RegistroDiario>> registrosPorOcurrencia = registros.stream()
                    .collect(Collectors.groupingBy(p -> p.getOcurrencia().getDescripcion()));

            model.addAttribute("departamento", departamento);
            model.addAttribute("fechaConsulta", fechaConsulta);
            model.addAttribute("registros", registros);
            model.addAttribute("registrosPorOcurrencia", registrosPorOcurrencia);
            model.addAttribute("totalRegistros", registros.size());

            return "asistencia/ver";

        } catch (Exception e) {
            log.error("Error al consultar registros: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los registros: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Muestra la página de impresión del registro de asistencia
     * Sin fecha pre-seleccionada - el usuario debe seleccionar
     */
    @GetMapping("/imprimir")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    public String mostrarImprimir(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam(required = false) LocalDate fecha,
                                  Model model) {

        log.info("Mostrando página de impresión para usuario: {}", userDetails.getUsername());

        try {
            // Obtener el departamento del usuario logueado
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Obtener todas las ocurrencias disponibles (para referencia)
            List<Ocurrencia> ocurrencias = ocurrenciaService.listarTodas();

            // Si se proporciona una fecha, cargar los datos del registro
            if (fecha != null) {
                log.info("Cargando datos del registro para fecha: {}", fecha);
                cargarDatosRegistro(idDepartamento, fecha, model);
            }

            // Datos básicos para el modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("fechaSeleccionada", fecha);
            model.addAttribute("ocurrencias", ocurrencias);

            return "asistencia/imprimir";

        } catch (Exception e) {
            log.error("Error al cargar página de impresión: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la página: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Muestra la página de actualización del último registro de asistencia
     * Solo se puede actualizar el último registro
     */
    @GetMapping("/actualizar")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    public String mostrarActualizar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {

        log.info("Mostrando formulario de actualización de registro para usuario: {}", userDetails.getUsername());

        try {
            // Obtener el departamento del usuario logueado
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Obtener fecha del último registro
            LocalDate fechaUltimoRegistro = registroDiarioService.obtenerFechaUltimoRegistro(idDepartamento);

            if (fechaUltimoRegistro == null) {
                model.addAttribute("error", "No existe ningún registro para actualizar");
                model.addAttribute("departamento", departamento);
                return "asistencia/actualizar";
            }

            // Obtener registros del último registro
            List<RegistroDiario> registrosUltimo = registroDiarioService.listarPorDepartamentoYFecha(idDepartamento, fechaUltimoRegistro);

            // Separar en 3 grupos (IGUAL QUE REGISTRAR):

            // 1. Grupo 1 que ASISTIERON (id_ocurrencia = 1)
            List<RegistroDiario> grupo1 = registrosUltimo.stream()
                    .filter(p -> p.getNivel() != null && p.getNivel().getTipoPersonal() != null)
                    .filter(p -> p.getNivel().getTipoPersonal().getIdTipoPersonal() == 1)
                    .filter(p -> p.getOcurrencia().getIdOcurrencia() == 1)
                    .collect(Collectors.toList());

            // 2. Grupo 2 que ASISTIERON (id_ocurrencia = 1)
            List<RegistroDiario> grupo2 = registrosUltimo.stream()
                    .filter(p -> p.getNivel() != null && p.getNivel().getTipoPersonal() != null)
                    .filter(p -> p.getNivel().getTipoPersonal().getIdTipoPersonal() == 2)
                    .filter(p -> p.getOcurrencia().getIdOcurrencia() == 1)
                    .collect(Collectors.toList());

            // 3. Ocurrencias (TODOS los que NO asistieron, sin importar si son Grupo 1 o Grupo 2)
            List<RegistroDiario> ocurrenciasPreLlenadas = registrosUltimo.stream()
                    .filter(p -> p.getOcurrencia().getIdOcurrencia() != 1)
                    .collect(Collectors.toList());

            // Obtener todas las ocurrencias disponibles
            List<Ocurrencia> ocurrencias = ocurrenciaService.listarTodas();

            // Datos para el modelo
            model.addAttribute("departamento", departamento);
            model.addAttribute("fechaRegistro", fechaUltimoRegistro);
            model.addAttribute("grupo1", grupo1);
            model.addAttribute("grupo2", grupo2);
            model.addAttribute("ocurrenciasPreLlenadas", ocurrenciasPreLlenadas);
            model.addAttribute("ocurrencias", ocurrencias);
            model.addAttribute("totalPersonal", registrosUltimo.size());
            model.addAttribute("esActualizacion", true); // Flag para diferenciar de registrar

            log.info("Último registro cargado - Fecha: {}, Total: {}, Grupo 1: {}, Grupo 2: {}, Ocurrencias: {}",
                    fechaUltimoRegistro, registrosUltimo.size(), grupo1.size(), grupo2.size(), ocurrenciasPreLlenadas.size());

            return "asistencia/actualizar";

        } catch (Exception e) {
            log.error("Error al cargar formulario de actualización: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Actualiza el último registro de asistencia
     * Elimina el registro anterior y guarda los nuevos datos
     */
    @PostMapping("/actualizar")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_SYS')")
    public String actualizarRegistro(HttpServletRequest request,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        try {
            // Extraer fecha del registro
            String fechaRegistroStr = request.getParameter("fechaRegistro");
            LocalDate fechaRegistro = LocalDate.parse(fechaRegistroStr);

            log.info("Iniciando actualización de registro de asistencia para fecha: {}", fechaRegistro);

            // Obtener datos del usuario
            Integer idDepartamento = userDetails.getIdDepartamento();
            String codigoUsuario = userDetails.getUsername();

            // VALIDACIÓN: Verificar que sea el último registro
            LocalDate fechaUltimoRegistro = registroDiarioService.obtenerFechaUltimoRegistro(idDepartamento);

            if (fechaUltimoRegistro == null) {
                redirectAttributes.addFlashAttribute("error", "No existe ningún registro");
                return "redirect:/asistencia/actualizar";
            }

            if (!fechaRegistro.equals(fechaUltimoRegistro)) {
                log.warn("Intento de actualizar registro que no es el último. Fecha: {}, Último: {}",
                        fechaRegistro, fechaUltimoRegistro);
                redirectAttributes.addFlashAttribute("error",
                        "Solo se puede actualizar el último registro. Último registro: " + fechaUltimoRegistro);
                return "redirect:/asistencia/actualizar";
            }

            // Construir lista de nuevos registros (mismo código que guardar-bloque)
            Map<String, String[]> paramMap = request.getParameterMap();
            List<RegistroDiario> nuevosRegistros = new ArrayList<>();

            // Detectar índices
            int maxIndex = -1;
            for (String key : paramMap.keySet()) {
                if (key.startsWith("registros[") && key.contains("].idPersonal")) {
                    String indexStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                    int index = Integer.parseInt(indexStr);
                    if (index > maxIndex) maxIndex = index;
                }
            }

            log.info("Total registros detectados: {}", maxIndex + 1);

            // Procesar cada registro
            for (int i = 0; i <= maxIndex; i++) {
                String idPersonalStr = request.getParameter("registros[" + i + "].idPersonal");
                String idOcurrenciaStr = request.getParameter("registros[" + i + "].idOcurrencia");

                if (idPersonalStr == null || idOcurrenciaStr == null) {
                    continue;
                }

                Integer idPersonal = Integer.parseInt(idPersonalStr);
                Integer idOcurrencia = Integer.parseInt(idOcurrenciaStr);

                // Obtener personal
                Personal personal = personalService.obtenerPorId(idPersonal)
                        .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + idPersonal));

                // Obtener ocurrencia
                Ocurrencia ocurrencia = ocurrenciaService.obtenerPorId(idOcurrencia)
                        .orElseThrow(() -> new RuntimeException("Ocurrencia no encontrada: " + idOcurrencia));

                // Crear entidad RegistroDiario
                RegistroDiario registro = new RegistroDiario();

                // Campos básicos
                registro.setFechaRegistro(fechaRegistro);
                registro.setPersonal(personal);
                registro.setCodigo(personal.getCodigo());
                registro.setAntiguedad(personal.getAntiguedad());
                registro.setDepartamento(personal.getDepartamento());

                // Snapshots históricos
                registro.setNivel(personal.getNivel());
                registro.setNivelSnapshot(personal.getNivel().getDescripcionCorta());
                registro.setEspecialidad(personal.getEspecialidad());
                registro.setEspecialidadSnapshot(personal.getEspecialidad().getDescripcionCorta());
                registro.setFullName(personal.getApPat() + " " +
                        personal.getApMat() + " " +
                        personal.getNombres());

                // Ocurrencia
                registro.setOcurrencia(ocurrencia);

                // Auditoría
                registro.setCreatedBy(codigoUsuario);

                // Solo asignar fechas y detalle si la ocurrencia NO es "Asistió" (id = 1)
                if (idOcurrencia != 1) {
                    String fechaInicioStr = request.getParameter("registros[" + i + "].fechaInicio");
                    String fechaTerminoStr = request.getParameter("registros[" + i + "].fechaTermino");
                    String detalle = request.getParameter("registros[" + i + "].detalle");

                    if (fechaInicioStr != null && !fechaInicioStr.isEmpty()) {
                        registro.setFechaInicio(LocalDate.parse(fechaInicioStr));
                    }
                    if (fechaTerminoStr != null && !fechaTerminoStr.isEmpty()) {
                        registro.setFechaTermino(LocalDate.parse(fechaTerminoStr));
                    }
                    if (detalle != null && !detalle.isEmpty()) {
                        registro.setDetalle(detalle);
                    }
                } else {
                    registro.setFechaInicio(null);
                    registro.setFechaTermino(null);
                    registro.setDetalle(null);
                }

                nuevosRegistros.add(registro);
            }

            // Actualizar (elimina el anterior y guarda los nuevos)
            log.info("Actualizando registro con {} registros...", nuevosRegistros.size());
            registroDiarioService.actualizarRegistro(idDepartamento, fechaRegistro, nuevosRegistros);

            log.info("Registro actualizado exitosamente para la fecha: {}", fechaRegistro);
            redirectAttributes.addFlashAttribute("success",
                    "Registro actualizado exitosamente. Total registros: " + nuevosRegistros.size());

            // Redirigir a la página de impresión (sin fecha)
            return "redirect:/asistencia/imprimir";

        } catch (Exception e) {
            log.error("Error al actualizar registro de asistencia: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al actualizar el registro: " + e.getMessage());
            return "redirect:/asistencia/actualizar";
        }
    }

    /**
     * Carga los datos del registro en el modelo para imprimir
     * Método privado de apoyo
     */
    private void cargarDatosRegistro(Integer idDepartamento, LocalDate fecha, Model model) {

        // Verificar que existe el registro
        boolean existeRegistro = registroDiarioService.existeRegistroPorDepartamentoYFecha(idDepartamento, fecha);

        if (!existeRegistro) {
            model.addAttribute("error", "No existe registro para la fecha " + fecha);
            return;
        }

        // 1. Obtener resumen (cuadro de efectivos/descuentos/disponibles)
        ResumenRegistroDTO resumen = registroDiarioService.obtenerResumenRegistro(idDepartamento, fecha);
        model.addAttribute("resumen", resumen);

        // 2. Obtener ocurrencias agrupadas por tipo - GRUPO 1
        Map<String, List<RegistroDiario>> ocurrenciasGrupo1 =
                registroDiarioService.obtenerOcurrenciasAgrupadasPorTipo(idDepartamento, fecha, 1);
        model.addAttribute("ocurrenciasGrupo1", ocurrenciasGrupo1);

        // 3. Obtener ocurrencias agrupadas por tipo - GRUPO 2
        Map<String, List<RegistroDiario>> ocurrenciasGrupo2 =
                registroDiarioService.obtenerOcurrenciasAgrupadasPorTipo(idDepartamento, fecha, 2);
        model.addAttribute("ocurrenciasGrupo2", ocurrenciasGrupo2);

        // 4. Obtener personal presente (mezclados, ordenados por antigüedad)
        List<RegistroDiario> personalPresente =
                registroDiarioService.obtenerPersonalPresenteRegistro(idDepartamento, fecha);
        model.addAttribute("personalPresente", personalPresente);

        log.info("Datos del registro cargados - Efectivos: {}, Descuentos: {}, Presentes: {}",
                resumen.getEfectivosTotal(),
                resumen.getDescuentosTotal(),
                personalPresente.size());
    }

    // ========== ENDPOINTS PARA CONSOLIDADO (ADMIN_APP) ==========

    /**
     * Muestra el consolidado general de asistencia (ADMIN_APP)
     * Vista principal con datepicker y botón para imprimir
     */
    @GetMapping("/consolidado")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String mostrarConsolidado(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam(required = false) LocalDate fecha,
                                     Model model) {

        LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now();

        log.info("Mostrando consolidado del registro para fecha: {}", fechaConsulta);

        try {
            // Obtener la empresa del usuario ADMIN_APP
            // El usuario ADMIN_APP tiene un departamento asignada, y ese departamento tiene una empresa
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"));

            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            log.info("Usuario ADMIN_APP pertenece a la empresa ID: {} ({})",
                    idEmpresa, departamentoUsuario.getEmpresa().getDescripcionCorta());

            // Obtener consolidado del registro FILTRADO por empresa
            ConsolidadoRegistroDTO consolidado = registroDiarioService.obtenerConsolidadoPorFecha(fechaConsulta, idEmpresa);

            model.addAttribute("consolidado", consolidado);
            model.addAttribute("fechaSeleccionada", fechaConsulta);

            log.info("Consolidado cargado - Total efectivos: {}, Total descuentos: {}, Total disponibles: {}",
                    consolidado.getTotalEfectivosGeneral(),
                    consolidado.getTotalDescuentosGeneral(),
                    consolidado.getTotalDisponiblesGeneral());

            return "asistencia/consolidado";

        } catch (Exception e) {
            log.error("Error al cargar consolidado: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el consolidado: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Vista previa vertical del registro de un departamento específica,
     * para ser cargada dentro de un iframe desde el consolidado.
     * Verifica que el departamento solicitada pertenezca a la empresa del usuario.
     */
    @GetMapping("/consolidado/departamento/{idDepartamento}/vista")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String verRegistroDepartamentoVertical(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @PathVariable Integer idDepartamento,
                                            @RequestParam LocalDate fecha,
                                            Model model) {

        log.info("Vista previa de registro de departamento {} para modal, fecha {}", idDepartamento, fecha);

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"));
            Integer idEmpresaUsuario = departamentoUsuario.getEmpresa().getIdEmpresa();

            Departamento departamentoSolicitada = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            if (departamentoSolicitada.getEmpresa() == null
                    || !departamentoSolicitada.getEmpresa().getIdEmpresa().equals(idEmpresaUsuario)) {
                log.warn("Acceso denegado: usuario de empresa {} intentó ver departamento {} de otra empresa",
                        idEmpresaUsuario, idDepartamento);
                model.addAttribute("error", "No tiene acceso a este departamento");
                return "asistencia/registro-departamento";
            }

            cargarDatosRegistro(idDepartamento, fecha, model);

            return "asistencia/registro-departamento";

        } catch (Exception e) {
            log.error("Error al cargar vista previa de departamento: {}", e.getMessage(), e);
            model.addAttribute("error", "No se pudo cargar el registro de este departamento");
            return "asistencia/registro-departamento";
        }
    }

    /**
     * Página de impresión del consolidado general
     * Se abre automáticamente con window.print()
     */
    @GetMapping("/consolidado/imprimir")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String imprimirConsolidado(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @RequestParam LocalDate fecha,
                                      Model model) {

        log.info("Generando impresión del consolidado para fecha: {}", fecha);

        try {
            // Obtener la empresa del usuario ADMIN_APP
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"));

            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            // Obtener consolidado del registro FILTRADO por empresa
            ConsolidadoRegistroDTO consolidado = registroDiarioService.obtenerConsolidadoPorFecha(fecha, idEmpresa);

            model.addAttribute("consolidado", consolidado);
            model.addAttribute("fechaSeleccionada", fecha);

            return "asistencia/consolidado-imprimir";

        } catch (Exception e) {
            log.error("Error al generar impresión del consolidado: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar la impresión: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Descarga el consolidado general en PDF (mismo contenido que consolidado-imprimir.html)
     * Generado 100% en servidor con openhtmltopdf, sin depender del diálogo de impresión.
     */
    @GetMapping("/consolidado/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public void descargarConsolidadoPdf(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestParam LocalDate fecha,
                                         HttpServletResponse response) throws IOException {

        log.info("Generando PDF del consolidado para fecha: {}", fecha);

        try {
            Integer idDepartamento = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"));

            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            ConsolidadoRegistroDTO consolidado = registroDiarioService.obtenerConsolidadoPorFecha(fecha, idEmpresa);

            Context context = new Context();
            context.setVariable("consolidado", consolidado);
            context.setVariable("fechaSeleccionada", fecha);

            String html = templateEngine.process("asistencia/consolidado-imprimir", context);

            byte[] pdfBytes = pdfGeneratorService.convertirHtmlAPdf(html);

            String nombreArchivo = "consolidado_" + fecha.toString() + ".pdf";

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("Error al generar PDF del consolidado: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo generar el PDF");
        }
    }

    /**
     * Elimina el último registro de un departamento.
     * Solo ADMIN_SYS — operación de emergencia para casos de fuerza mayor.
     */
    @PostMapping("/eliminar-ultimo")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_DPTO')")
    @Transactional
    public String eliminarUltimoRegistro(@RequestParam Integer idDepartamento,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {

        log.info("ADMIN_SYS {} solicitó eliminar último registro de departamento ID: {}",
                userDetails.getUsername(), idDepartamento);

        try {
            // Obtener fecha del último registro antes de eliminar (para el mensaje)
            LocalDate fechaUltimo = registroDiarioService.obtenerFechaUltimoRegistro(idDepartamento);

            if (!userDetails.hasRole("ADMIN_SYS")) {
                // ADMIN_DPTO: forzar su propia departamento, ignorar cualquier idDepartamento
                // que venga en la petición (nunca confiar en ese parámetro para no-ADMIN_SYS).
                idDepartamento = userDetails.getIdDepartamento();
                fechaUltimo = registroDiarioService.obtenerFechaUltimoRegistro(idDepartamento);

                if (fechaUltimo == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "No existe ningún registro para tu departamento.");
                    return "redirect:/asistencia/actualizar";
                }

                if (!fechaUltimo.isAfter(LocalDate.now())) {
                    redirectAttributes.addFlashAttribute("error",
                            "Solo puedes eliminar un registro que hayas registrado por adelantado "
                            + "(una fecha futura). No puedes eliminar el registro de hoy ni de fechas pasadas.");
                    return "redirect:/asistencia/actualizar";
                }
            }

            if (fechaUltimo == null) {
                redirectAttributes.addFlashAttribute("error",
                        "No existe ningún registro para este departamento.");
                return "redirect:/asistencia/actualizar";
            }

            registroDiarioService.eliminarUltimoRegistro(idDepartamento);

            redirectAttributes.addFlashAttribute("success",
                    "Registro del " + fechaUltimo + " eliminado correctamente. " +
                            "Ahora puede realizar los cambios necesarios.");

            log.warn("REGISTRO ELIMINADO — Departamento: {} | Fecha: {} | Por: {}",
                    idDepartamento, fechaUltimo, userDetails.getUsername());

            return "redirect:/asistencia/actualizar";

        } catch (IllegalArgumentException e) {
            log.warn("Error al eliminar registro: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/asistencia/ver";
        } catch (Exception e) {
            log.error("Error inesperado al eliminar registro: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al eliminar el registro: " + e.getMessage());
            return "redirect:/asistencia/ver";
        }
    }

    /**
     * Vista de administración de registros — solo ADMIN_SYS
     * Permite buscar y eliminar registros por departamento y fecha
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String vistaAdmin(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(required = false) Integer idDepartamento,
                             @RequestParam(required = false) LocalDate fecha,
                             Model model) {

        log.info("Vista admin registros — usuario: {}", userDetails.getUsername());

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            List<Departamento> departamentos = departamentoService.listarPorEmpresa(idEmpresa);

            List<RegistroDiario> registros = null;
            boolean busquedaRealizada = false;

            if (idDepartamento != null && fecha != null) {
                registros = registroDiarioService.listarPorDepartamentoYFechaAdmin(idDepartamento, fecha);
                busquedaRealizada = true;
            }

            model.addAttribute("departamentos",         departamentos);
            model.addAttribute("departamentoUsuario",   departamentoUsuario);
            model.addAttribute("idDepartamento",        idDepartamento);
            model.addAttribute("fecha",             fecha);
            model.addAttribute("registros",            registros);
            model.addAttribute("busquedaRealizada", busquedaRealizada);
            model.addAttribute("pageTitle",         "Administración de Registros");
            model.addAttribute("headerTitle",       "Administración de Registros");

            return "asistencia/admin";

        } catch (Exception e) {
            log.error("Error en vista admin: {}", e.getMessage(), e);
            model.addAttribute("error", "Error: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Eliminar registro por departamento y fecha — solo ADMIN_SYS
     */
    @PostMapping("/admin/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    public String eliminarRegistro(@RequestParam Integer idDepartamento,
                                @RequestParam LocalDate fecha,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

        log.warn("Usuario {} eliminando registro Departamento: {} fecha: {}",
                userDetails.getUsername(), idDepartamento, fecha);

        boolean esAdminSys = userDetails.hasRole("ADMIN_SYS");
        if (!esAdminSys && !fecha.equals(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error",
                    "Como Admin App, solo puedes eliminar el registro del día de hoy. "
                    + "Para eliminar registros de fechas anteriores, contacta a Sistemas (ADMIN_SYS).");
            return "redirect:/asistencia/admin?idDepartamento=" + idDepartamento +
                    "&fecha=" + fecha;
        }

        try {
            registroDiarioService.eliminarRegistroPorDepartamentoYFecha(idDepartamento, fecha);

            redirectAttributes.addFlashAttribute("success",
                    "Registro del " + fecha + " eliminado correctamente. "
                    + "Si existían registros posteriores, también fueron eliminados "
                    + "para mantener la consistencia de los datos.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al eliminar registro: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al eliminar: " + e.getMessage());
        }

        return "redirect:/asistencia/admin?idDepartamento=" + idDepartamento +
                "&fecha=" + fecha;
    }


}