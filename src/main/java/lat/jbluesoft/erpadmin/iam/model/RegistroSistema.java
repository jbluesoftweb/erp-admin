package lat.jbluesoft.erpadmin.iam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad: Registro Sistema
 * Tabla: registro_sistema
 * Descripción: Asignación Usuario-Rol-Sistema
 */
@Entity
@Table(name = "registro_sistema", schema = "iam",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_rol", "id_sistema"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema", nullable = false)
    private Sistema sistema;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 9)
    private String createdBy;  // Código del usuario que asignó

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}