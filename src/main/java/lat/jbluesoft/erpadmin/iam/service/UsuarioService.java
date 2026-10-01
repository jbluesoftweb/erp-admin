package lat.jbluesoft.erpadmin.iam.service;

import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * UsuarioService
 * Lógica de negocio para gestión de usuarios del sistema ERP Admin.
 * Solo ADMIN_SYS puede ejecutar estas operaciones.
 */
public interface UsuarioService {

    /**
     * Listar todos los usuarios con sus registros de sistema.
     */
    List<Usuario> listarTodos();

    /**
     * Buscar usuario por ID.
     */
    Optional<Usuario> buscarPorId(Integer id);

    /**
     * Buscar usuario por código.
     */
    Optional<Usuario> buscarPorCodigo(String codigo);

    /**
     * Crear nuevo usuario.
     * Valida que el código exista en personal y no tenga usuario previo.
     * Asigna rol en ERP Admin automáticamente.
     */
    Usuario crear(String codigo, String email, String password,
                  Integer idRol, String createdBy);

    /**
     * Resetear contraseña de cualquier usuario.
     * Solo ADMIN_SYS.
     */
    void resetearPassword(Integer idUsuario, String nuevaPassword);

    /**
     * Cambiar propia contraseña.
     * El usuario debe confirmar su password actual.
     */
    void cambiarPassword(Integer idUsuario, String passwordActual,
                         String nuevaPassword);

    /**
     * Actualizar email.
     */
    void actualizarEmail(Integer idUsuario, String email);

    /**
     * Cambiar rol del usuario en ERP Admin.
     * Solo ADMIN_SYS.
     */
    void cambiarRol(Integer idUsuario, Integer idRolNuevo, String updatedBy);

    /**
     * Deshabilitar usuario (soft delete).
     * No puede deshabilitarse a sí mismo.
     */
    void deshabilitar(Integer idUsuario, Integer idUsuarioSolicitante);

    /**
     * Reactivar usuario deshabilitado.
     */
    void reactivar(Integer idUsuario);

    /**
     * Obtener registros de sistema del usuario.
     */
    List<RegistroSistema> obtenerRegistros(Integer idUsuario);

    /**
     * Elimina físicamente el usuario asociado a un personal (si existe),
     * incluyendo sus registros de rol/sistema. No hace nada si el personal
     * no tiene usuario asociado. Se usa cuando el personal cambia de unidad
     * o de departamento — sus accesos previos ya no corresponden.
     *
     * @return true si se eliminó un usuario, false si no tenía ninguno.
     */
    boolean eliminarPorPersonal(Integer idPersonal);
}