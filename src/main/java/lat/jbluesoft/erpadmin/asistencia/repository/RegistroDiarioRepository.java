package lat.jbluesoft.erpadmin.asistencia.repository;

import lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroDiarioRepository extends JpaRepository<RegistroDiario, Integer> {

    // MEJORA: Cargamos personal, nivel, especialidad y ocurrencia para evitar N+1
    @EntityGraph(attributePaths = {"personal", "nivel", "especialidad", "departamento", "ocurrencia"})
    List<RegistroDiario> findByDepartamento_IdDepartamentoAndFechaRegistro(Integer idDepartamento, LocalDate fechaRegistro);

    @EntityGraph(attributePaths = {"personal", "nivel", "especialidad", "departamento", "ocurrencia"})
    List<RegistroDiario> findByFechaRegistro(LocalDate fechaRegistro);

    boolean existsByDepartamento_IdDepartamentoAndFechaRegistro(Integer idDepartamento, LocalDate fechaRegistro);

    @EntityGraph(attributePaths = {"personal", "nivel", "especialidad", "departamento", "ocurrencia"})
    List<RegistroDiario> findByDepartamento_IdDepartamentoAndFechaRegistroBetween(
            Integer idDepartamento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    @EntityGraph(attributePaths = {"personal", "nivel", "especialidad", "departamento", "ocurrencia"})
    List<RegistroDiario> findByOcurrencia_IdOcurrenciaAndFechaRegistro(Integer idOcurrencia, LocalDate fechaRegistro);

    @Query("SELECT pd FROM RegistroDiario pd " +
            "JOIN FETCH pd.personal p " +
            "JOIN FETCH pd.nivel g " +
            "JOIN FETCH pd.especialidad a " +
            "JOIN FETCH pd.departamento c " +
            "JOIN FETCH pd.ocurrencia o " +
            "WHERE c.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "ORDER BY g.orden, pd.antiguedad ASC")
    List<RegistroDiario> findByDepartamentoYFechaOrdenados(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro
    );

    @Query("SELECT COUNT(pd) FROM RegistroDiario pd " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND pd.ocurrencia.idOcurrencia = 1")
    Long contarAsistenciasPorDepartamentoYFecha(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro
    );

    @Query("SELECT COUNT(pd) FROM RegistroDiario pd " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND pd.ocurrencia.idOcurrencia != 1")
    Long contarOcurrenciasPorDepartamentoYFecha(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro
    );

    void deleteByDepartamento_IdDepartamentoAndFechaRegistro(Integer idDepartamento, LocalDate fechaRegistro);

    @Query("SELECT pd FROM RegistroDiario pd " +
            "JOIN FETCH pd.personal " +
            "JOIN FETCH pd.nivel " +
            "JOIN FETCH pd.especialidad " +
            "JOIN FETCH pd.departamento " +
            "JOIN FETCH pd.ocurrencia " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = (SELECT MAX(pd2.fechaRegistro) " +
            "FROM RegistroDiario pd2 " +
            "WHERE pd2.departamento.idDepartamento = :idDepartamento)")
    List<RegistroDiario> findUltimoRegistroPorDepartamento(@Param("idDepartamento") Integer idDepartamento);

    /**
     * Obtiene la fecha del último registro de un departamento.
     */
    @Query("SELECT MAX(p.fechaRegistro) FROM RegistroDiario p " +
            "WHERE p.departamento.idDepartamento = :idDepartamento")
    Optional<LocalDate> findFechaUltimoRegistroPorDepartamento(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT pd FROM RegistroDiario pd " +
            "JOIN FETCH pd.personal " +
            "JOIN FETCH pd.nivel " +
            "JOIN FETCH pd.especialidad " +
            "JOIN FETCH pd.departamento " +
            "JOIN FETCH pd.ocurrencia " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = (SELECT MAX(pd2.fechaRegistro) " +
            "FROM RegistroDiario pd2 " +
            "WHERE pd2.departamento.idDepartamento = :idDepartamento) " +
            "AND pd.ocurrencia.idOcurrencia != 1")
    List<RegistroDiario> findOcurrenciasUltimoRegistro(@Param("idDepartamento") Integer idDepartamento);

    // ========== NUEVOS MÉTODOS PARA IMPRIMIR REGISTRO ==========

    /**
     * Cuenta el total de efectivos (todos los registros) por tipo de personal
     */
    @Query("SELECT COUNT(pd) FROM RegistroDiario pd " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND pd.nivel.tipoPersonal.idTipoPersonal = :tipoPersonal")
    Long contarEfectivosPorTipoPersonal(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    /**
     * Cuenta los descuentos (ocurrencias != ASISTIÓ) por tipo de personal
     */
    @Query("SELECT COUNT(pd) FROM RegistroDiario pd " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND pd.nivel.tipoPersonal.idTipoPersonal = :tipoPersonal " +
            "AND pd.ocurrencia.idOcurrencia != 1")
    Long contarDescuentosPorTipoPersonal(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    /**
     * Obtiene las ocurrencias (id_ocurrencia != 1) por tipo de personal
     * Ordenadas por nivel y antigüedad
     */
    @Query("SELECT pd FROM RegistroDiario pd " +
            "JOIN FETCH pd.personal " +
            "JOIN FETCH pd.nivel g " +
            "JOIN FETCH pd.especialidad " +
            "JOIN FETCH pd.departamento " +
            "JOIN FETCH pd.ocurrencia o " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND g.tipoPersonal.idTipoPersonal = :tipoPersonal " +
            "AND o.idOcurrencia != 1 " +
            "ORDER BY o.idOcurrencia, g.orden, pd.antiguedad ASC")
    List<RegistroDiario> findOcurrenciasPorTipoPersonal(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    /**
     * Obtiene el personal presente (id_ocurrencia = 1)
     * Mezclados: Grupo 1 + Grupo 2 ordenados por antigüedad
     */
    @Query("SELECT pd FROM RegistroDiario pd " +
            "JOIN FETCH pd.personal " +
            "JOIN FETCH pd.nivel " +
            "JOIN FETCH pd.especialidad " +
            "JOIN FETCH pd.departamento " +
            "JOIN FETCH pd.ocurrencia o " +
            "WHERE pd.departamento.idDepartamento = :idDepartamento " +
            "AND pd.fechaRegistro = :fechaRegistro " +
            "AND o.idOcurrencia = 1 " +
            "ORDER BY pd.antiguedad ASC")
    List<RegistroDiario> findPersonalPresente(
            @Param("idDepartamento") Integer idDepartamento,
            @Param("fechaRegistro") LocalDate fechaRegistro
    );

    // ========== MÉTODOS PARA CONSOLIDADO (ADMIN_APP) ==========

    /**
     * Contar TODOS los registros (efectivos) por departamento y tipo de personal
     * Efectivos = TOTAL de personal registrado (con cualquier ocurrencia)
     */
    @Query("SELECT new lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO(" +
            "c.idDepartamento, c.descripcionCorta, COUNT(p)) " +
            "FROM RegistroDiario p " +
            "JOIN p.departamento c " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.nivel.tipoPersonal.idTipoPersonal = :tipoPersonal " +
            "GROUP BY c.idDepartamento, c.descripcionCorta " +
            "ORDER BY c.idDepartamento")
    List<ConsolidadoDepartamentoDTO> contarEfectivosPorDepartamento(
            @Param("fecha") LocalDate fecha,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    /**
     * Contar descuentos por departamento y tipo de personal
     * Retorna el personal que NO asistió (id_ocurrencia != 1)
     */
    @Query("SELECT new lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO(" +
            "c.idDepartamento, c.descripcionCorta, COUNT(p)) " +
            "FROM RegistroDiario p " +
            "JOIN p.departamento c " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.nivel.tipoPersonal.idTipoPersonal = :tipoPersonal " +
            "AND p.ocurrencia.idOcurrencia != 1 " +
            "GROUP BY c.idDepartamento, c.descripcionCorta " +
            "ORDER BY c.idDepartamento")
    List<ConsolidadoDepartamentoDTO> contarDescuentosPorDepartamento(
            @Param("fecha") LocalDate fecha,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    /**
     * Contar TODOS los registros (efectivos) por departamento, sin distinguir tipo de personal
     * Usado para el cuadro consolidado unificado (Grupo 1 + Grupo 2)
     */
    @Query("SELECT new lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO(" +
            "c.idDepartamento, c.descripcionCorta, COUNT(p)) " +
            "FROM RegistroDiario p " +
            "JOIN p.departamento c " +
            "WHERE p.fechaRegistro = :fecha " +
            "GROUP BY c.idDepartamento, c.descripcionCorta " +
            "ORDER BY c.idDepartamento")
    List<ConsolidadoDepartamentoDTO> contarEfectivosPorDepartamentoTotal(@Param("fecha") LocalDate fecha);

    /**
     * Contar descuentos por departamento, sin distinguir tipo de personal
     * Usado para el cuadro consolidado unificado (Grupo 1 + Grupo 2)
     */
    @Query("SELECT new lat.jbluesoft.erpadmin.asistencia.dto.ConsolidadoDepartamentoDTO(" +
            "c.idDepartamento, c.descripcionCorta, COUNT(p)) " +
            "FROM RegistroDiario p " +
            "JOIN p.departamento c " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.ocurrencia.idOcurrencia != 1 " +
            "GROUP BY c.idDepartamento, c.descripcionCorta " +
            "ORDER BY c.idDepartamento")
    List<ConsolidadoDepartamentoDTO> contarDescuentosPorDepartamentoTotal(@Param("fecha") LocalDate fecha);

    /**
     * Listar todas las ocurrencias (sin ASISTIÓ) para el consolidado
     * Filtrado por tipo de personal
     * Ordenado por ocurrencia, nivel, antigüedad
     */
    // (Este ya estaba perfecto, no se tocó)
    @Query("SELECT p FROM RegistroDiario p " +
            "JOIN FETCH p.personal " +
            "JOIN FETCH p.nivel " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH p.ocurrencia " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.nivel.tipoPersonal.idTipoPersonal = :tipoPersonal " +
            "AND p.ocurrencia.idOcurrencia != 1 " +
            "ORDER BY p.ocurrencia.idOcurrencia, p.nivel.orden ASC, p.antiguedad ASC")
    List<RegistroDiario> findOcurrenciasConsolidadoPorTipoPersonal(
            @Param("fecha") LocalDate fecha,
            @Param("tipoPersonal") Integer tipoPersonal
    );

    @Query("SELECT p FROM RegistroDiario p " +
            "JOIN FETCH p.personal " +
            "JOIN FETCH p.nivel " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH p.ocurrencia " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.ocurrencia.idOcurrencia = 1 " +
            "ORDER BY p.antiguedad ASC")
    List<RegistroDiario> findPersonalPresenteConsolidado(@Param("fecha") LocalDate fecha);

    /**
     * Listar todas las ocurrencias (sin ASISTIÓ) para el consolidado, sin filtrar por tipo de personal
     * Ordenado por ocurrencia y antigüedad (mismo criterio que Personal Presente)
     */
    @Query("SELECT p FROM RegistroDiario p " +
            "JOIN FETCH p.personal " +
            "JOIN FETCH p.nivel " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH p.ocurrencia " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.ocurrencia.idOcurrencia != 1 " +
            "ORDER BY p.ocurrencia.idOcurrencia, p.antiguedad ASC")
    List<RegistroDiario> findOcurrenciasConsolidado(@Param("fecha") LocalDate fecha);

    // ========== MÉTODOS PARA ALMUERZO ==========

    /**
     * Obtener personal PRESENTE de un departamento por fecha
     * SOLO los que ASISTIERON (id_ocurrencia = 1)
     * Para el módulo de Almuerzo
     */
    @Query("SELECT p FROM RegistroDiario p " +
            "JOIN FETCH p.personal " +
            "JOIN FETCH p.nivel g " +
            "JOIN FETCH p.especialidad " +
            "JOIN FETCH p.departamento " +
            "JOIN FETCH p.ocurrencia o " +
            "WHERE p.fechaRegistro = :fecha " +
            "AND p.departamento.idDepartamento = :idDepartamento " +
            "AND o.almuerzo = true " +
            "ORDER BY g.tipoPersonal.idTipoPersonal ASC, g.orden ASC, p.antiguedad ASC")
    List<RegistroDiario> findPersonalPresentePorDepartamentoYFecha(
            @Param("fecha") LocalDate fecha,
            @Param("idDepartamento") Integer idDepartamento
    );

    /**
     * Contar registros de asistencia por fecha y departamento
     * Usado para verificar si existe registro
     */
    long countByFechaRegistroAndDepartamento_IdDepartamento(LocalDate fechaRegistro, Integer idDepartamento);

    /**
     * Verifica si existe registro para un personal en una fecha específica.
     * Usado para bloquear movimientos cuando hay registro activo hoy.
     */
    boolean existsByPersonal_IdPersonalAndFechaRegistro(Integer idPersonal, LocalDate fechaRegistro);

    /**
     * Lista todos los registros de asistencia de un departamento en una fecha específica.
     */
    @EntityGraph(attributePaths = {"personal", "nivel", "especialidad", "departamento", "ocurrencia"})
        List<RegistroDiario> findByPersonal_IdPersonalAndFechaRegistro(Integer idPersonal, LocalDate fechaRegistro);

    /**
     * Verifica si existe registro presente o futuro para un personal.
     * Bloquea movimientos cuando hay registro hoy o en días futuros.
     */
    boolean existsByPersonal_IdPersonalAndFechaRegistroGreaterThanEqual(Integer idPersonal, LocalDate fecha);

    /**
     * Verifica si existe registro para un personal dentro de una ventana de fechas.
     * Usado para bloquear movimientos cuando hay registro entre el
     * día hábil anterior y el día hábil siguiente a hoy (ventana ampliada).
     */
    boolean existsByPersonal_IdPersonalAndFechaRegistroBetween(
            Integer idPersonal, LocalDate desde, LocalDate hasta);

    List<RegistroDiario> findByPersonal_IdPersonalAndFechaRegistroBetween(
            Integer idPersonal, LocalDate desde, LocalDate hasta);

    List<RegistroDiario> findByPersonal_IdPersonalAndFechaRegistroGreaterThanEqualOrderByFechaRegistroAsc(
            Integer idPersonal, LocalDate fecha);

    List<RegistroDiario> findByDepartamento_IdDepartamentoAndFechaRegistroAfter(
            Integer idDepartamento, LocalDate fecha);
}