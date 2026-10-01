package lat.jbluesoft.erpadmin.asistencia.dto;

import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO para el consolidado general de asistencia (ADMIN_APP)
 * Contiene datos agregados de todos los departamentos de una empresa
 */
@Data
@NoArgsConstructor
public class ConsolidadoRegistroDTO {

    /**
     * Fecha del registro consolidado
     */
    private LocalDate fechaRegistro;

    /**
     * Descripción de la empresa (ej: "Mi Empresa S.A.C.")
     */
    private String empresaDescripcion;

    // ========== GRUPO 1 ==========

    /**
     * Lista de consolidados por departamento (Grupo 1)
     * Cada elemento tiene: departamentoDescripcion, efectivos, descuentos, disponibles
     */
    private List<ConsolidadoDepartamentoDTO> consolidadoGrupo1 = new ArrayList<>();

    /**
     * Total de efectivos Grupo 1 (suma de todos los departamentos)
     */
    private Long totalEfectivosGrupo1 = 0L;

    /**
     * Total de descuentos Grupo 1 (suma de todos los departamentos)
     */
    private Long totalDescuentosGrupo1 = 0L;

    /**
     * Total de disponibles Grupo 1 (efectivos - descuentos)
     */
    private Long totalDisponiblesGrupo1 = 0L;

    /**
     * Ocurrencias del Grupo 1 agrupadas por tipo
     * Key: Descripción de la ocurrencia (ej: "PERMISO", "COMISIÓN")
     * Value: Lista de registros con esa ocurrencia
     */
    private Map<String, List<RegistroDiario>> ocurrenciasGrupo1Agrupadas = new LinkedHashMap<>();

    // ========== GRUPO 2 ==========

    /**
     * Lista de consolidados por departamento (Grupo 2)
     */
    private List<ConsolidadoDepartamentoDTO> consolidadoGrupo2 = new ArrayList<>();

    /**
     * Total de efectivos Grupo 2 (suma de todos los departamentos)
     */
    private Long totalEfectivosGrupo2 = 0L;

    /**
     * Total de descuentos Grupo 2 (suma de todos los departamentos)
     */
    private Long totalDescuentosGrupo2 = 0L;

    /**
     * Total de disponibles Grupo 2 (efectivos - descuentos)
     */
    private Long totalDisponiblesGrupo2 = 0L;

    /**
     * Ocurrencias del Grupo 2 agrupadas por tipo
     * Key: Descripción de la ocurrencia
     * Value: Lista de registros con esa ocurrencia
     */
    private Map<String, List<RegistroDiario>> ocurrenciasGrupo2Agrupadas = new LinkedHashMap<>();

    // ========== TOTAL POR DEPARTAMENTO (Grupo 1 + Grupo 2 unificado) ==========

    /**
     * Lista de consolidados por departamento, combinando Grupo 1 y Grupo 2
     */
    private List<ConsolidadoDepartamentoDTO> consolidadoTotal = new ArrayList<>();

    /**
     * Ocurrencias (Grupo 1 + Grupo 2) agrupadas por tipo de ocurrencia
     * Key: Descripción de la ocurrencia (ej: "PERMISO", "COMISIÓN")
     * Value: Lista de registros con esa ocurrencia
     */
    private Map<String, List<RegistroDiario>> ocurrenciasAgrupadas = new LinkedHashMap<>();

    // ========== PERSONAL PRESENTE ==========

    private List<RegistroDiario> personalPresente = new ArrayList<>();

    // ========== TOTALES GENERALES ==========

    /**
     * Total general de efectivos (Grupo 1 + Grupo 2)
     */
    private Long totalEfectivosGeneral = 0L;

    /**
     * Total general de descuentos (Grupo 1 + Grupo 2)
     */
    private Long totalDescuentosGeneral = 0L;

    /**
     * Total general de disponibles (Grupo 1 + Grupo 2)
     */
    private Long totalDisponiblesGeneral = 0L;

    /**
     * Calcular todos los totales
     */
    public void calcularTotales() {
        // Totales Grupo 1
        this.totalEfectivosGrupo1 = consolidadoGrupo1.stream()
                .mapToLong(ConsolidadoDepartamentoDTO::getEfectivos)
                .sum();

        this.totalDescuentosGrupo1 = consolidadoGrupo1.stream()
                .mapToLong(ConsolidadoDepartamentoDTO::getDescuentos)
                .sum();

        this.totalDisponiblesGrupo1 = this.totalEfectivosGrupo1 - this.totalDescuentosGrupo1;

        // Totales Grupo 2
        this.totalEfectivosGrupo2 = consolidadoGrupo2.stream()
                .mapToLong(ConsolidadoDepartamentoDTO::getEfectivos)
                .sum();

        this.totalDescuentosGrupo2 = consolidadoGrupo2.stream()
                .mapToLong(ConsolidadoDepartamentoDTO::getDescuentos)
                .sum();

        this.totalDisponiblesGrupo2 = this.totalEfectivosGrupo2 - this.totalDescuentosGrupo2;

        // Totales generales
        this.totalEfectivosGeneral = this.totalEfectivosGrupo1 + this.totalEfectivosGrupo2;
        this.totalDescuentosGeneral = this.totalDescuentosGrupo1 + this.totalDescuentosGrupo2;
        this.totalDisponiblesGeneral = this.totalDisponiblesGrupo1 + this.totalDisponiblesGrupo2;
    }
}