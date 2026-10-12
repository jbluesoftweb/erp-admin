package lat.jbluesoft.erpadmin.asistencia.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de Ocurrencia.esAsistencia(): la asistencia se
 * identifica por el código ASI, nunca por el id.
 */
class OcurrenciaTest {

    private static Ocurrencia ocurrencia(Integer id, String codigo) {
        Ocurrencia o = new Ocurrencia();
        o.setIdOcurrencia(id);
        o.setCodigo(codigo);
        return o;
    }

    @Test
    @DisplayName("El código ASI es asistencia")
    void codigoAsi_esAsistencia() {
        assertTrue(ocurrencia(1, "ASI").esAsistencia());
    }

    @Test
    @DisplayName("El código PER no es asistencia")
    void codigoPer_noEsAsistencia() {
        assertFalse(ocurrencia(2, "PER").esAsistencia());
    }

    @Test
    @DisplayName("Sin código no es asistencia")
    void codigoNulo_noEsAsistencia() {
        assertFalse(ocurrencia(1, null).esAsistencia());
    }

    @Test
    @DisplayName("Código ASI con id 99 sigue siendo asistencia")
    void codigoAsiConIdDistintoDeUno_esAsistencia() {
        assertTrue(ocurrencia(99, "ASI").esAsistencia());
    }

    @Test
    @DisplayName("Código PER con id 1 no es asistencia")
    void codigoPerConIdUno_noEsAsistencia() {
        assertFalse(ocurrencia(1, "PER").esAsistencia());
    }
}
