package lat.jbluesoft.erpadmin.iam.repository;

import lat.jbluesoft.erpadmin.iam.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository: Rol
 * Acceso a datos de rol
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Buscar rol por código (ADMIN_SYS, ADMIN_APP, ADMIN_DPTO)
     */
    Optional<Rol> findByCodigo(String codigo);
}
