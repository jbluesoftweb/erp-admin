package lat.jbluesoft.erpadmin.core.util;

/**
 * Mensajes de error que se muestran al usuario.
 * El detalle técnico de una excepción inesperada va solo al log, nunca a la pantalla.
 */
public final class MensajesError {

    public static final String GENERICO =
            "Ocurrió un error inesperado. Inténtalo de nuevo; si el problema continúa, avisa al administrador del sistema.";

    private MensajesError() {
    }
}
