package lat.jbluesoft.erpadmin.rrhh.repository;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Departamento
 * Acceso a datos de departamento
 */
@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {

    /**
     * Buscar departamentos habilitados
     */
    List<Departamento> findByEnabledTrue();

    /**
     * Buscar por descripción corta
     */
    Optional<Departamento> findByDescripcionCorta(String descripcionCorta);

    /**
     * Buscar departamentos por empresa (id_empresa) y habilitados
     * Excluye 'SIN_DPTO'
     */
    List<Departamento> findByEmpresa_IdEmpresaAndEnabledTrueOrderByIdDepartamentoAsc(Integer idEmpresa);
}