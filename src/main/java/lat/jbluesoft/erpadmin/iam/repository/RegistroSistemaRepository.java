package lat.jbluesoft.erpadmin.iam.repository;

import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Sistema;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository: RegistroSistema
 * Acceso a datos de registro_sistema
 */
@Repository
public interface RegistroSistemaRepository extends JpaRepository<RegistroSistema, Integer> {

    /**
     * Buscar registros por usuario
     */
    List<RegistroSistema> findByUsuario(Usuario usuario);

    /**
     * Buscar registros por usuario y sistema
     */
    List<RegistroSistema> findByUsuarioAndSistema(Usuario usuario, Sistema sistema);

    /**
     * Obtener roles de un usuario en un sistema específico
     */
    @Query("SELECT rs FROM RegistroSistema rs " +
           "JOIN FETCH rs.rol " +
           "WHERE rs.usuario.codigo = :codigo AND rs.sistema.codigo = :codigoSistema")
    List<RegistroSistema> findRolesByCodigoAndSistema(
        @Param("codigo") String codigo,
        @Param("codigoSistema") String codigoSistema
    );

    /**
     * Buscar registros de un usuario con rol cargado.
     */
    @Query("SELECT rs FROM RegistroSistema rs " +
            "JOIN FETCH rs.rol " +
            "JOIN FETCH rs.sistema " +
            "WHERE rs.usuario = :usuario")
    List<RegistroSistema> findByUsuarioConRol(@Param("usuario") Usuario usuario);

    /**
     * Eliminar todos los registros de un usuario en un sistema.
     */
    @Modifying
    @Query("DELETE FROM RegistroSistema rs " +
            "WHERE rs.usuario = :usuario AND rs.sistema = :sistema")
    void deleteByUsuarioAndSistema(@Param("usuario") Usuario usuario,
                                   @Param("sistema") Sistema sistema);

    /**
     * Eliminar TODOS los registros de rol/sistema de un usuario,
     * sin importar el sistema (usado al eliminar el usuario por completo).
     */
    @Modifying
    @Query("DELETE FROM RegistroSistema rs WHERE rs.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);

}
