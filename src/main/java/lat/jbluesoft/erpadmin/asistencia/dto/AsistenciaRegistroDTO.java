package lat.jbluesoft.erpadmin.asistencia.dto;

import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para transportar datos del formulario de registro de asistencia
 * Contiene las listas separadas después de comparar efectivo vs último registro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaRegistroDTO {

    // Fecha del registro que se está registrando
    private LocalDate fechaRegistro;

    // Grupo 1 que van en la tabla "Grupo 1"
    // Incluye: nuevos + los que tenían ASISTIÓ en último registro
    private List<Personal> grupo1;

    // Grupo 2 que van en la tabla "Grupo 2"
    // Incluye: nuevos + los que tenían ASISTIÓ en último registro
    private List<Personal> grupo2;

    // Personas con ocurrencias del último registro (id_ocurrencia != 1)
    // Van pre-llenadas en la tabla "Ocurrencias"
    private List<RegistroDiario> ocurrencias;

    // Total de efectivo actual
    private Integer totalEfectivo;

    // Bandera que indica si existe un registro anterior
    private Boolean existeRegistroAnterior;

    // Fecha del último registro (si existe)
    private LocalDate fechaUltimoRegistro;
}