package lat.jbluesoft.erpadmin.iam.repository;

import lat.jbluesoft.erpadmin.iam.model.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository: Sistema
 * Acceso a datos de sistema
 */
@Repository
public interface SistemaRepository extends JpaRepository<Sistema, Integer> {

    /**
     * Buscar sistema por código (ASISTENCIA, GESTION_DOC)
     */
    Optional<Sistema> findByCodigo(String codigo);


}
