package lat.jbluesoft.erpadmin.core.security;

import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import lat.jbluesoft.erpadmin.iam.repository.RegistroSistemaRepository;
import lat.jbluesoft.erpadmin.iam.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomUserDetailsService
 * Servicio para cargar usuarios desde la base de datos
 * Implementa UserDetailsService de Spring Security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroSistemaRepository registroSistemaRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String codigo) throws UsernameNotFoundException {
        log.debug("Intentando autenticar usuario con código: {}", codigo);

        // Buscar usuario con detalles (EAGER fetch)
        Usuario usuario = usuarioRepository.findByCodigoWithDetails(codigo)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado: {}", codigo);
                    return new UsernameNotFoundException("Usuario no encontrado: " + codigo);
                });

        // Verificar que esté habilitado
        if (!usuario.getEnabled()) {
            log.warn("Usuario deshabilitado: {}", codigo);
            throw new UsernameNotFoundException("Usuario deshabilitado: " + codigo);
        }

        // Cargar roles del sistema ASISTENCIA
        List<RegistroSistema> registros = registroSistemaRepository
                .findRolesByCodigoAndSistema(codigo, "ASISTENCIA");

        if (registros.isEmpty()) {
            log.warn("Usuario sin roles asignados en ASISTENCIA: {}", codigo);
            throw new UsernameNotFoundException("Usuario sin permisos para ASISTENCIA: " + codigo);
        }

        log.info("Usuario autenticado exitosamente: {} con {} rol(es)", 
                 codigo, registros.size());

        return new CustomUserDetails(usuario, registros);
    }
}
