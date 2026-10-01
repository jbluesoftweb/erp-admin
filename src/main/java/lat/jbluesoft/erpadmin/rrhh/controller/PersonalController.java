package lat.jbluesoft.erpadmin.rrhh.controller;

import lat.jbluesoft.erpadmin.rrhh.model.*;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.rrhh.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalService  personalService;
    private final DepartamentoService  departamentoService;
    private final NivelService     nivelService;
    private final EspecialidadService      especialidadService;

    // =============================================
    // LISTAR PERSONAL
    // =============================================

    /**
     * ADMIN_DPTO : solo su departamento, sin filtro adicional
     * ADMIN_APP / ADMIN_SYS : todo el personal de la Empresa, con filtro por departamento
     */
    @GetMapping("/listar")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String listarPersonal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam(required = false) Integer idDepartamento,
                                 @RequestParam(required = false) String buscar,
                                 Model model) {

        log.info("Listando personal - usuario: {}", userDetails.getUsername());

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            boolean esAdminSys  = userDetails.hasRole("ADMIN_SYS");
            boolean esAdminApp  = userDetails.hasRole("ADMIN_APP");
            boolean esAdminDepto = userDetails.hasRole("ADMIN_DPTO") && !esAdminApp && !esAdminSys;

            List<Departamento> departamentos;
            List<Personal> listaPersonal;
            String headerTitle;

            if (esAdminDepto) {
                // ADMIN_DPTO: solo ve su departamento, sin filtro dropdown
                departamentos = List.of(departamentoUsuario);
                headerTitle = "Personal de la " + departamentoUsuario.getDescripcionCorta();

                if (buscar != null && !buscar.trim().isEmpty()) {
                    listaPersonal = personalService.buscarPorCodigoONombre(buscar.trim())
                            .stream()
                            .filter(p -> p.getDepartamento().getIdDepartamento().equals(idDepartamentoUsuario))
                            .toList();
                } else {
                    listaPersonal = personalService.listarPorDepartamentoConRelaciones(idDepartamentoUsuario);
                }

            } else {
                // ADMIN_APP / ADMIN_SYS: toda la Empresa con filtro por departamento
                departamentos = departamentoService.listarPorEmpresa(idEmpresa);
                headerTitle = "Personal de la Empresa";

                if (buscar != null && !buscar.trim().isEmpty()) {
                    listaPersonal = personalService.buscarPorCodigoONombreEnEmpresa(buscar.trim(), idEmpresa);
                } else if (idDepartamento != null) {
                    listaPersonal = personalService.listarPorDepartamentoConRelaciones(idDepartamento);
                } else {
                    // ADMIN_APP / ADMIN_SYS sin filtro de departamento: Listar todo por antigüedad global (sin agrupar por departamento)
                    listaPersonal = personalService.listarPorEmpresaOrdenadoAntiguedad(idEmpresa);
                }
            }

            model.addAttribute("listaPersonal",         listaPersonal);
            model.addAttribute("departamentos",              departamentos);
            model.addAttribute("idDepartamentoSeleccionada", idDepartamento);
            model.addAttribute("buscar",                 buscar);
            model.addAttribute("departamento",               departamentoUsuario);
            model.addAttribute("headerTitle",            headerTitle);
            model.addAttribute("pageTitle",              "Listar Personal");
            model.addAttribute("esAdminSys",             esAdminSys);
            model.addAttribute("esAdminApp",             esAdminApp);
            model.addAttribute("esAdminDepto",           esAdminDepto);

            log.info("Personal cargado: {} registros", listaPersonal.size());
            return "personal/listar-personal";

        } catch (Exception e) {
            log.error("Error al listar personal: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el personal: " + e.getMessage());
            return "error";
        }
    }

    // =============================================
    // AGREGAR / BUSCAR EN SIN_EMPRESA (ADMIN_APP y ADMIN_SYS)
    // =============================================

    /**
     * Muestra el formulario de búsqueda.
     * Busca SOLO en SIN_EMPRESA para ADMIN_APP y ADMIN_SYS.
     */
    @GetMapping("/agregar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String mostrarAgregar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {

        log.info("Mostrando página agregar personal - usuario: {}", userDetails.getUsername());

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            boolean esAdminSys = userDetails.hasRole("ADMIN_SYS");

            // Solo departamentos reales de la unidad (excluye SIN_DPTO)
            List<Departamento> departamentos = departamentoService.listarPorEmpresa(idEmpresa);

            model.addAttribute("departamento",     departamentoUsuario);
            model.addAttribute("departamentos",    departamentos);
            model.addAttribute("headerTitle",  "Agregar Personal");
            model.addAttribute("pageTitle",    "Agregar Personal");
            model.addAttribute("esAdminSys",   esAdminSys);
            model.addAttribute("rolActual",    userDetails.getAuthorities().iterator().next()
                    .getAuthority().replace("ROLE_", ""));

            return "personal/registrar-persona";

        } catch (Exception e) {
            log.error("Error al mostrar agregar personal: {}", e.getMessage(), e);
            model.addAttribute("error", "Error: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Busca personal en SIN_EMPRESA por código o apellidos/nombres.
     */
    @PostMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String buscarPersonal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam String query,
                                 Model model) {

        log.info("Buscando personal en SIN_EMPRESA: {}", query);

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            boolean esAdminSys = userDetails.hasRole("ADMIN_SYS");
            List<Departamento> departamentos = departamentoService.listarPorEmpresa(idEmpresa);

            // Buscar SOLO en SIN_EMPRESA
            List<Personal> resultados = personalService.buscarEnSinEmpresa(query.trim());

            model.addAttribute("departamento",        departamentoUsuario);
            model.addAttribute("departamentos",       departamentos);
            model.addAttribute("resultados",      resultados);
            model.addAttribute("query",           query);
            model.addAttribute("busquedaRealizada", true);
            model.addAttribute("headerTitle",     "Agregar Personal");
            model.addAttribute("pageTitle",       "Agregar Personal");
            model.addAttribute("esAdminSys",      esAdminSys);
            model.addAttribute("rolActual",       userDetails.getAuthorities().iterator().next()
                    .getAuthority().replace("ROLE_", ""));

            return "personal/registrar-persona";

        } catch (Exception e) {
            log.error("Error en búsqueda de personal: {}", e.getMessage(), e);
            model.addAttribute("error", "Error en la búsqueda: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Asigna personal de SIN_EMPRESA a un departamento de la unidad del usuario.
     */
    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional
    public String asignarPersonal(@RequestParam Integer idPersonal,
                                  @RequestParam Integer idDepartamento,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        log.info("Asignando personal ID: {} a departamento ID: {}", idPersonal, idDepartamento);

        try {
            Personal personal = personalService.buscarPorId(idPersonal)
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

            // Validar que el personal venga de SIN_EMPRESA
            String empresaPersonal = personal.getDepartamento().getEmpresa().getDescripcionCorta();
            if (!"SIN_EMPRESA".equals(empresaPersonal)) {
                redirectAttributes.addFlashAttribute("error",
                        personal.getFullName() + " ya pertenece a la unidad: " + empresaPersonal
                                + ". Solo se puede incorporar personal de SIN_EMPRESA.");
                return "redirect:/personal/agregar";
            }

            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Validar que el departamento destino pertenezca a la unidad del usuario
            Integer idEmpresaUsuario = departamentoService.buscarPorId(userDetails.getIdDepartamento())
                    .orElseThrow(() -> new RuntimeException("Departamento del usuario no encontrado"))
                    .getEmpresa().getIdEmpresa();

            if (!departamento.getEmpresa().getIdEmpresa().equals(idEmpresaUsuario)) {
                redirectAttributes.addFlashAttribute("error",
                        "No puede asignar personal a un departamento fuera de su unidad.");
                return "redirect:/personal/agregar";
            }

            personal.setDepartamento(departamento);
            personalService.actualizar(personal);

            redirectAttributes.addFlashAttribute("success",
                    personal.getFullName() + " incorporado correctamente a "
                            + departamento.getDescripcionCorta());
            return "redirect:/personal/listar";

        } catch (Exception e) {
            log.error("Error al asignar personal: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al asignar: " + e.getMessage());
            return "redirect:/personal/agregar";
        }
    }

    // =============================================
    // REGISTRAR NUEVO PERSONAL (solo ADMIN_SYS)
    // =============================================

    @GetMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String mostrarRegistrar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {

        log.info("Mostrando formulario registrar nuevo personal - usuario: {}", userDetails.getUsername());

        try {
            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            model.addAttribute("departamento",      departamentoUsuario);
            model.addAttribute("departamentos",     departamentoService.listarPorEmpresa(idEmpresa));
            model.addAttribute("tipoPersonales",  nivelService.listarTipoPersonales());
            model.addAttribute("niveles",        nivelService.listarTodos());
            model.addAttribute("especialidades",         especialidadService.listarTodas());
            model.addAttribute("headerTitle",   "Registrar Personal");
            model.addAttribute("pageTitle",     "Registrar Personal");

            return "personal/registrar-nuevo";

        } catch (Exception e) {
            log.error("Error al cargar formulario de registro: {}", e.getMessage(), e);
            model.addAttribute("error", "Error: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Guarda nuevo personal.
     * Valida que código/DNI no existan en otra unidad activa.
     */
    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN_APP', 'ADMIN_SYS')")
    @Transactional
    public String guardarPersonal(@RequestParam String codigo,
                                  @RequestParam String dni,
                                  @RequestParam String apPat,
                                  @RequestParam String apMat,
                                  @RequestParam String nombres,
                                  @RequestParam Integer idNivel,
                                  @RequestParam Integer idEspecialidad,
                                  @RequestParam Integer idDepartamento,
                                  @RequestParam Integer antiguedad,
                                  @RequestParam(required = false) String cargo,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        log.info("Guardando nuevo personal código: {}", codigo);

        try {
            // Verificar si el código ya existe
            personalService.buscarPorCodigo(codigo.trim()).ifPresent(p -> {
                String empresa = p.getDepartamento().getEmpresa().getDescripcionCorta();
                if (!"SIN_EMPRESA".equals(empresa)) {
                    throw new IllegalArgumentException(
                            "El código " + codigo + " ya pertenece a la unidad: " + empresa
                                    + ". Si desea incorporarlo, use la opción AGREGAR.");
                }
            });

            // Verificar si el DNI ya existe
            personalService.buscarPorDni(dni.trim()).ifPresent(p -> {
                String empresa = p.getDepartamento().getEmpresa().getDescripcionCorta();
                if (!"SIN_EMPRESA".equals(empresa)) {
                    throw new IllegalArgumentException(
                            "El DNI " + dni + " ya pertenece a la unidad: " + empresa + ".");
                }
            });

            Nivel nivel = nivelService.buscarPorId(idNivel)
                    .orElseThrow(() -> new RuntimeException("Nivel no encontrado"));
            Especialidad especialidad = especialidadService.buscarPorId(idEspecialidad)
                    .orElseThrow(() -> new RuntimeException("Especialidad no encontrada"));
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

            // Validar coherencia nivel/especialidad (mismo tipo de personal)
            if (!nivel.getTipoPersonal().getIdTipoPersonal()
                    .equals(especialidad.getTipoPersonal().getIdTipoPersonal())) {
                throw new IllegalArgumentException(
                        "El nivel y la especialidad no corresponden al mismo tipo de personal.");
            }

            Personal personal = new Personal();
            personal.setCodigo(codigo.trim());
            personal.setDni(dni.trim());
            personal.setApPat(apPat.trim().toUpperCase());
            personal.setApMat(apMat.trim().toUpperCase());
            personal.setNombres(nombres.trim().toUpperCase());
            personal.setNivel(nivel);
            personal.setEspecialidad(especialidad);
            personal.setDepartamento(departamento);
            personal.setAntiguedad(antiguedad);
            personal.setCargo(cargo != null ? cargo.trim() : null);
            personal.setEnabled(true);
            personal.setCreatedBy(userDetails.getUsername());

            personalService.guardar(personal);

            redirectAttributes.addFlashAttribute("success",
                    "Personal registrado correctamente: " + personal.getFullName());
            return "redirect:/personal/listar";

        } catch (IllegalArgumentException e) {
            log.warn("Validación al registrar personal: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/personal/registrar";
        } catch (Exception e) {
            log.error("Error al guardar personal: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al registrar: " + e.getMessage());
            return "redirect:/personal/registrar";
        }
    }

    // =============================================
    // EDITAR PERSONAL
    // =============================================

    /**
     * ADMIN_DPTO : solo cargo, y solo si el personal es de su departamento
     * ADMIN_APP / ADMIN_SYS : todos los campos + selector TipoPersonal dinámico
     */
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_APP', 'ADMIN_SYS')")
    @Transactional(readOnly = true)
    public String mostrarEditar(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        log.info("Mostrando edición de personal ID: {} - usuario: {}", id, userDetails.getUsername());

        try {
            Personal personal = personalService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            Departamento departamentoUsuario = departamentoService.buscarPorId(idDepartamentoUsuario)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
            Integer idEmpresa = departamentoUsuario.getEmpresa().getIdEmpresa();

            boolean esAdminSys  = userDetails.hasRole("ADMIN_SYS");
            boolean esAdminApp  = userDetails.hasRole("ADMIN_APP");
            boolean esAdminDepto = userDetails.hasRole("ADMIN_DPTO") && !esAdminApp && !esAdminSys;

            // ADMIN_DPTO solo puede editar personal de su propio departamento
            if (esAdminDepto &&
                    !personal.getDepartamento().getIdDepartamento().equals(idDepartamentoUsuario)) {
                redirectAttributes.addFlashAttribute("error",
                        "No tiene permisos para editar personal de otro departamento.");
                return "redirect:/personal/listar";
            }

            model.addAttribute("personal",      personal);
            model.addAttribute("departamento",      departamentoUsuario);
            model.addAttribute("departamentos",     departamentoService.listarPorEmpresa(idEmpresa));
            model.addAttribute("tipoPersonales",  nivelService.listarTipoPersonales());
            model.addAttribute("niveles",        nivelService.listarTodos());
            model.addAttribute("especialidades",         especialidadService.listarTodas());
            model.addAttribute("headerTitle",   "Editar Personal");
            model.addAttribute("pageTitle",     "Editar Personal");
            model.addAttribute("esAdminSys",    esAdminSys);
            model.addAttribute("esAdminApp",    esAdminApp);
            model.addAttribute("esAdminDepto",  esAdminDepto);
            model.addAttribute("rolActual",     userDetails.getAuthorities().iterator().next()
                    .getAuthority().replace("ROLE_", ""));

            return "personal/editar-persona";

        } catch (Exception e) {
            log.error("Error al cargar edición: {}", e.getMessage(), e);
            model.addAttribute("error", "Error: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_DPTO', 'ADMIN_APP', 'ADMIN_SYS')")
    public String actualizarPersonal(@PathVariable Integer id,
                                     @RequestParam(required = false) Integer idNivel,
                                     @RequestParam(required = false) Integer idEspecialidad,
                                     @RequestParam(required = false) Integer idDepartamento,
                                     @RequestParam(required = false) Integer antiguedad,
                                     @RequestParam(required = false) String cargo,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {

        log.info("Actualizando personal ID: {} - usuario: {}", id, userDetails.getUsername());

        try {
            Personal personal = personalService.obtenerPorId(id)
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

            Integer idDepartamentoUsuario = userDetails.getIdDepartamento();
            boolean esAdminSys  = userDetails.hasRole("ADMIN_SYS");
            boolean esAdminApp  = userDetails.hasRole("ADMIN_APP");
            boolean esAdminDepto = userDetails.hasRole("ADMIN_DPTO") && !esAdminApp && !esAdminSys;

            // ADMIN_DPTO: solo puede actualizar cargo de su propio personal
            if (esAdminDepto) {
                if (!personal.getDepartamento().getIdDepartamento().equals(idDepartamentoUsuario)) {
                    redirectAttributes.addFlashAttribute("error",
                            "No tiene permisos para editar personal de otro departamento.");
                    return "redirect:/personal/listar";
                }
                if (cargo != null) {
                    personal.setCargo(cargo.trim());
                }
                personalService.actualizar(personal);
                redirectAttributes.addFlashAttribute("success",
                        "Cargo actualizado correctamente: " + personal.getFullName());
                return "redirect:/personal/listar";
            }

            // ADMIN_APP / ADMIN_SYS: pueden editar todos los campos
            if (idNivel != null && idEspecialidad != null) {
                Nivel nivel = nivelService.buscarPorId(idNivel)
                        .orElseThrow(() -> new RuntimeException("Nivel no encontrado"));
                Especialidad especialidad = especialidadService.buscarPorId(idEspecialidad)
                        .orElseThrow(() -> new RuntimeException("Especialidad no encontrada"));

                // Validar coherencia nivel/especialidad
                if (!nivel.getTipoPersonal().getIdTipoPersonal()
                        .equals(especialidad.getTipoPersonal().getIdTipoPersonal())) {
                    redirectAttributes.addFlashAttribute("error",
                            "El nivel y la especialidad no corresponden al mismo tipo de personal.");
                    return "redirect:/personal/editar/" + id;
                }
                personal.setNivel(nivel);
                personal.setEspecialidad(especialidad);
            }

            if (idDepartamento != null) {
                personal.setDepartamento(departamentoService.buscarPorId(idDepartamento)
                        .orElseThrow(() -> new RuntimeException("Departamento no encontrado")));
            }
            if (antiguedad != null) {
                personal.setAntiguedad(antiguedad);
            }
            if (cargo != null) {
                personal.setCargo(cargo.trim());
            }

            personalService.actualizar(personal);
            redirectAttributes.addFlashAttribute("success",
                    "Personal actualizado correctamente: " + personal.getFullName());
            return "redirect:/personal/listar";

        } catch (IllegalStateException e) {
            log.warn("Bloqueo por registro activo al actualizar: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/personal/editar/" + id;
        } catch (Exception e) {
            log.error("Error al actualizar personal: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/personal/editar/" + id;
        }
    }

    // =============================================
    // MOVER A SIN_EMPRESA (solo ADMIN_SYS)
    // =============================================

    /**
     * Mueve personal a SIN_DPTO / SIN_EMPRESA.
     * Lo deja disponible para que otra unidad lo incorpore.
     */
    @PostMapping("/mover-sin-unidad/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SYS', 'ADMIN_APP')")
    @Transactional
    public String moverASinEmpresa(@PathVariable Integer id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        log.info("Moviendo personal ID: {} a SIN_EMPRESA - usuario: {}", id, userDetails.getUsername());

        try {
            Personal personal = personalService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

            if (!userDetails.hasRole("ADMIN_SYS")) {
                if (personal.getDepartamento() == null
                        || personal.getDepartamento().getEmpresa() == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "No se pudo determinar la unidad de esta persona.");
                    return "redirect:/personal/listar";
                }

                Departamento departamentoActor = departamentoService.buscarPorId(userDetails.getIdDepartamento())
                        .orElseThrow(() -> new IllegalStateException(
                                "Departamento del usuario autenticado no encontrado."));
                Integer idEmpresaActor = departamentoActor.getEmpresa().getIdEmpresa();
                Integer idEmpresaObjetivo = personal.getDepartamento().getEmpresa().getIdEmpresa();

                if (!idEmpresaObjetivo.equals(idEmpresaActor)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Solo puedes mover a SIN_EMPRESA personal de tu propia unidad.");
                    return "redirect:/personal/listar";
                }
            }

            // Buscar el departamento SIN_DPTO
            Departamento sinDepartamento = departamentoService.buscarPorDescripcionCorta("SIN_DPTO")
                    .orElseThrow(() -> new RuntimeException(
                            "No existe el departamento SIN_DPTO en el sistema."));

            String nombre = personal.getFullName();
            personalService.moverASinEmpresa(id, sinDepartamento.getIdDepartamento());

            redirectAttributes.addFlashAttribute("success",
                    nombre + " movido a SIN_EMPRESA correctamente. "
                            + "Ahora puede ser incorporado por otra unidad.");
            return "redirect:/personal/listar";

        } catch (IllegalStateException e) {
            log.warn("Bloqueo por registro activo al mover a SIN_EMPRESA: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/personal/listar";
        } catch (Exception e) {
            log.error("Error al mover personal a SIN_EMPRESA: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error al mover personal: " + e.getMessage());
            return "redirect:/personal/listar";
        }
    }
}