package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Especialidad
 * Tabla: especialidad
 * Descripción: Especialidades del personal
 */
@Entity
@Table(name = "especialidad", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_personal", nullable = false)
    private TipoPersonal tipoPersonal;

    @Column(name = "descripcion_corta", nullable = false, length = 50)
    private String descripcionCorta;  // Abreviatura de la especialidad

    @Column(name = "descripcion_larga", nullable = false, length = 100)
    private String descripcionLarga;  // Nombre completo de la especialidad
}