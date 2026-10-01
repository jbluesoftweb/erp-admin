package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.AsistenciaRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.ResumenRegistroDTO;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.asistencia.repository.RegistroDiarioRepository;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.asistencia.service.RegistroDiarioService;
import lat.jbluesoft.erpadmin.rrhh.service.PersonalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistroDiarioServiceImpl implements RegistroDiarioService {

    private final RegistroDiarioRepository registroDiarioRepository;

    private final PersonalService personalService;

    private final DepartamentoService departamentoService;

    // ========== MÉTODOS BÁSICOS EXISTENTES ==========

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> listarTodos() {
        log.debug("Listando todos los registros diarios");
        List<RegistroDiario> registros = registroDiarioRepository.findAll();
        cargarRelaciones(registros);
        return registros;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistroDiario> obtenerPorId(Integer id) {
        log.debug("Obteniendo registro de asistencia por ID: {}", id);
        Optional<RegistroDiario> registroOpt = registroDiarioRepository.findById(id);
        registroOpt.ifPresent(this::cargarRelaciones);
        return registroOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> listarPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha) {
        log.debug("Listando registros por departamento: {} y fecha: {}", idDepartamento, fecha);
        List<RegistroDiario> registros = registroDiarioRepository.findByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha);
        cargarRelaciones(registros);
        return registros;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> listarPorFecha(LocalDate fecha) {
        log.debug("Listando registros por fecha: {}", fecha);
        List<RegistroDiario> registros = registroDiarioRepository.findByFechaRegistro(fecha);
        cargarRelaciones(registros);
        return registros;
    }

    @Override
    @Transactional
    public RegistroDiario guardar(RegistroDiario registroDiario) {
        log.debug("Guardando registro de asistencia: {}", registroDiario);
        return registroDiarioRepository.save(registroDiario);
    }

    @Override
    @Transactional
    public List<RegistroDiario> guardarRegistrosEnBloque(List<RegistroDiario> registros) {
        log.info("Guardando {} registros en bloque (transacción única)", registros.size());

        try {
            List<RegistroDiario> registrosGuardados = registroDiarioRepository.saveAll(registros);
            log.info("Registros guardados exitosamente: {}", registrosGuardados.size());
            return registrosGuardados;

        } catch (Exception e) {
            log.error("Error al guardar registros en bloque. Ejecutando rollback...", e);
            throw new RuntimeException("Error al guardar registros en bloque: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        log.debug("Eliminando registro de asistencia con ID: {}", id);
        registroDiarioRepository.deleteById(id);
    }



    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> listarPorPersonalYFecha(Integer idPersonal, LocalDate fecha) {
        log.debug("Listando registros por personal: {} y fecha: {}", idPersonal, fecha);
        List<RegistroDiario> registros = registroDiarioRepository.findByPersonal_IdPersonalAndFechaRegistro(idPersonal, fecha);
        cargarRelaciones(registros);
        return registros;
    }

    // ========== NUEVOS MÉTODOS ==========

    @Override
    @Transactional(readOnly = true)
    public AsistenciaRegistroDTO prepararRegistroDiario(Integer idDepartamento, LocalDate fechaRegistro) {
        log.info("Preparando registro de asistencia para departamento: {} y fecha: {}", idDepartamento, fechaRegistro);

        AsistenciaRegistroDTO dto = new AsistenciaRegistroDTO();
        dto.setFechaRegistro(fechaRegistro);

        // 1. Obtener TODO el efectivo actual del departamento
        List<Personal> efectivoActual = personalService.listarPorDepartamentoConRelaciones(idDepartamento);
        dto.setTotalEfectivo(efectivoActual.size());

        log.debug("Efectivo actual total: {}", efectivoActual.size());

        // 2. Obtener el último registro
        LocalDate fechaUltimoRegistro = obtenerFechaUltimoRegistro(idDepartamento);
        dto.setExisteRegistroAnterior(fechaUltimoRegistro != null);
        dto.setFechaUltimoRegistro(fechaUltimoRegistro);

        if (fechaUltimoRegistro == null) {
            // NO HAY REGISTRO ANTERIOR - Primera vez
            log.info("No existe registro anterior. Mostrando todo el efectivo.");
            separarPorTipoPersonal(efectivoActual, dto);
            dto.setOcurrencias(new ArrayList<>());

        } else {
            // SÍ HAY REGISTRO ANTERIOR - Comparar
            log.info("Existe registro anterior del: {}. Comparando efectivo vs último registro.", fechaUltimoRegistro);
            compararYSepararPersonal(efectivoActual, idDepartamento, dto);
        }

        log.info("Preparación completada - Grupo 1: {}, Grupo 2: {}, Ocurrencias: {}",
                dto.getGrupo1().size(),
                dto.getGrupo2().size(),
                dto.getOcurrencias().size());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> obtenerUltimoRegistro(Integer idDepartamento) {
        log.debug("Obteniendo último registro de departamento: {}", idDepartamento);
        List<RegistroDiario> registros = registroDiarioRepository.findUltimoRegistroPorDepartamento(idDepartamento);
        cargarRelaciones(registros);
        return registros;
    }



    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> obtenerOcurrenciasUltimoRegistro(Integer idDepartamento) {
        log.debug("Obteniendo ocurrencias del último registro de departamento: {}", idDepartamento);
        List<RegistroDiario> ocurrencias = registroDiarioRepository.findOcurrenciasUltimoRegistro(idDepartamento);
        cargarRelaciones(ocurrencias);
        return ocurrencias;
    }

    @Override
    @Transactional
    public List<RegistroDiario> actualizarRegistro(Integer idDepartamento, LocalDate fechaRegistro, List<RegistroDiario> datosActualizados) {
        log.info("Actualizando registro de departamento: {} fecha: {}", idDepartamento, fechaRegistro);

        try {
            // 1. Obtener los registros EXISTENTES de la BD
            List<RegistroDiario> registrosExistentes = registroDiarioRepository
                    .findByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fechaRegistro);

            if (registrosExistentes.isEmpty()) {
                throw new RuntimeException("No existe registro para actualizar en la fecha: " + fechaRegistro);
            }

            log.info("Registros existentes encontrados: {}", registrosExistentes.size());

            // 2. Crear un mapa código -> RegistroDiario existente para búsqueda rápida
            Map<String, RegistroDiario> mapaExistentes = registrosExistentes.stream()
                    .collect(Collectors.toMap(RegistroDiario::getCodigo, p -> p));

            // 3. Actualizar cada registro existente con los nuevos datos
            for (RegistroDiario datosNuevos : datosActualizados) {
                RegistroDiario existente = mapaExistentes.get(datosNuevos.getCodigo());

                if (existente != null) {
                    // ACTUALIZAR campos modificables (mantiene id_registro, created_at, created_by)
                    existente.setOcurrencia(datosNuevos.getOcurrencia());
                    existente.setFechaInicio(datosNuevos.getFechaInicio());
                    existente.setFechaTermino(datosNuevos.getFechaTermino());
                    existente.setDetalle(datosNuevos.getDetalle());

                    log.debug("Actualizado registro para código: {} - Ocurrencia: {}",
                            existente.getCodigo(),
                            existente.getOcurrencia().getDescripcion());
                } else {
                    log.warn("No se encontró registro existente para código: {}", datosNuevos.getCodigo());
                }
            }

            // 4. Guardar (JPA hace UPDATE automáticamente porque son entidades managed)
            List<RegistroDiario> registrosActualizados = registroDiarioRepository.saveAll(registrosExistentes);

            log.info("Registro actualizado exitosamente: {} registros modificados", registrosActualizados.size());
            return registrosActualizados;

        } catch (Exception e) {
            log.error("Error al actualizar registro: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar registro: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void eliminarRegistro(Integer idDepartamento, LocalDate fechaRegistro) {
        log.info("Eliminando registro de departamento: {} fecha: {}", idDepartamento, fechaRegistro);
        registroDiarioRepository.deleteByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fechaRegistro);
    }

    // ========== NUEVOS MÉTODOS PARA IMPRIMIR REGISTRO ==========

    @Override
    @Transactional(readOnly = true)
    public ResumenRegistroDTO obtenerResumenRegistro(Integer idDepartamento, LocalDate fechaRegistro) {
        log.info("Generando resumen del registro para departamento: {} fecha: {}", idDepartamento, fechaRegistro);

        ResumenRegistroDTO resumen = new ResumenRegistroDTO();
        resumen.setFechaRegistro(fechaRegistro);

        // Obtener nombre del departamento
        Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
        resumen.setNombreDepartamento(departamento.getDescripcionLarga());

        // Contar efectivos por tipo de personal
        Long efectivosOO = registroDiarioRepository.contarEfectivosPorTipoPersonal(idDepartamento, fechaRegistro, 1);
        Long efectivosGrupo2 = registroDiarioRepository.contarEfectivosPorTipoPersonal(idDepartamento, fechaRegistro, 2);

        resumen.setEfectivosGrupo1(efectivosOO.intValue());
        resumen.setEfectivosGrupo2(efectivosGrupo2.intValue());

        // Contar descuentos por tipo de personal
        Long descuentosOO = registroDiarioRepository.contarDescuentosPorTipoPersonal(idDepartamento, fechaRegistro, 1);
        Long descuentosGrupo2 = registroDiarioRepository.contarDescuentosPorTipoPersonal(idDepartamento, fechaRegistro, 2);

        resumen.setDescuentosGrupo1(descuentosOO.intValue());
        resumen.setDescuentosGrupo2(descuentosGrupo2.intValue());

        // Calcular disponibles primero (efectivos - descuentos)
        resumen.calcularDisponibles();

        // Luego calcular totales
        resumen.calcularTotales();

        log.info("Resumen generado - Efectivos: {}, Descuentos: {}, Disponibles: {}",
                resumen.getEfectivosTotal(),
                resumen.getDescuentosTotal(),
                resumen.getDisponiblesTotal());

        return resumen;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<RegistroDiario>> obtenerOcurrenciasAgrupadasPorTipo(
            Integer idDepartamento, LocalDate fechaRegistro, Integer tipoPersonal) {

        log.info("Obteniendo ocurrencias agrupadas para departamento: {} fecha: {} tipo: {}",
                idDepartamento, fechaRegistro, tipoPersonal);

        // Obtener todas las ocurrencias del tipo de personal
        List<RegistroDiario> ocurrencias = registroDiarioRepository.findOcurrenciasPorTipoPersonal(
                idDepartamento, fechaRegistro, tipoPersonal);

        // Cargar relaciones LAZY
        cargarRelaciones(ocurrencias);

        // Agrupar por descripción de ocurrencia
        Map<String, List<RegistroDiario>> ocurrenciasAgrupadas = ocurrencias.stream()
                .collect(Collectors.groupingBy(
                        pd -> pd.getOcurrencia().getDescripcion(),
                        LinkedHashMap::new,  // Mantener orden de inserción
                        Collectors.toList()
                ));

        log.info("Ocurrencias agrupadas: {} grupos encontrados", ocurrenciasAgrupadas.size());

        return ocurrenciasAgrupadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> obtenerPersonalPresenteRegistro(Integer idDepartamento, LocalDate fechaRegistro) {
        log.info("Obteniendo personal presente para departamento: {} fecha: {}", idDepartamento, fechaRegistro);

        List<RegistroDiario> presentes = registroDiarioRepository.findPersonalPresente(idDepartamento, fechaRegistro);

        // Cargar relaciones LAZY
        cargarRelaciones(presentes);

        log.info("Personal presente: {} registros", presentes.size());

        return presentes;
    }

    // ========== MÉTODOS PRIVADOS DE APOYO ==========

    /**
     * Separa el personal por tipo (Grupo 1 vs Grupo 2)
     * Sin comparación con registro anterior
     */
    private void separarPorTipoPersonal(List<Personal> personal, AsistenciaRegistroDTO dto) {
        List<Personal> grupo1 = new ArrayList<>();
        List<Personal> grupo2 = new ArrayList<>();

        for (Personal p : personal) {
            if (p.getNivel() != null && p.getNivel().getTipoPersonal() != null) {
                Integer tipoPersonal = p.getNivel().getTipoPersonal().getIdTipoPersonal();

                if (tipoPersonal == 1) {
                    grupo1.add(p);
                } else if (tipoPersonal == 2) {
                    grupo2.add(p);
                }
            }
        }

        dto.setGrupo1(grupo1);
        dto.setGrupo2(grupo2);
    }

    /**
     * Compara el efectivo actual vs el último registro
     * Separa en: Grupo 1, Grupo 2 y Ocurrencias
     */
    private void compararYSepararPersonal(List<Personal> efectivoActual, Integer idDepartamento, AsistenciaRegistroDTO dto) {

        // 1. Obtener último registro completo
        List<RegistroDiario> ultimoRegistro = obtenerUltimoRegistro(idDepartamento);

        // 2. Crear un mapa código -> RegistroDiario del último registro
        Map<String, RegistroDiario> mapaUltimoRegistro = ultimoRegistro.stream()
                .collect(Collectors.toMap(RegistroDiario::getCodigo, pd -> pd));

        // 3. Listas para separar
        List<Personal> grupo1 = new ArrayList<>();
        List<Personal> grupo2 = new ArrayList<>();
        List<RegistroDiario> ocurrencias = new ArrayList<>();

        // 4. Iterar sobre el efectivo actual
        for (Personal persona : efectivoActual) {
            RegistroDiario registroAnterior = mapaUltimoRegistro.get(persona.getCodigo());

            if (registroAnterior == null) {
                // PERSONA NUEVA - No estaba en el último registro
                agregarAPorTipo(persona, grupo1, grupo2);

            } else {
                // PERSONA EXISTÍA EN EL ÚLTIMO REGISTRO
                Integer idOcurrencia = registroAnterior.getOcurrencia().getIdOcurrencia();

                if (idOcurrencia == 1) {
                    // Tenía ASISTIÓ (PRESENTE) -> Va a Grupo 1 o Grupo 2
                    agregarAPorTipo(persona, grupo1, grupo2);

                } else {
                    // Tenía OCURRENCIA -> Va a tabla Ocurrencias (pre-llenado)
                    ocurrencias.add(registroAnterior);
                }
            }
        }

        dto.setGrupo1(grupo1);
        dto.setGrupo2(grupo2);
        dto.setOcurrencias(ocurrencias);
    }

    /**
     * Agrega una persona a la lista correspondiente según su tipo
     */
    private void agregarAPorTipo(Personal persona, List<Personal> grupo1, List<Personal> grupo2) {
        if (persona.getNivel() != null && persona.getNivel().getTipoPersonal() != null) {
            Integer tipoPersonal = persona.getNivel().getTipoPersonal().getIdTipoPersonal();

            if (tipoPersonal == 1) {
                grupo1.add(persona);
            } else if (tipoPersonal == 2) {
                grupo2.add(persona);
            }
        }
    }

    /**
     * Fuerza la carga de todas las relaciones LAZY de una lista de registros
     * Nota: Si se usan las consultas optimizadas con FETCH del repositorio, este método
     * ya no realizará consultas adicionales a la base de datos.
     */
    private void cargarRelaciones(List<RegistroDiario> registros) {
        // Con las optimizaciones del repositorio (JOIN FETCH), las relaciones ya vienen cargadas.
        // Mantenemos este método para compatibilidad con código existente, pero es menos crítico.
        registros.forEach(this::cargarRelaciones);
    }

    /**
     * Fuerza la carga de todas las relaciones LAZY de un registro
     */
    private void cargarRelaciones(RegistroDiario registro) {
        if (registro.getOcurrencia() != null) {
            registro.getOcurrencia().getDescripcion();
        }
        if (registro.getPersonal() != null) {
            registro.getPersonal().getCodigo();
        }
        if (registro.getNivel() != null) {
            registro.getNivel().getDescripcionCorta();
        }
        if (registro.getEspecialidad() != null) {
            registro.getEspecialidad().getDescripcionCorta();
        }
        if (registro.getDepartamento() != null) {
            registro.getDepartamento().getDescripcionCorta();
        }
    }




    // ========== MÉTODO PARA CONSOLIDADO (ADMIN_APP) ==========

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoRegistroDTO obtenerConsolidadoPorFecha(LocalDate fechaRegistro, Integer idEmpresa) {
        log.info("Generando consolidado del registro para fecha: {} y empresa ID: {}", fechaRegistro, idEmpresa);

        ConsolidadoRegistroDTO consolidado = new ConsolidadoRegistroDTO();
        consolidado.setFechaRegistro(fechaRegistro);

        try {
            // ========== OBTENER TODOS LOS DEPARTAMENTOS DE LA EMPRESA ==========
            List<Departamento> departamentosDeLaEmpresa = departamentoService.listarPorEmpresa(idEmpresa);

            if (!departamentosDeLaEmpresa.isEmpty() && departamentosDeLaEmpresa.get(0).getEmpresa() != null) {
                consolidado.setEmpresaDescripcion(departamentosDeLaEmpresa.get(0).getEmpresa().getDescripcionCorta());
            } else {
                consolidado.setEmpresaDescripcion("UNIDAD NO ESPECIFICADA");
            }

            log.info("Departamentos de la empresa {}: {}", idEmpresa,
                    departamentosDeLaEmpresa.stream()
                            .map(Departamento::getDescripcionCorta)
                            .collect(Collectors.joining(", ")));

            // ========== GRUPO 1 ==========

            // 1. Obtener efectivos por departamento (Grupo 1) - TODOS los registros
            List<ConsolidadoDepartamentoDTO> efectivosOO =
                    registroDiarioRepository.contarEfectivosPorDepartamento(fechaRegistro, 1);

            // 2. Obtener descuentos por departamento (Grupo 1) - Solo los que NO asistieron
            List<ConsolidadoDepartamentoDTO> descuentosOO =
                    registroDiarioRepository.contarDescuentosPorDepartamento(fechaRegistro, 1);

            // 3. Crear mapa de TODOS los departamentos de la empresa (inicializar en 0)
            Map<Integer, ConsolidadoDepartamentoDTO> mapaOO = new LinkedHashMap<>();

            for (Departamento departamento : departamentosDeLaEmpresa) {
                ConsolidadoDepartamentoDTO dto = new ConsolidadoDepartamentoDTO();
                dto.setIdDepartamento(departamento.getIdDepartamento());
                dto.setDepartamentoDescripcion(departamento.getDescripcionCorta());
                dto.setEfectivos(0L);
                dto.setDescuentos(0L);
                dto.setDisponibles(0L);

                mapaOO.put(departamento.getIdDepartamento(), dto);
            }

            // 4. Actualizar con efectivos reales
            for (ConsolidadoDepartamentoDTO dto : efectivosOO) {
                ConsolidadoDepartamentoDTO existente = mapaOO.get(dto.getIdDepartamento());
                if (existente != null) {
                    existente.setEfectivos(dto.getEfectivos());
                }
            }

            // 5. Actualizar con descuentos reales
            for (ConsolidadoDepartamentoDTO dto : descuentosOO) {
                ConsolidadoDepartamentoDTO existente = mapaOO.get(dto.getIdDepartamento());
                if (existente != null) {
                    existente.setDescuentos(dto.getEfectivos()); // El count viene en "efectivos"
                }
            }

            // 6. Calcular disponibles para cada departamento
            for (ConsolidadoDepartamentoDTO dto : mapaOO.values()) {
                dto.calcularDisponibles();
            }

            consolidado.setConsolidadoGrupo1(new ArrayList<>(mapaOO.values()));

            // 7. Obtener ocurrencias agrupadas (Grupo 1)
            List<RegistroDiario> ocurrenciasOO =
                    registroDiarioRepository.findOcurrenciasConsolidadoPorTipoPersonal(fechaRegistro, 1);

            Map<String, List<RegistroDiario>> ocurrenciasOOAgrupadas = ocurrenciasOO.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getOcurrencia().getDescripcion(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            consolidado.setOcurrenciasGrupo1Agrupadas(ocurrenciasOOAgrupadas);

            // ========== GRUPO 2 ==========

            // 8. Obtener efectivos por departamento (Grupo 2) - TODOS los registros
            List<ConsolidadoDepartamentoDTO> efectivosGrupo2 =
                    registroDiarioRepository.contarEfectivosPorDepartamento(fechaRegistro, 2);

            // 9. Obtener descuentos por departamento (Grupo 2)
            List<ConsolidadoDepartamentoDTO> descuentosGrupo2 =
                    registroDiarioRepository.contarDescuentosPorDepartamento(fechaRegistro, 2);

            // 10. Crear mapa de TODOS los departamentos de la empresa (inicializar en 0)
            Map<Integer, ConsolidadoDepartamentoDTO> mapaGrupo2 = new LinkedHashMap<>();

            for (Departamento departamento : departamentosDeLaEmpresa) {
                ConsolidadoDepartamentoDTO dto = new ConsolidadoDepartamentoDTO();
                dto.setIdDepartamento(departamento.getIdDepartamento());
                dto.setDepartamentoDescripcion(departamento.getDescripcionCorta());
                dto.setEfectivos(0L);
                dto.setDescuentos(0L);
                dto.setDisponibles(0L);

                mapaGrupo2.put(departamento.getIdDepartamento(), dto);
            }

            // 11. Actualizar con efectivos reales
            for (ConsolidadoDepartamentoDTO dto : efectivosGrupo2) {
                ConsolidadoDepartamentoDTO existente = mapaGrupo2.get(dto.getIdDepartamento());
                if (existente != null) {
                    existente.setEfectivos(dto.getEfectivos());
                }
            }

            // 12. Actualizar con descuentos reales
            for (ConsolidadoDepartamentoDTO dto : descuentosGrupo2) {
                ConsolidadoDepartamentoDTO existente = mapaGrupo2.get(dto.getIdDepartamento());
                if (existente != null) {
                    existente.setDescuentos(dto.getEfectivos());
                }
            }

            // 13. Calcular disponibles para cada departamento
            for (ConsolidadoDepartamentoDTO dto : mapaGrupo2.values()) {
                dto.calcularDisponibles();
            }

            consolidado.setConsolidadoGrupo2(new ArrayList<>(mapaGrupo2.values()));

            // 14. Obtener ocurrencias agrupadas (Grupo 2)
            List<RegistroDiario> ocurrenciasGrupo2 =
                    registroDiarioRepository.findOcurrenciasConsolidadoPorTipoPersonal(fechaRegistro, 2);

            Map<String, List<RegistroDiario>> ocurrenciasGrupo2Agrupadas = ocurrenciasGrupo2.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getOcurrencia().getDescripcion(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            consolidado.setOcurrenciasGrupo2Agrupadas(ocurrenciasGrupo2Agrupadas);

            // ========== OCURRENCIAS (Grupo 1 + Grupo 2 unificado) ==========
            List<RegistroDiario> ocurrenciasTodas =
                    registroDiarioRepository.findOcurrenciasConsolidado(fechaRegistro);

            Map<String, List<RegistroDiario>> ocurrenciasAgrupadas = ocurrenciasTodas.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getOcurrencia().getDescripcion(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            consolidado.setOcurrenciasAgrupadas(ocurrenciasAgrupadas);

            // ========== PERSONAL PRESENTE ==========
            List<RegistroDiario> presentes = registroDiarioRepository.findPersonalPresenteConsolidado(fechaRegistro);
            consolidado.setPersonalPresente(presentes);

            // ========== CUADRO TOTAL POR DEPARTAMENTO (Grupo 1 + Grupo 2) ==========
            List<ConsolidadoDepartamentoDTO> efectivosTotal =
                    registroDiarioRepository.contarEfectivosPorDepartamentoTotal(fechaRegistro);
            List<ConsolidadoDepartamentoDTO> descuentosTotal =
                    registroDiarioRepository.contarDescuentosPorDepartamentoTotal(fechaRegistro);

            Map<Integer, ConsolidadoDepartamentoDTO> mapaTotal = new LinkedHashMap<>();
            for (Departamento departamento : departamentosDeLaEmpresa) {
                ConsolidadoDepartamentoDTO dto = new ConsolidadoDepartamentoDTO();
                dto.setIdDepartamento(departamento.getIdDepartamento());
                dto.setDepartamentoDescripcion(departamento.getDescripcionCorta());
                dto.setEfectivos(0L);
                dto.setDescuentos(0L);
                dto.setDisponibles(0L);

                mapaTotal.put(departamento.getIdDepartamento(), dto);
            }
            for (ConsolidadoDepartamentoDTO dto : efectivosTotal) {
                ConsolidadoDepartamentoDTO existente = mapaTotal.get(dto.getIdDepartamento());
                if (existente != null) existente.setEfectivos(dto.getEfectivos());
            }
            for (ConsolidadoDepartamentoDTO dto : descuentosTotal) {
                ConsolidadoDepartamentoDTO existente = mapaTotal.get(dto.getIdDepartamento());
                if (existente != null) existente.setDescuentos(dto.getEfectivos());
            }
            for (ConsolidadoDepartamentoDTO dto : mapaTotal.values()) {
                dto.calcularDisponibles();
            }
            consolidado.setConsolidadoTotal(new ArrayList<>(mapaTotal.values()));

            // ========== CALCULAR TOTALES ==========
            consolidado.calcularTotales();

            log.info("Consolidado generado - Grupo 1: {}, Grupo 2: {}, Total: {}",
                    consolidado.getTotalEfectivosGrupo1(),
                    consolidado.getTotalEfectivosGrupo2(),
                    consolidado.getTotalEfectivosGeneral());

            return consolidado;

        } catch (Exception e) {
            log.error("Error al generar consolidado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar consolidado del registro: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeRegistroHoyParaPersonal(Integer idPersonal) {
        log.debug("Verificando si existe registro hoy para personal ID: {}", idPersonal);
        LocalDate hoy = LocalDate.now();
        return registroDiarioRepository
                .existsByPersonal_IdPersonalAndFechaRegistro(idPersonal, hoy);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate obtenerFechaUltimoRegistro(Integer idDepartamento) {
        log.debug("Obteniendo fecha del último registro para departamento ID: {}", idDepartamento);
        return registroDiarioRepository
                .findFechaUltimoRegistroPorDepartamento(idDepartamento)
                .orElse(null);
    }

    @Override
    public LocalDate obtenerDiaHabilAnterior(LocalDate fecha) {
        return lat.jbluesoft.erpadmin.asistencia.util.DiaHabilUtil.obtenerDiaHabilAnterior(fecha);
    }

    @Override
    public LocalDate obtenerDiaHabilSiguiente(LocalDate fecha) {
        return lat.jbluesoft.erpadmin.asistencia.util.DiaHabilUtil.obtenerDiaHabilSiguiente(fecha);
    }

    @Override
    public boolean puedeRegistrarAsistencia(Integer idDepartamento, LocalDate fechaRegistro) {
        LocalDate fechaUltimoRegistro = obtenerFechaUltimoRegistro(idDepartamento);

        if (fechaUltimoRegistro == null) {
            // Nunca se registró ningún registro para este departamento: primer uso, se permite.
            return true;
        }

        LocalDate diaHabilAnterior = obtenerDiaHabilAnterior(fechaRegistro);

        // Se permite si el último registro es del día hábil anterior o más reciente.
        return !fechaUltimoRegistro.isBefore(diaHabilAnterior);
    }



    @Override
    @Transactional
    public void eliminarUltimoRegistro(Integer idDepartamento) {
        log.info("Eliminando último registro de departamento ID: {}", idDepartamento);

        // Obtener fecha del último registro
        LocalDate fechaUltimo = registroDiarioRepository
                .findFechaUltimoRegistroPorDepartamento(idDepartamento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe ningún registro para este departamento."));

        // Eliminar los registros de asistencia
        List<RegistroDiario> registros = registroDiarioRepository
                .findByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fechaUltimo);

        registroDiarioRepository.deleteAll(registros);

        log.info("Último registro eliminado — Departamento: {} | Fecha: {} | Registros: {}",
                idDepartamento, fechaUltimo, registros.size());
    }


/*
    @Override
    @Transactional(readOnly = true)
    public boolean existeRegistroPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha) {
        log.debug("Verificando registro Departamento: {} fecha: {}", idDepartamento, fecha);
        return registroDiarioRepository.existsByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha);
    }
*/
    @Override
    @Transactional(readOnly = true)
    public boolean existeRegistroPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha) {
        log.debug("Verificando si existe registro para departamento: {} y fecha: {}", idDepartamento, fecha);
        return registroDiarioRepository.existsByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha);
    }

    @Override
    @Transactional
    public void eliminarRegistroPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha) {
        log.info("Eliminando registro Departamento: {} fecha: {}", idDepartamento, fecha);

        // Verificar que existe
        if (!registroDiarioRepository.existsByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha)) {
            throw new IllegalArgumentException(
                    "No existe registro para ese departamento y fecha.");
        }

        // Eliminar registros de asistencia
        List<RegistroDiario> registros = registroDiarioRepository
                .findByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha);

        registroDiarioRepository.deleteAll(registros);

        log.warn("REGISTRO ELIMINADO — Departamento: {} | Fecha: {} | Registros: {}",
                idDepartamento, fecha, registros.size());

        // Cascada: cualquier registro posterior de este departamento queda basado en
        // datos que ya no existen tras este borrado, así que también se elimina.
        List<RegistroDiario> registrosPosteriores = registroDiarioRepository
                .findByDepartamento_IdDepartamentoAndFechaRegistroAfter(idDepartamento, fecha);

        if (!registrosPosteriores.isEmpty()) {
            LocalDate fechaMasAntigua = registrosPosteriores.stream()
                    .map(RegistroDiario::getFechaRegistro)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            registroDiarioRepository.deleteAll(registrosPosteriores);
            log.warn("REGISTROS POSTERIORES ELIMINADOS EN CASCADA — Departamento: {} | Desde: {} | Registros: {}",
                    idDepartamento, fechaMasAntigua, registrosPosteriores.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroDiario> listarPorDepartamentoYFechaAdmin(Integer idDepartamento, LocalDate fecha) {
        log.debug("Listando registro admin Departamento: {} fecha: {}", idDepartamento, fecha);
        return registroDiarioRepository
                .findByDepartamento_IdDepartamentoAndFechaRegistro(idDepartamento, fecha);
    }




}