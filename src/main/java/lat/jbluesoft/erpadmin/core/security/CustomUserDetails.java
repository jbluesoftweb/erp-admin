package lat.jbluesoft.erpadmin.core.security;

import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CustomUserDetails
 * Implementa UserDetails de Spring Security
 * Envuelve el Usuario de la BD con los datos necesarios para autenticación
 */
@Getter
public class CustomUserDetails implements UserDetails {
    // Agregar este campo para evitar problemas de serialización
    private static final long serialVersionUID = 1L;
    
    private final Usuario usuario;
    private final List<RegistroSistema> registros;

    public CustomUserDetails(Usuario usuario, List<RegistroSistema> registros) {
        this.usuario = usuario;
        this.registros = registros;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convertir roles a GrantedAuthority
        // Ejemplo: ROLE_ADMIN_SYS, ROLE_ADMIN_APP, ROLE_ADMIN_DPTO
        return registros.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRol().getCodigo()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getCodigo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.getEnabled();
    }

    /**
     * Obtener nombre completo del usuario
     */
    public String getFullName() {
        if (usuario.getPersonal() != null) {
            return usuario.getPersonal().getFullName();
        }
        return usuario.getCodigo();
    }

    /**
     * Obtener ID de departamento del usuario
     * Siempre obtiene desde Personal (fuente de verdad)
     */
    public Integer getIdDepartamento() {
        if (usuario.getPersonal() != null && usuario.getPersonal().getDepartamento() != null) {
            return usuario.getPersonal().getDepartamento().getIdDepartamento();
        }
        return null;
    }

    /**
     * Obtener ID de nivel del usuario
     * Siempre obtiene desde Personal (fuente de verdad)
     */
    public Integer getIdNivel() {
        if (usuario.getPersonal() != null && usuario.getPersonal().getNivel() != null) {
            return usuario.getPersonal().getNivel().getIdNivel();
        }
        return null;
    }

    /**
     * Obtener ID de especialidad del usuario
     * Siempre obtiene desde Personal (fuente de verdad)
     */
    public Integer getIdEspecialidad() {
        if (usuario.getPersonal() != null && usuario.getPersonal().getEspecialidad() != null) {
            return usuario.getPersonal().getEspecialidad().getIdEspecialidad();
        }
        return null;
    }

    /**
     * Obtener antigüedad del usuario
     * Siempre obtiene desde Personal (fuente de verdad)
     */
    public Integer getAntiguedad() {
        if (usuario.getPersonal() != null) {
            return usuario.getPersonal().getAntiguedad();
        }
        return null;
    }

    /**
     * Verificar si tiene un rol específico
     */
    public boolean hasRole(String roleCodigo) {
        return registros.stream()
                .anyMatch(r -> r.getRol().getCodigo().equals(roleCodigo));
    }
}