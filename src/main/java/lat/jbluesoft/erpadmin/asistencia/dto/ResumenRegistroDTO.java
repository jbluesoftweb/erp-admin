package lat.jbluesoft.erpadmin.asistencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para el cuadro resumen del registro de asistencia
 * Contiene los totales de efectivos, descuentos y disponibles
 * separados por tipo de personal (Grupo 1 y Grupo 2)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenRegistroDTO {

    // Fecha del registro
    private LocalDate fechaRegistro;

    // Nombre del departamento
    private String nombreDepartamento;

    // GRUPO 1
    private Integer efectivosGrupo1 = 0;      // Total del Grupo 1 registrados
    private Integer descuentosGrupo1 = 0;     // Grupo 1 con ocurrencia != ASISTIÓ
    private Integer disponiblesGrupo1 = 0;    // efectivos - descuentos

    // GRUPO 2
    private Integer efectivosGrupo2 = 0;       // Total del Grupo 2 registrados
    private Integer descuentosGrupo2 = 0;      // Grupo 2 con ocurrencia != ASISTIÓ
    private Integer disponiblesGrupo2 = 0;     // efectivos - descuentos

    // TOTALES
    private Integer efectivosTotal = 0;          // Suma del Grupo 1 + Grupo 2
    private Integer descuentosTotal = 0;         // Suma de descuentos
    private Integer disponiblesTotal = 0;        // Suma de disponibles

    /**
     * Calcula los disponibles automáticamente
     */
    public void calcularDisponibles() {
        this.disponiblesGrupo1 = this.efectivosGrupo1 - this.descuentosGrupo1;
        this.disponiblesGrupo2 = this.efectivosGrupo2 - this.descuentosGrupo2;
        this.disponiblesTotal = this.efectivosTotal - this.descuentosTotal;
    }

    /**
     * Calcula los totales automáticamente
     */
    public void calcularTotales() {
        this.efectivosTotal = this.efectivosGrupo1 + this.efectivosGrupo2;
        this.descuentosTotal = this.descuentosGrupo1 + this.descuentosGrupo2;
        this.disponiblesTotal = this.disponiblesGrupo1 + this.disponiblesGrupo2;
    }
}