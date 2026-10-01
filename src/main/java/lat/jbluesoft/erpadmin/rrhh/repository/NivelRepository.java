package lat.jbluesoft.erpadmin.rrhh.repository;

import lat.jbluesoft.erpadmin.rrhh.model.Nivel;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Nivel
 * Acceso a datos de nivel
 */
@Repository
public interface NivelRepository extends JpaRepository<Nivel, Integer> {

    /**
     * Buscar niveles por tipo de personal, ordenados por orden
     */
    List<Nivel> findByTipoPersonalOrderByOrdenAsc(TipoPersonal tipoPersonal);

    /**
     * Buscar por descripción corta
     */
    Optional<Nivel> findByDescripcionCorta(String descripcionCorta);

    @Query("SELECT g FROM Nivel g JOIN FETCH g.tipoPersonal " +
            "ORDER BY g.tipoPersonal.idTipoPersonal ASC, g.orden ASC")
    List<Nivel> findAllConTipoPersonal();
}
