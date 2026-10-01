package lat.jbluesoft.erpadmin.iam.model;

import jakarta.persistence.*;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad: Usuario
 * Tabla: usuario
 * Descripción: Usuarios del sistema
 */
@Entity
@Table(name = "usuario", schema = "iam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "codigo", nullable = false, unique = true, length = 9, columnDefinition = "CHAR(9)")
    private String codigo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_personal", unique=true)
    private Personal personal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}