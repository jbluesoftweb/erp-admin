package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Nivel
 * Tabla: nivel
 * Descripción: Niveles dentro de la organización
 */
@Entity
@Table(name = "nivel", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel")
    private Integer idNivel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_personal", nullable = false)
    private TipoPersonal tipoPersonal;

    @Column(name = "descripcion_corta", nullable = false, length = 50)
    private String descripcionCorta;  // Abreviatura del nivel

    @Column(name = "descripcion_larga", nullable = false, length = 100)
    private String descripcionLarga;  // Nombre completo del nivel

    @Column(name = "orden", nullable = false)
    private Integer orden;  // Para ordenar en listados
}