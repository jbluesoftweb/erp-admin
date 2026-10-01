package lat.jbluesoft.erpadmin.rrhh.service;

import lat.jbluesoft.erpadmin.rrhh.model.Especialidad;

import java.util.List;
import java.util.Optional;

/**
 * EspecialidadService
 * Lógica de negocio para gestión de especialidades
 */
public interface EspecialidadService {

    /**
     * Listar todas las especialidades
     */
    List<Especialidad> listarTodas();

    /**
     * Listar especialidades por tipo de personal
     */
    List<Especialidad> listarPorTipoPersonal(Integer idTipoPersonal);

    /**
     * Buscar especialidad por ID
     */
    Optional<Especialidad> buscarPorId(Integer id);
}