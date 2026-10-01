package lat.jbluesoft.erpadmin.rrhh.service;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;

import java.util.List;
import java.util.Optional;

/**
 * DepartamentoService
 * Lógica de negocio para gestión de departamentos
 */
public interface DepartamentoService {

    /**
     * Listar todos los departamentos habilitados
     */
    List<Departamento> listarTodas();

    /**
     * Buscar departamento por ID
     */
    Optional<Departamento> buscarPorId(Integer id);

    /**
     * Buscar departamento por descripción corta
     */
    Optional<Departamento> buscarPorDescripcionCorta(String descripcionCorta);

    /**
     * Listar departamentos por empresa (id_empresa)
     * Retorna solo los departamentos de la empresa especificada
     */
    List<Departamento> listarPorEmpresa(Integer idEmpresa);
}