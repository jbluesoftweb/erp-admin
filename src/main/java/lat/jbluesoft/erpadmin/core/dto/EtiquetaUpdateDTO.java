package lat.jbluesoft.erpadmin.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO: EtiquetaUpdateDTO
 * Binding del formulario de administración de etiquetas.
 * Solo expone la clave (identificador, no editable) y el valor personalizado;
 * valorDefecto y descripcion nunca se reciben desde el formulario.
 */
@Data
public class EtiquetaUpdateDTO {

    @NotBlank(message = "La clave de la etiqueta es obligatoria")
    private String clave;

    @Size(max = 100, message = "El valor no puede superar los 100 caracteres")
    private String valorPersonalizado;
}
