package lat.jbluesoft.erpadmin.asistencia.config;

import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.repository.OcurrenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Al arrancar, comprueba que la ocurrencia ASI (Asistió) exista y esté bien
 * configurada: habilitada, tipo DISPONIBLE y con almuerzo.
 * Si algo falla solo registra un error: nunca impide el arranque.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificadorOcurrenciaAsistencia implements ApplicationRunner {

    private final OcurrenciaRepository ocurrenciaRepository;

    @Override
    public void run(ApplicationArguments args) {
        verificar();
    }

    /**
     * Devuelve true si la ocurrencia ASI está bien configurada.
     */
    public boolean verificar() {
        String codigo = Ocurrencia.CODIGO_ASISTENCIA;
        try {
            Optional<Ocurrencia> encontrada = ocurrenciaRepository.findByCodigo(codigo);
            if (encontrada.isEmpty()) {
                log.error("No existe la ocurrencia con código {} (Asistió) en asistencia.ocurrencia. "
                        + "Sin ella el registro de asistencia no funciona correctamente. "
                        + "Créala con el código {}, tipo DISPONIBLE y almuerzo activado.", codigo, codigo);
                return false;
            }

            Ocurrencia asi = encontrada.get();
            List<String> problemas = new ArrayList<>();
            if (!Boolean.TRUE.equals(asi.getEnabled())) {
                problemas.add("no está habilitada (enabled debe ser true)");
            }
            if (!"DISPONIBLE".equals(asi.getTipo())) {
                problemas.add("su tipo es '" + asi.getTipo() + "' y debe ser DISPONIBLE");
            }
            if (!Boolean.TRUE.equals(asi.getAlmuerzo())) {
                problemas.add("no tiene almuerzo activado (almuerzo debe ser true)");
            }

            if (!problemas.isEmpty()) {
                log.error("La ocurrencia {} (Asistió) está mal configurada: {}. "
                        + "Sin ella el registro de asistencia no funciona correctamente.",
                        codigo, String.join("; ", problemas));
                return false;
            }

            log.debug("Ocurrencia {} verificada correctamente", codigo);
            return true;

        } catch (Exception e) {
            log.error("No se pudo verificar la ocurrencia {} (Asistió) al arrancar. "
                    + "Sin ella el registro de asistencia no funciona correctamente.", codigo, e);
            return false;
        }
    }
}
