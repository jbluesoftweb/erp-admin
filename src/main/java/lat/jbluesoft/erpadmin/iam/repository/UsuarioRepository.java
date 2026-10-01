package lat.jbluesoft.erpadmin.iam.repository;

import lat.jbluesoft.erpadmin.iam.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Usuario
 * Acceso a datos de usuario
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Buscar usuario por código
     */
    Optional<Usuario> findByCodigo(String codigo);

    /**
     * Buscar usuario por código y habilitado
     */
    Optional<Usuario> findByCodigoAndEnabledTrue(String codigo);

    /**
     * Buscar usuario con sus roles y sistemas
     * (Para autenticación)
     * Carga EAGER: Personal > Nivel, Especialidad, Departamento > Empresa
     */
    @Query("SELECT u FROM Usuario u " +
            "LEFT JOIN FETCH u.personal p " +
            "LEFT JOIN FETCH p.nivel " +
            "LEFT JOIN FETCH p.especialidad " +
            "LEFT JOIN FETCH p.departamento c " +
            "LEFT JOIN FETCH c.empresa " +
            "WHERE u.codigo = :codigo AND u.enabled = true")
    Optional<Usuario> findByCodigoWithDetails(@Param("codigo") String codigo);

    /**
     * Listar todos los usuarios con su personal cargado.
     */
    @Query("SELECT u FROM Usuario u " +
            "LEFT JOIN FETCH u.personal p " +
            "LEFT JOIN FETCH p.nivel " +
            "LEFT JOIN FETCH p.especialidad " +
            "LEFT JOIN FETCH p.departamento " +
            "ORDER BY u.enabled DESC, p.antiguedad ASC")
    List<Usuario> findAllConPersonal();

    /**
     * Buscar usuario por ID con personal cargado.
     */
    @Query("SELECT u FROM Usuario u " +
            "LEFT JOIN FETCH u.personal p " +
            "LEFT JOIN FETCH p.nivel " +
            "LEFT JOIN FETCH p.especialidad " +
            "LEFT JOIN FETCH p.departamento " +
            "WHERE u.idUsuario = :id")
    Optional<Usuario> findByIdConPersonal(@Param("id") Integer id);

    /**
     * Verificar si email ya existe.
     */
    boolean existsByEmail(String email);

    /**
     * Verificar si email existe excluyendo un usuario específico.
     * Usado al actualizar para no chocar con el propio email.
     */
    boolean existsByEmailAndIdUsuarioNot(String email, Integer idUsuario);

    @Query("SELECT DISTINCT u FROM Usuario u " +
            "LEFT JOIN FETCH u.personal p " +
            "LEFT JOIN FETCH p.nivel " +
            "LEFT JOIN FETCH p.especialidad " +
            "LEFT JOIN FETCH p.departamento " +
            "WHERE EXISTS (" +
            "  SELECT rs FROM RegistroSistema rs " +
            "  WHERE rs.usuario = u " +
            "  AND rs.sistema.codigo = 'ASISTENCIA'" +
            ") " +
            "ORDER BY u.enabled DESC, p.antiguedad ASC")
    List<Usuario> findAllConPersonalBySistema();

    /**
     * Buscar el usuario asociado a un personal específico, si existe.
     */
    Optional<Usuario> findByPersonal_IdPersonal(Integer idPersonal);

}