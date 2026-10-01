package lat.jbluesoft.erpadmin.asistencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para datos consolidados de un departamento en el registro general
 * Contiene efectivos, descuentos y disponibles agregados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsolidadoDepartamentoDTO {

    /**
     * ID del departamento
     */
    private Integer idDepartamento;

    /**
     * Descripción corta del departamento (ej: "DEPTO-1", "DEPTO-2")
     */
    private String departamentoDescripcion;

    /**
     * Total de efectivos (personal con ocurrencia = ASISTIÓ)
     */
    private Long efectivos;

    /**
     * Total de descuentos (personal con ocurrencia != ASISTIÓ)
     */
    private Long descuentos;

    /**
     * Disponibles (efectivos - descuentos)
     * Se calcula en el Service
     */
    private Long disponibles;

    /**
     * Constructor para queries que solo retornan idDepartamento, descripcion, efectivos
     */
    public ConsolidadoDepartamentoDTO(Integer idDepartamento, String departamentoDescripcion, Long efectivos) {
        this.idDepartamento = idDepartamento;
        this.departamentoDescripcion = departamentoDescripcion;
        this.efectivos = efectivos;
        this.descuentos = 0L;
        this.disponibles = efectivos;
    }

    /**
     * Calcular disponibles
     */
    public void calcularDisponibles() {
        this.disponibles = this.efectivos - this.descuentos;
    }
}