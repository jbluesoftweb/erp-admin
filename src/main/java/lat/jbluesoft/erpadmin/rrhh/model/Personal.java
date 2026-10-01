package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad: Personal
 * Tabla: personal
 * Descripción: Registro de personal
 */
@Entity
@Table(name = "personal", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personal")
    private Integer idPersonal;

    @Column(name = "codigo", nullable = false, unique = true, length = 9, columnDefinition = "CHAR(9)")
    private String codigo;

    @Column(name = "dni", nullable = false, unique = true, length = 8, columnDefinition = "CHAR(8)")
    private String dni;

    @Column(name = "ap_pat", nullable = false, length = 50)
    private String apPat;  // Apellido Paterno

    @Column(name = "ap_mat", nullable = false, length = 50)
    private String apMat;  // Apellido Materno

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel", nullable = false)
    private Nivel nivel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @Column(name = "antiguedad", nullable = false)
    private Integer antiguedad;

    @Column(name = "cargo", length = 50)
    private String cargo;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 9)
    private String createdBy;  // Código del usuario que creó

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Método de utilidad para obtener el nombre completo
     */
    public String getFullName() {
        return apPat + " " + apMat + " " + nombres;
    }
}