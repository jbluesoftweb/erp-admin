package lat.jbluesoft.erpadmin.asistencia.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lombok.Data;

/**
 * DTO: OcurrenciaEditarDTO
 * Binding del formulario de edición de ocurrencias.
 * Solo descripcion, color y orden se aplican. codigo, tipo, almuerzo y enabled
 * existen únicamente para detectar si llegaron en la petición: el servicio
 * los ignora siempre y conserva los valores guardados.
 */
@Data
public class OcurrenciaEditarDTO {

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres")
    private String descripcion;

    @NotBlank(message = "El color es obligatorio")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "El color debe tener el formato #RRGGBB, por ejemplo #1F6BE0")
    private String color;

    /** Vacío: se conserva el orden actual */
    @Min(value = 1, message = "El orden debe ser un número entero mayor o igual a 1")
    private Integer orden;

    // No editables: se ignoran (ver OcurrenciaService.actualizar)
    private String codigo;
    private String tipo;
    private Boolean almuerzo;
    private Boolean enabled;

    public static OcurrenciaEditarDTO desde(Ocurrencia ocurrencia) {
        OcurrenciaEditarDTO dto = new OcurrenciaEditarDTO();
        dto.setDescripcion(ocurrencia.getDescripcion());
        dto.setColor(ocurrencia.getColor() != null ? ocurrencia.getColor().toUpperCase() : null);
        dto.setOrden(ocurrencia.getOrden());
        return dto;
    }
}
