package lat.jbluesoft.erpadmin.core.repository;

import lat.jbluesoft.erpadmin.core.model.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository: Etiqueta
 * Acceso a datos de core.etiqueta
 */
@Repository
public interface EtiquetaRepository extends JpaRepository<Etiqueta, String> {
}
