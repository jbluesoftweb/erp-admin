package lat.jbluesoft.erpadmin.asistencia.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Cálculo de días hábiles (lunes a viernes) para las reglas de registro
 * de registros diarios. Clase estática sin dependencias de Spring, para que
 * pueda ser usada tanto por RegistroDiarioService como por PersonalService
 * sin crear una dependencia circular entre ambos.
 */
public class DiaHabilUtil {

    private DiaHabilUtil() {
    }

    public static LocalDate obtenerDiaHabilAnterior(LocalDate fecha) {
        LocalDate anterior = fecha.minusDays(1);
        while (anterior.getDayOfWeek() == DayOfWeek.SATURDAY
                || anterior.getDayOfWeek() == DayOfWeek.SUNDAY) {
            anterior = anterior.minusDays(1);
        }
        return anterior;
    }

    public static LocalDate obtenerDiaHabilSiguiente(LocalDate fecha) {
        LocalDate siguiente = fecha.plusDays(1);
        while (siguiente.getDayOfWeek() == DayOfWeek.SATURDAY
                || siguiente.getDayOfWeek() == DayOfWeek.SUNDAY) {
            siguiente = siguiente.plusDays(1);
        }
        return siguiente;
    }
}
