package lat.jbluesoft.erpadmin.rrhh.service;

import lat.jbluesoft.erpadmin.rrhh.model.Personal;

import java.util.List;
import java.util.Optional;

/**
 * PersonalService
 * Lógica de negocio para gestión de personal
 */
public interface PersonalService {

    /**
     * Listar todo el personal habilitado
     */
    List<Personal> listarTodos();

    /**
     * Listar personal de un departamento específico
     */
    List<Personal> listarPorDepartamento(Integer idDepartamento);

    /**
     * Buscar personal por código
     */
    Optional<Personal> buscarPorCodigo(String codigo);

    /**
     * Buscar personal por ID
     */
    Optional<Personal> buscarPorId(Integer id);

    /**
     * Obtener personal por ID (alias de buscarPorId para compatibilidad)
     */
    default Optional<Personal> obtenerPorId(Integer id) {
        return buscarPorId(id);
    }

    /**
     * Contar personal de un departamento
     */
    long contarPorDepartamento(Integer idDepartamento);

    /**
     * Buscar personal por nombre (búsqueda parcial)
     */
    List<Personal> buscarPorNombre(String nombre);

    /**
     * Listar personal con relaciones cargadas (EAGER) para vistas
     */
    List<Personal> listarPorDepartamentoConRelaciones(Integer idDepartamento);
    
    /**
     * Listar todo el personal de la empresa ordenado por antigüedad global
     */
    List<Personal> listarPorEmpresaOrdenadoAntiguedad(Integer idEmpresa);

// =============================================
    // MÉTODOS CRUD - PERSONAL
    // =============================================

    /**
     * Guardar nuevo personal (SYS_ADMIN)
     */
    Personal guardar(Personal personal);

    /**
     * Actualizar personal existente
     */
    Personal actualizar(Personal personal);

    /**
     * Eliminar personal (soft delete: enabled = false)
     */
    void eliminar(Integer id);

    /**
     * Buscar personal por código o apellidos/nombres (para registrar-persona)
     */
    List<Personal> buscarPorCodigoONombre(String query);

    /**
     * Buscar personal por código o apellidos/nombres, acotado a una empresa
     * Usado por ADMIN_APP y ADMIN_SYS al buscar dentro de "Personal de la Empresa"
     */
    List<Personal> buscarPorCodigoONombreEnEmpresa(String query, Integer idEmpresa);

    /**
     * Buscar personal en SIN_EMPRESA por código o nombre
     * Usado por ADMIN_APP y ADMIN_SYS para incorporar personal a su unidad
     */
    List<Personal> buscarEnSinEmpresa(String query);

    /**
     * Mover personal a SIN_EMPRESA (salida de unidad)
     * Solo ADMIN_SYS
     */
    Personal moverASinEmpresa(Integer idPersonal, Integer idDepartamentoSinEmpresa);

    Optional<Personal> buscarPorDni(String dni);
}
