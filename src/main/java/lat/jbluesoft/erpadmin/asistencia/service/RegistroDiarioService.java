package lat.jbluesoft.erpadmin.asistencia.service;

import lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.AsistenciaRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.ResumenRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegistroDiarioService {


    List<RegistroDiario> listarTodos();
    Optional<RegistroDiario> obtenerPorId(Integer id);
    List<RegistroDiario> listarPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha);
    List<RegistroDiario> listarPorFecha(LocalDate fecha);
    RegistroDiario guardar(RegistroDiario registroDiario);
    List<RegistroDiario> guardarRegistrosEnBloque(List<RegistroDiario> registros);
    void eliminar(Integer id);
    List<RegistroDiario> listarPorPersonalYFecha(Integer idPersonal, LocalDate fecha);

    // ========== NUEVOS MÉTODOS ==========

    /**
     * Prepara los datos para el formulario de registro de asistencia
     * Compara el efectivo actual vs el último registro
     * Separa en: Grupo 1, Grupo 2 y Ocurrencias
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro a registrar
     * @return DTO con las listas separadas
     */
    AsistenciaRegistroDTO prepararRegistroDiario(Integer idDepartamento, LocalDate fechaRegistro);

    /**
     * Obtiene el último registro de un departamento
     *
     * @param idDepartamento ID del departamento
     * @return Lista de registros del último registro (puede estar vacía si no hay registros)
     */
    List<RegistroDiario> obtenerUltimoRegistro(Integer idDepartamento);

    /**
     * Obtiene la fecha del último registro de un departamento
     *
     * @param idDepartamento ID del departamento
     * @return Fecha del último registro o null si no existe
     */
    LocalDate obtenerFechaUltimoRegistro(Integer idDepartamento);

    /**
     * Determina si un departamento puede registrar la asistencia de una fecha dada,
     * según la regla de "día hábil anterior": si ya existe al menos un registro
     * histórico para el departamento, el último registro debe ser del
     * día hábil inmediatamente anterior a la fecha que se quiere registrar.
     * Si el departamento nunca registró ningún registro, siempre retorna true
     * (primer uso, no aplica la regla).
     */
    boolean puedeRegistrarAsistencia(Integer idDepartamento, LocalDate fechaRegistro);

    /**
     * Calcula el día hábil anterior a una fecha dada (retrocede hasta
     * encontrar un día que no sea sábado ni domingo).
     */
    LocalDate obtenerDiaHabilAnterior(LocalDate fecha);

    /**
     * Calcula el día hábil siguiente a una fecha dada (avanza hasta
     * encontrar un día que no sea sábado ni domingo).
     */
    LocalDate obtenerDiaHabilSiguiente(LocalDate fecha);

    /**
     * Obtiene solo las ocurrencias (id_ocurrencia != 1) del último registro
     *
     * @param idDepartamento ID del departamento
     * @return Lista de registros con ocurrencias del último registro
     */
    List<RegistroDiario> obtenerOcurrenciasUltimoRegistro(Integer idDepartamento);

    /**
     * Actualiza un registro existente
     * NO elimina y vuelve a crear, sino que hace UPDATE de los registros existentes
     * preservando id_registro, created_at y created_by
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro a actualizar
     * @param datosActualizados Datos actualizados del registro (ocurrencia, fechas, detalles)
     * @return Lista de registros actualizados
     */
    List<RegistroDiario> actualizarRegistro(Integer idDepartamento, LocalDate fechaRegistro, List<RegistroDiario> datosActualizados);

    /**
     * Elimina todas las filas de un registro específico
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro a eliminar
     */
    void eliminarRegistro(Integer idDepartamento, LocalDate fechaRegistro);

    // ========== NUEVOS MÉTODOS PARA IMPRIMIR REGISTRO ==========

    /**
     * Obtiene el resumen del registro con efectivos, descuentos y disponibles
     * separados por tipo de personal (Grupo 1 y Grupo 2)
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro
     * @return DTO con el resumen del registro
     */
    ResumenRegistroDTO obtenerResumenRegistro(Integer idDepartamento, LocalDate fechaRegistro);

    /**
     * Obtiene las ocurrencias (id_ocurrencia != 1) por tipo de personal
     * Agrupadas por tipo de ocurrencia para mostrar en secciones
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro
     * @param tipoPersonal 1=Grupo 1, 2=Grupo 2
     * @return Mapa: Descripción de ocurrencia -> Lista de registros con esa ocurrencia
     */
    Map<String, List<RegistroDiario>> obtenerOcurrenciasAgrupadasPorTipo(
            Integer idDepartamento, LocalDate fechaRegistro, Integer tipoPersonal);

    /**
     * Obtiene el personal presente (id_ocurrencia = 1)
     * Mezclados: Grupo 1 + Grupo 2 ordenados por antigüedad
     *
     * @param idDepartamento ID del departamento
     * @param fechaRegistro Fecha del registro
     * @return Lista de registros con personal presente
     */
    List<RegistroDiario> obtenerPersonalPresenteRegistro(Integer idDepartamento, LocalDate fechaRegistro);
    /**
     * Obtiene el consolidado general de asistencia para una fecha
     * Incluye datos agregados de todos los departamentos de la empresa
     *
     * @param fechaRegistro Fecha del registro a consolidar
     * @param idEmpresa ID de la empresa a consolidar
     * @return ConsolidadoRegistroDTO con datos del Grupo 1, Grupo 2 y ocurrencias
     */
    ConsolidadoRegistroDTO obtenerConsolidadoPorFecha(LocalDate fechaRegistro, Integer idEmpresa);

    /**
     * Verifica si existe un registro hoy para un personal específico.
     * Usado para bloquear movimientos de personal cuando hay registro activo.
     */
    boolean existeRegistroHoyParaPersonal(Integer idPersonal);


    /**
     * Elimina el último registro de un departamento junto con su autorización.
     * Solo ADMIN_SYS — solo el registro más reciente.
     */
    void eliminarUltimoRegistro(Integer idDepartamento);

    /**
     * Verifica si existe registro para un departamento en una fecha específica.
     * Usado en la vista de administración para mostrar qué registros existen.
     */
    boolean existeRegistroPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha);

    /**
     * Elimina el registro de un departamento en una fecha específica.
     * Solo ADMIN_SYS — elimina también la autorización asociada.
     */
    void eliminarRegistroPorDepartamentoYFecha(Integer idDepartamento, LocalDate fecha);

    /**
     * Lista los registros de asistencia de un departamento en una fecha específica.
     * Usado para previsualizar antes de eliminar.
     */
    List<RegistroDiario> listarPorDepartamentoYFechaAdmin(Integer idDepartamento, LocalDate fecha);

}