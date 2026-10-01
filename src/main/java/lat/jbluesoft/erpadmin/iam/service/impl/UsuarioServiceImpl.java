package lat.jbluesoft.erpadmin.iam.service.impl;

import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Rol;
import lat.jbluesoft.erpadmin.iam.model.Sistema;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import lat.jbluesoft.erpadmin.iam.repository.RegistroSistemaRepository;
import lat.jbluesoft.erpadmin.iam.repository.RolRepository;
import lat.jbluesoft.erpadmin.iam.repository.SistemaRepository;
import lat.jbluesoft.erpadmin.iam.repository.UsuarioRepository;
import lat.jbluesoft.erpadmin.iam.service.UsuarioService;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.rrhh.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository       usuarioRepository;
    private final PersonalRepository      personalRepository;
    private final RegistroSistemaRepository registroSistemaRepository;
    private final RolRepository           rolRepository;
    private final SistemaRepository       sistemaRepository;
    private final PasswordEncoder         passwordEncoder;

    // =============================================
    // LISTAR / BUSCAR
    // =============================================

    @Override
    public List<Usuario> listarTodos() {
        log.debug("Listando todos los usuarios");
        return usuarioRepository.findAllConPersonalBySistema();
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer id) {
        log.debug("Buscando usuario ID: {}", id);
        return usuarioRepository.findByIdConPersonal(id);
    }

    @Override
    public Optional<Usuario> buscarPorCodigo(String codigo) {
        log.debug("Buscando usuario con código: {}", codigo);
        return usuarioRepository.findByCodigo(codigo);
    }

    @Override
    public List<RegistroSistema> obtenerRegistros(Integer idUsuario) {
        log.debug("Obteniendo registros de sistema para usuario ID: {}", idUsuario);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));
        return registroSistemaRepository.findByUsuarioConRol(usuario);
    }

    // =============================================
    // CREAR USUARIO
    // =============================================

    @Override
    @Transactional
    public Usuario crear(String codigo, String email, String password,
                         Integer idRol, String createdBy) {
        log.info("Creando usuario con código: {}", codigo);

        // Verificar que el personal existe
        Personal personal = personalRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe personal con código: " + codigo +
                                ". Registre primero al personal."));

        // Verificar que no tenga usuario ya
        if (usuarioRepository.findByCodigo(codigo.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "El código " + codigo + " ya tiene un usuario registrado.");
        }

        // Verificar email único si se proporciona
        if (email != null && !email.trim().isEmpty()) {
            if (usuarioRepository.existsByEmail(email.trim())) {
                throw new IllegalArgumentException(
                        "El email " + email + " ya está en uso.");
            }
        }

        // Obtener rol y sistema
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + idRol));

        List<String> rolesExcluidos = List.of("ADMIN_IMINT", "USER_IMINT");
        if (rolesExcluidos.contains(rol.getCodigo())) {
            throw new IllegalArgumentException(
                    "El rol " + rol.getCodigo() + " no pertenece a ERP Admin (Asistencia). "
                    + "Los roles de IMINT App se gestionan desde ese sistema.");
        }

        Sistema sistema = sistemaRepository.findByCodigo("ASISTENCIA")
                .orElseThrow(() -> new IllegalStateException(
                        "Sistema ASISTENCIA no encontrado."));

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setCodigo(codigo.trim());
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setEmail(email != null && !email.trim().isEmpty()
                ? email.trim() : null);
        usuario.setEnabled(true);
        usuario.setPersonal(personal);

        Usuario guardado = usuarioRepository.save(usuario);

        // Asignar rol en ERP Admin
        RegistroSistema registro = new RegistroSistema();
        registro.setUsuario(guardado);
        registro.setRol(rol);
        registro.setSistema(sistema);
        registro.setCreatedBy(createdBy);
        registroSistemaRepository.save(registro);

        log.info("Usuario creado: {} con rol: {}", codigo, rol.getCodigo());
        return guardado;
    }

    // =============================================
    // PASSWORDS
    // =============================================

    @Override
    @Transactional
    public void resetearPassword(Integer idUsuario, String nuevaPassword) {
        log.info("Reseteando password usuario ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        log.info("Password reseteado para usuario: {}", usuario.getCodigo());
    }

    @Override
    @Transactional
    public void cambiarPassword(Integer idUsuario, String passwordActual,
                                String nuevaPassword) {
        log.info("Cambiando password usuario ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        // Verificar password actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "La contraseña actual no es correcta.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        log.info("Password actualizado para usuario: {}", usuario.getCodigo());
    }

    // =============================================
    // ACTUALIZAR
    // =============================================

    @Override
    @Transactional
    public void actualizarEmail(Integer idUsuario, String email) {
        log.info("Actualizando email usuario ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        // Verificar email único
        if (email != null && !email.trim().isEmpty()) {
            if (usuarioRepository.existsByEmailAndIdUsuarioNot(
                    email.trim(), idUsuario)) {
                throw new IllegalArgumentException(
                        "El email " + email + " ya está en uso.");
            }
            usuario.setEmail(email.trim());
        } else {
            usuario.setEmail(null);
        }

        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarRol(Integer idUsuario, Integer idRolNuevo,
                           String updatedBy) {
        log.info("Cambiando rol usuario ID: {} → rol ID: {}",
                idUsuario, idRolNuevo);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        Rol rolNuevo = rolRepository.findById(idRolNuevo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + idRolNuevo));

        Sistema sistema = sistemaRepository.findByCodigo("ASISTENCIA")
                .orElseThrow(() -> new IllegalStateException(
                        "Sistema ASISTENCIA no encontrado."));

        // Eliminar registro actual y crear el nuevo
        List<RegistroSistema> registrosActuales =
                registroSistemaRepository.findByUsuarioAndSistema(usuario, sistema);
        registroSistemaRepository.deleteAll(registrosActuales);
        registroSistemaRepository.flush();


        RegistroSistema nuevo = new RegistroSistema();
        nuevo.setUsuario(usuario);
        nuevo.setRol(rolNuevo);
        nuevo.setSistema(sistema);
        nuevo.setCreatedBy(updatedBy);
        registroSistemaRepository.save(nuevo);

        log.info("Rol actualizado: {} → {}", usuario.getCodigo(), rolNuevo.getCodigo());
    }

    // =============================================
    // SOFT DELETE / REACTIVAR
    // =============================================

    @Override
    @Transactional
    public void deshabilitar(Integer idUsuario, Integer idUsuarioSolicitante) {
        log.info("Deshabilitando usuario ID: {}", idUsuario);

        // No puede deshabilitarse a sí mismo
        if (idUsuario.equals(idUsuarioSolicitante)) {
            throw new IllegalArgumentException(
                    "No puede deshabilitar su propio usuario.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        usuario.setEnabled(false);
        usuarioRepository.save(usuario);
        log.info("Usuario deshabilitado: {}", usuario.getCodigo());
    }

    @Override
    @Transactional
    public void reactivar(Integer idUsuario) {
        log.info("Reactivando usuario ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + idUsuario));

        usuario.setEnabled(true);
        usuarioRepository.save(usuario);
        log.info("Usuario reactivado: {}", usuario.getCodigo());
    }

    @Override
    @Transactional
    public boolean eliminarPorPersonal(Integer idPersonal) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByPersonal_IdPersonal(idPersonal);

        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        String codigo = usuario.getCodigo();

        registroSistemaRepository.deleteByUsuario(usuario);
        usuarioRepository.delete(usuario);

        log.warn("USUARIO ELIMINADO AUTOMÁTICAMENTE — Código: {} (personal ID {} cambió de empresa/departamento)",
                codigo, idPersonal);

        return true;
    }
}