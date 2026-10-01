package lat.jbluesoft.erpadmin.asistencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para recibir el registro masivo de asistencia
 * Contiene la fecha del registro y la lista de todos los registros individuales
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaBloqueDTO {
    
    @NotNull(message = "La fecha del registro es obligatoria")
    private LocalDate fechaRegistro;
    
    @Valid
    @NotNull(message = "La lista de registros no puede ser nula")
    private List<RegistroIndividualDTO> registros;
    
    /**
     * DTO para cada registro individual de asistencia
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistroIndividualDTO {
        
        @NotNull(message = "El ID del personal es obligatorio")
        private Integer idPersonal;
        
        @NotNull(message = "El ID de la ocurrencia es obligatorio")
        private Integer idOcurrencia;
        
        // Campos opcionales (solo requeridos si ocurrencia != Asistió)
        private LocalDate fechaInicio;
        
        private LocalDate fechaTermino;
        
        private String detalle;
    }
}
