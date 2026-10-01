package lat.jbluesoft.erpadmin.asistencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO: AlmuerzoDTO
 * Consolidado de almuerzo de un departamento
 * Contiene el personal que recibe almuerzo (SOLO presentes)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlmuerzoDTO {

    /**
     * Fecha del almuerzo
     */
    private LocalDate fecha;

    /**
     * Departamento
     */
    private String departamentoDescripcion;

    /**
     * Empresa (para el encabezado)
     */
    private String empresaDescripcion;

    /**
     * Lista del Grupo 1 presentes
     */
    private List<PersonalAlmuerzoDTO> grupo1 = new ArrayList<>();

    /**
     * Lista del Grupo 2 presentes
     */
    private List<PersonalAlmuerzoDTO> grupo2 = new ArrayList<>();

    /**
     * Total del Grupo 1
     */
    private Integer totalGrupo1 = 0;

    /**
     * Total del Grupo 2
     */
    private Integer totalGrupo2 = 0;

    /**
     * Total general
     */
    private Integer totalGeneral = 0;

    /**
     * Calcular totales automáticamente
     */
    public void calcularTotales() {
        this.totalGrupo1 = this.grupo1.size();
        this.totalGrupo2 = this.grupo2.size();
        this.totalGeneral = this.totalGrupo1 + this.totalGrupo2;
    }
}