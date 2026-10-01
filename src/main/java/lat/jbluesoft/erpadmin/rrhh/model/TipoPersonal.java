package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Tipo de Personal
 * Tabla: tipo_personal
 * Descripción: Clasifica el personal por tipo (ej. profesional, técnico)
 */
@Entity
@Table(name = "tipo_personal", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_personal")
    private Integer idTipoPersonal;

    @Column(name = "descripcion_corta", nullable = false, length = 10, unique = true)
    private String descripcionCorta;  // Código corto del tipo (ej. 'PROF', 'TEC')

    @Column(name = "descripcion_larga", nullable = false, length = 50)
    private String descripcionLarga;  // Descripción del tipo (ej. 'Profesional', 'Técnico')
}