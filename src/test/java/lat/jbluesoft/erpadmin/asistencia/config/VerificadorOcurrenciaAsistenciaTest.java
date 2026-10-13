package lat.jbluesoft.erpadmin.asistencia.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.repository.OcurrenciaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Comprobación de arranque de la ocurrencia ASI: registra un error si falta
 * o está mal configurada, y nunca lanza excepción.
 */
@ExtendWith(MockitoExtension.class)
class VerificadorOcurrenciaAsistenciaTest {

    @Mock
    private OcurrenciaRepository ocurrenciaRepository;

    @InjectMocks
    private VerificadorOcurrenciaAsistencia verificador;

    private final Logger logger = (Logger) LoggerFactory.getLogger(VerificadorOcurrenciaAsistencia.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void capturarLog() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("ASI correcta: no registra error")
    void asiCorrecta_sinError() {
        when(ocurrenciaRepository.findByCodigo("ASI"))
                .thenReturn(Optional.of(asi(true, "DISPONIBLE", true)));

        assertDoesNotThrow(() -> verificador.run(new DefaultApplicationArguments()));

        assertTrue(errores().isEmpty());
    }

    @Test
    @DisplayName("Sin ASI (base vacía): registra el error y no lanza excepción")
    void sinAsi_registraError() {
        when(ocurrenciaRepository.findByCodigo("ASI")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> verificador.run(new DefaultApplicationArguments()));

        List<String> errores = errores();
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("No existe la ocurrencia con código ASI"));
        assertTrue(errores.get(0).contains("no funciona correctamente"));
    }

    @Test
    @DisplayName("ASI mal configurada: registra el error con cada problema y no lanza excepción")
    void asiMalConfigurada_registraError() {
        when(ocurrenciaRepository.findByCodigo("ASI"))
                .thenReturn(Optional.of(asi(false, "DESCUENTO", false)));

        assertDoesNotThrow(() -> verificador.run(new DefaultApplicationArguments()));

        List<String> errores = errores();
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("no está habilitada"));
        assertTrue(errores.get(0).contains("debe ser DISPONIBLE"));
        assertTrue(errores.get(0).contains("no tiene almuerzo activado"));
    }

    @Test
    @DisplayName("Fallo al consultar la base: registra el error y no lanza excepción")
    void falloDeBase_registraError() {
        when(ocurrenciaRepository.findByCodigo("ASI"))
                .thenThrow(new DataAccessResourceFailureException("relation asistencia.ocurrencia does not exist"));

        assertDoesNotThrow(() -> verificador.run(new DefaultApplicationArguments()));

        assertEquals(1, errores().size());
    }

    private List<String> errores() {
        return appender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static Ocurrencia asi(boolean enabled, String tipo, boolean almuerzo) {
        return new Ocurrencia(1, "ASI", "Asistió", tipo, "#28A745", 1, enabled, almuerzo);
    }
}
