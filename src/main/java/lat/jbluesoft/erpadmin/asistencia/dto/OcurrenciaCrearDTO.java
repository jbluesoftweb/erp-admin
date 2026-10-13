package lat.jbluesoft.erpadmin.asistencia.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO: OcurrenciaCrearDTO
 * Binding del formulario de alta de ocurrencias.
 * enabled no se recibe: toda ocurrencia nueva queda habilitada.
 * El servicio vuelve a validar y normaliza (código y color en mayúsculas).
 */
@Data
public class OcurrenciaCrearDTO {

    /** Color con el que se abre el formulario de alta */
    public static final String COLOR_INICIAL = "#64708A";

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^([A-Za-z0-9]{2,10})?$",
            message = "El código debe tener de 2 a 10 letras (sin tildes ni ñ) o números, sin espacios")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres")
    private String descripcion;

    @NotBlank(message = "Elige un tipo")
    @Pattern(regexp = "^(DISPONIBLE|DESCUENTO)?$", message = "El tipo debe ser DISPONIBLE o DESCUENTO")
    private String tipo;

    @NotNull(message = "Indica si la ocurrencia cuenta para el almuerzo")
    private Boolean almuerzo;

    @NotBlank(message = "El color es obligatorio")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "El color debe tener el formato #RRGGBB, por ejemplo #1F6BE0")
    private String color = COLOR_INICIAL;

    /** Vacío: se asigna el mayor orden existente + 1 */
    @Min(value = 1, message = "El orden debe ser un número entero mayor o igual a 1")
    private Integer orden;
}
