package lat.jbluesoft.erpadmin.asistencia.model;

import jakarta.persistence.*;
import lat.jbluesoft.erpadmin.rrhh.model.Especialidad;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.rrhh.model.Nivel;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad: Registro Diario
 * Tabla: registro_diario
 * Descripción: Registro diario de asistencia y ocurrencias del personal
 */
@Entity
@Table(name = "registro_diario", schema = "asistencia",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fecha_registro", "codigo"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal personal;

    @Column(name = "codigo", nullable = false, length = 9, columnDefinition = "CHAR(9)")
    private String codigo;  // Redundante para velocidad

    @Column(name = "antiguedad", nullable = false)
    private Integer antiguedad;  // Para ordenar

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    // Snapshot histórico (para cuando cambien de nivel/especialidad)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel", nullable = false)
    private Nivel nivel;

    @Column(name = "nivel_snapshot", length = 50)
    private String nivelSnapshot;  // Descripción corta del nivel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidad especialidad;

    @Column(name = "especialidad_snapshot", length = 50)
    private String especialidadSnapshot;  // Descripción corta de la especialidad

    @Column(name = "full_name", length = 150)
    private String fullName;  // ap_pat + ap_mat + nombres (para velocidad)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ocurrencia", nullable = false)
    private Ocurrencia ocurrencia;

    @Column(name = "detalle", length = 255)
    private String detalle;  // Observaciones

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;  // Inicio del permiso/licencia

    @Column(name = "fecha_termino")
    private LocalDate fechaTermino;  // Fin del permiso/licencia

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 9)
    private String createdBy;  // Código del usuario que registró

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // Auto-llenar snapshots si no están ya seteados
        if (nivelSnapshot == null && nivel != null) {
            nivelSnapshot = nivel.getDescripcionCorta();
        }
        if (especialidadSnapshot == null && especialidad != null) {
            especialidadSnapshot = especialidad.getDescripcionCorta();
        }
        if (fullName == null && personal != null) {
            fullName = personal.getFullName();
        }
        if (codigo == null && personal != null) {
            codigo = personal.getCodigo();
        }
        if (antiguedad == null && personal != null) {
            antiguedad = personal.getAntiguedad();
        }
    }
}