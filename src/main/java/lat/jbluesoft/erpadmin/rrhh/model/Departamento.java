package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Entidad Departamento
 * Representa un departamento dentro de una empresa
 */
@Entity
@Table(name = "departamento", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Departamento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento")
    private Integer idDepartamento;

    @Column(name = "descripcion_corta", length = 50)
    private String descripcionCorta;

    @Column(name = "descripcion_larga", length = 200)
    private String descripcionLarga;

    @Column(name = "enabled")
    private Boolean enabled;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_empresa", referencedColumnName = "id_empresa")
    private Empresa empresa;
}