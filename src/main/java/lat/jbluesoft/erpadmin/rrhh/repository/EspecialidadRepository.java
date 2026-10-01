package lat.jbluesoft.erpadmin.rrhh.repository;

import lat.jbluesoft.erpadmin.rrhh.model.Especialidad;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Especialidad
 * Acceso a datos de especialidad
 */
@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {

    /**
     * Buscar especialidades por tipo de personal
     */
    List<Especialidad> findByTipoPersonal(TipoPersonal tipoPersonal);

    /**
     * Buscar por descripción corta
     */
    Optional<Especialidad> findByDescripcionCorta(String descripcionCorta);

    @Query("SELECT a FROM Especialidad a JOIN FETCH a.tipoPersonal " +
            "ORDER BY a.tipoPersonal.idTipoPersonal ASC, a.descripcionCorta ASC")
    List<Especialidad> findAllConTipoPersonal();
}
