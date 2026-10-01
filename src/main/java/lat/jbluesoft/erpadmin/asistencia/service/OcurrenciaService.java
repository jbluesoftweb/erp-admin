package lat.jbluesoft.erpadmin.asistencia.service;

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
}
