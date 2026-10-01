package lat.jbluesoft.erpadmin.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad: Etiqueta
 * Tabla: core.etiqueta
 * Descripción: Títulos/labels administrables usados en las vistas
 */
@Entity
@Table(name = "etiqueta", schema = "core")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Etiqueta {

    @Id
    @Column(name = "clave", length = 100)
    private String clave;

    @Column(name = "valor_defecto", nullable = false, length = 100)
    private String valorDefecto;

    @Column(name = "valor_personalizado", length = 100)
    private String valorPersonalizado;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
