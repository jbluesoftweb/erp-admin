package lat.jbluesoft.erpadmin.asistencia.repository;

import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository: Ocurrencia
 * Acceso a datos de ocurrencia
 */
@Repository
public interface OcurrenciaRepository extends JpaRepository<Ocurrencia, Integer> {

    /**
     * Buscar ocurrencias habilitadas, ordenadas por orden
     */
    List<Ocurrencia> findByEnabledTrueOrderByOrdenAsc();

    /**
     * Buscar por código (ASI, PER, SS, LIC)
     */
    Optional<Ocurrencia> findByCodigo(String codigo);

    /**
     * Buscar por tipo (DISPONIBLE, DESCUENTO)
     */
    List<Ocurrencia> findByTipoAndEnabledTrueOrderByOrdenAsc(String tipo);

    /**
     * Indica si ya existe una ocurrencia con ese código, sin distinguir mayúsculas
     */
    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Mayor valor de orden registrado (0 si no hay ninguno)
     */
    @Query("SELECT COALESCE(MAX(o.orden), 0) FROM Ocurrencia o")
    Integer obtenerOrdenMaximo();
}
