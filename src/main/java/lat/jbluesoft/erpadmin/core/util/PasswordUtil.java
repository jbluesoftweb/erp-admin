package lat.jbluesoft.erpadmin.core.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * PasswordUtil
 * Utilidad para generar passwords BCrypt
 * Usar para crear/actualizar passwords en la BD
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Generar hash BCrypt de un password
     */
    public static String encode(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    /**
     * Verificar si un password coincide con un hash
     */
    public static boolean matches(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Main para generar passwords desde consola
     * Ejemplo: java PasswordUtil "Admin123"
     */
    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Uso: java PasswordUtil <password>");
//            System.out.println("Ejemplo: java PasswordUtil Admin123");
//            return;
//        }

//        String plainPassword = args[0];
        String hashedPassword = encode("Master123");

        System.out.println("==================================================");
//        System.out.println("PASSWORD ORIGINAL: " + plainPassword);
        System.out.println("PASSWORD HASHEADO: " + hashedPassword);
        System.out.println("==================================================");
        System.out.println("\nSQL UPDATE:");
        System.out.println("UPDATE usuario SET password_hash = '" + hashedPassword + "' WHERE codigo = 'TU_CODIGO';");
        System.out.println("==================================================");
    }
}
