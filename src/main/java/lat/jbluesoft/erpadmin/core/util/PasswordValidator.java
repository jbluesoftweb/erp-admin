package lat.jbluesoft.erpadmin.core.util;

import java.util.regex.Pattern;

/**
 * Valida la complejidad mínima de una contraseña nueva.
 * Reglas: mínimo 8 caracteres, al menos 1 número, al menos 1 carácter
 * especial (no alfanumérico).
 */
public class PasswordValidator {

    private static final Pattern TIENE_NUMERO = Pattern.compile(".*\\d.*");
    private static final Pattern TIENE_ESPECIAL = Pattern.compile(".*[^a-zA-Z0-9].*");

    private PasswordValidator() {
    }

    /**
     * Lanza IllegalArgumentException con un mensaje claro si la contraseña
     * no cumple la política mínima. No hace nada si es válida.
     */
    public static void validar(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres.");
        }
        if (!TIENE_NUMERO.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "La contraseña debe incluir al menos un número.");
        }
        if (!TIENE_ESPECIAL.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "La contraseña debe incluir al menos un carácter especial "
                    + "(por ejemplo: ! @ # $ % & *).");
        }
    }
}
