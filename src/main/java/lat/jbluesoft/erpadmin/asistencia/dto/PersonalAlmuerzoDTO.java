package lat.jbluesoft.erpadmin.asistencia.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: PersonalAlmuerzoDTO
 * Representa a una persona que recibe almuerzo
 * Datos para la lista de almuerzo
 */
@Data
@NoArgsConstructor
public class PersonalAlmuerzoDTO {

    /**
     * Número de orden en la lista (1, 2, 3...)
     */
    private Integer numero;

    /**
     * Nivel (snapshot)
     */
    private String nivel;

    /**
     * Especialidad (snapshot)
     */
    private String especialidad;

    /**
     * Nombre completo (Apellidos y Nombres)
     */
    private String nombreCompleto;

    /**
     * Código del personal
     */
    private String codigo;

    /**
     * Constructor desde RegistroDiario
     */
    public PersonalAlmuerzoDTO(Integer numero, String nivel, String especialidad, String nombreCompleto, String codigo) {
        this.numero = numero;
        this.nivel = nivel;
        this.especialidad = especialidad;
        this.nombreCompleto = nombreCompleto;
        this.codigo = codigo;
    }
}