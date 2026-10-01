package lat.jbluesoft.erpadmin.rrhh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresa", schema = "rrhh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer idEmpresa;

    @Column(name = "descripcion_corta", length = 50)
    private String descripcionCorta;

    @Column(name = "descripcion_larga", length = 200)
    private String descripcionLarga;
}