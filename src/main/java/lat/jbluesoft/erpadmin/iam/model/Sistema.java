package lat.jbluesoft.erpadmin.iam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Sistema
 * Tabla: sistema
 * Descripción: Sistemas/Módulos del ERP (ASISTENCIA, GESTION_DOC)
 */
@Entity
@Table(name = "sistema", schema = "iam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sistema")
    private Integer idSistema;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;  // 'ASISTENCIA'

    @Column(name = "descripcion_corta", nullable = false, length = 50)
    private String descripcionCorta;  // 'Asistencia'

    @Column(name = "descripcion_larga", nullable = false, length = 100)
    private String descripcionLarga;  // 'Sistema de Registro de Asistencia'

    @Column(name = "url", length = 255)
    private String url;  // '/asistencia'
}