package lat.jbluesoft.erpadmin.rrhh.repository;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Personal
 * Acceso a datos de personal
 */
@Repository
public interface PersonalRepository extends JpaRepository<Personal, Integer> {

    List<Personal> findByDepartamento_IdDepartamentoAndEnabledTrue(Integer idDepartamento);

    Optional<Personal> findByCodigo(String codigo);

    Optional<Personal> findByDni(String dni);

    List<Personal> findByDepartamentoAndEnabledTrueOrderByNivel_OrdenAscAntiguedadDesc(Departamento departamento);

    List<Personal> findByEnabledTrue();

    long countByDepartamento_IdDepartamentoAndEnabledTrue(Integer idDepartamento);


    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento c " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE c.empresa.idEmpresa = :idEmpresa " +
            "AND p.enabled = true " +
            "ORDER BY p.antiguedad ASC")
    List<Personal> findByEmpresaOrdenadoAntiguedad(@Param("idEmpresa") Integer idEmpresa);

    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.departamento.idDepartamento = :idDepartamento " +
            "AND p.enabled = true " +
            "ORDER BY p.antiguedad ASC")
    List<Personal> findByDepartamentoOrdenado(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.enabled = true " +
            "AND (LOWER(p.apPat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.apMat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.antiguedad ASC")
    List<Personal> searchByNombre(@Param("search") String search);

    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.codigo = :codigo " +
            "AND p.enabled = true")
    Optional<Personal> findByCodigoConRelaciones(@Param("codigo") String codigo);

    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento c " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.codigo = :codigo " +
            "AND p.enabled = true " +
            "AND c.empresa.idEmpresa = :idEmpresa")
    Optional<Personal> findByCodigoConRelacionesEnEmpresa(@Param("codigo") String codigo,
                                                        @Param("idEmpresa") Integer idEmpresa);

    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento c " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.enabled = true " +
            "AND c.empresa.idEmpresa = :idEmpresa " +
            "AND (LOWER(p.apPat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.apMat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.antiguedad ASC")
    List<Personal> searchByNombreEnEmpresa(@Param("search") String search,
                                            @Param("idEmpresa") Integer idEmpresa);

    /**
     * Buscar por código exacto en SIN_EMPRESA (para flujo agregar — ADMIN_APP y ADMIN_SYS)
     */
    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento c " +
            "JOIN FETCH c.empresa " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE p.codigo = :codigo " +
            "AND c.empresa.descripcionCorta = 'SIN_EMPRESA' " +
            "AND p.enabled = true")
    Optional<Personal> findByCodigoEnSinEmpresa(@Param("codigo") String codigo);

    /**
     * Buscar por nombre en SIN_EMPRESA (para flujo agregar — ADMIN_APP y ADMIN_SYS)
     */
    @Query("SELECT p FROM Personal p " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento c " +
            "JOIN FETCH c.empresa " +
            "JOIN FETCH g.tipoPersonal " +
            "WHERE c.empresa.descripcionCorta = 'SIN_EMPRESA' " +
            "AND p.enabled = true " +
            "AND (LOWER(p.apPat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.apMat)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.antiguedad ASC")
    List<Personal> searchByNombreEnSinEmpresa(@Param("search") String search);

}
