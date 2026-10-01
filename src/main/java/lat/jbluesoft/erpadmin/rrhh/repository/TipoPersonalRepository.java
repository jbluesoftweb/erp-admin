package lat.jbluesoft.erpadmin.rrhh.repository;

import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository: TipoPersonal
 * Acceso a datos de tipo_personal
 */
@Repository
public interface TipoPersonalRepository extends JpaRepository<TipoPersonal, Integer> {

    /**
     * Buscar por descripción corta del tipo de personal
     */
    Optional<TipoPersonal> findByDescripcionCorta(String descripcionCorta);
}
