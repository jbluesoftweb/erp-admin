package lat.jbluesoft.erpadmin.rrhh.service;

import lat.jbluesoft.erpadmin.rrhh.model.Nivel;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;

import java.util.List;
import java.util.Optional;

/**
 * NivelService
 * Lógica de negocio para gestión de niveles
 */
public interface NivelService {

    /**
     * Listar todos los niveles ordenados por orden ASC
     */
    List<Nivel> listarTodos();

    /**
     * Listar niveles por tipo de personal
     */
    List<Nivel> listarPorTipoPersonal(Integer idTipoPersonal);

    /**
     * Buscar nivel por ID
     */
    Optional<Nivel> buscarPorId(Integer id);

    /**
     * Listar todos los tipos de persona
     * Usado para el selector dinámico en editar/registrar
     */
    List<TipoPersonal> listarTipoPersonales();
}