package lat.jbluesoft.erpadmin.iam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad: Rol
 * Tabla: rol
 * Descripción: Roles del sistema (ADMIN_SYS, ADMIN_APP, ADMIN_DPTO)
 */
@Entity
@Table(name = "rol", schema = "iam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;  // 'ADMIN_SYS', 'ADMIN_APP', 'ADMIN_DPTO'

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;  // 'Administrador del Sistema'
}