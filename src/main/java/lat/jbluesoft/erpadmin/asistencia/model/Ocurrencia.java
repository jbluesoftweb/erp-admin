package lat.jbluesoft.erpadmin.asistencia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Ocurrencia
 * Tabla: ocurrencia
 * Descripción: Ocurrencias del registro de asistencia (Asistió, Permiso, etc.)
 */
@Entity
@Table(name = "ocurrencia", schema = "asistencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ocurrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocurrencia")
    private Integer idOcurrencia;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;  // 'ASI', 'PER', 'SS', 'LIC'

    @Column(name = "descripcion", nullable = false, length = 50)
    private String descripcion;  // 'Asistió', 'Permiso'

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;  // 'DISPONIBLE', 'DESCUENTO'

    @Column(name = "color", length = 7)
    private String color;  // '#28a745' (verde), '#dc3545' (rojo)

    @Column(name = "orden")
    private Integer orden;  // Para ordenar en UI

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "almuerzo", nullable = false)
    private Boolean almuerzo = false;


}