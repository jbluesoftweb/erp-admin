package lat.jbluesoft.erpadmin.asistencia.service;

import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaCrearDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaEditarDTO;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;

import java.util.List;
import java.util.Optional;

/**
 * OcurrenciaService
 * Lógica de negocio para gestión de ocurrencias
 */
public interface OcurrenciaService {

    /**
     * Listar todas las ocurrencias habilitadas, ordenadas
     */
    List<Ocurrencia> listarTodas();

    /**
     * Buscar ocurrencia por código (ASI, PER, SS, etc.)
     */
    Optional<Ocurrencia> buscarPorCodigo(String codigo);

    /**
     * Buscar ocurrencia por ID
     */
    Optional<Ocurrencia> obtenerPorId(Integer id);

    /**
     * Buscar ocurrencias por tipo (DISPONIBLE, DESCUENTO)
     */
    List<Ocurrencia> listarPorTipo(String tipo);

    /**
     * Obtener la ocurrencia por defecto (Asistió)
     */
    Optional<Ocurrencia> obtenerOcurrenciaPorDefecto();

    // ========== ADMINISTRACIÓN (ADMIN_SYS) ==========

    /**
     * Listar todas las ocurrencias (habilitadas o no), por orden y luego por id
     */
    List<Ocurrencia> listarParaAdministracion();

    /**
     * Crear una ocurrencia nueva (queda habilitada).
     * Lanza IllegalArgumentException si algún dato no es válido o el código ya existe.
     */
    Ocurrencia crear(OcurrenciaCrearDTO dto);

    /**
     * Actualizar descripción, color y orden de una ocurrencia.
     * Código, tipo, almuerzo y enabled nunca se modifican, aunque lleguen en el DTO.
     * Lanza IllegalArgumentException si algún dato no es válido o la ocurrencia no existe.
     */
    Ocurrencia actualizar(Integer id, OcurrenciaEditarDTO dto);
}
