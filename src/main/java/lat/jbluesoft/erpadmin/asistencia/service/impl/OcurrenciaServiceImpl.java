package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaCrearDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaEditarDTO;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.repository.OcurrenciaRepository;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * OcurrenciaServiceImpl
 * Implementación de lógica de negocio para ocurrencias
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OcurrenciaServiceImpl implements OcurrenciaService {

    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Z0-9]{2,10}$");
    private static final Pattern PATRON_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final List<String> TIPOS = List.of("DISPONIBLE", "DESCUENTO");
    private static final int MAX_DESCRIPCION = 50;

    private final OcurrenciaRepository ocurrenciaRepository;

    @Override
    public List<Ocurrencia> listarTodas() {
        log.debug("Listando todas las ocurrencias habilitadas");
        return ocurrenciaRepository.findByEnabledTrueOrderByOrdenAsc();
    }

    @Override
    public Optional<Ocurrencia> buscarPorCodigo(String codigo) {
        log.debug("Buscando ocurrencia con código: {}", codigo);
        return ocurrenciaRepository.findByCodigo(codigo);
    }

    @Override
    public Optional<Ocurrencia> obtenerPorId(Integer id) {
        log.debug("Buscando ocurrencia con ID: {}", id);
        return ocurrenciaRepository.findById(id);
    }

    @Override
    public List<Ocurrencia> listarPorTipo(String tipo) {
        log.debug("Listando ocurrencias de tipo: {}", tipo);
        return ocurrenciaRepository.findByTipoAndEnabledTrueOrderByOrdenAsc(tipo);
    }

    @Override
    public Optional<Ocurrencia> obtenerOcurrenciaPorDefecto() {
        log.debug("Obteniendo ocurrencia por defecto (ASI)");
        return ocurrenciaRepository.findByCodigo(Ocurrencia.CODIGO_ASISTENCIA);
    }

    // ========== ADMINISTRACIÓN (ADMIN_SYS) ==========

    @Override
    public List<Ocurrencia> listarParaAdministracion() {
        return ocurrenciaRepository.findAll(Sort.by("orden", "idOcurrencia"));
    }

    @Override
    @Transactional
    public Ocurrencia crear(OcurrenciaCrearDTO dto) {
        String codigo = normalizarCodigo(dto.getCodigo());
        String descripcion = validarDescripcion(dto.getDescripcion());
        String tipo = validarTipo(dto.getTipo());
        String color = validarColor(dto.getColor());
        if (dto.getAlmuerzo() == null) {
            throw new IllegalArgumentException("Indica si la ocurrencia cuenta para el almuerzo.");
        }
        Integer orden = dto.getOrden() != null
                ? validarOrden(dto.getOrden())
                : ocurrenciaRepository.obtenerOrdenMaximo() + 1;

        if (ocurrenciaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Ya existe una ocurrencia con el código " + codigo + ".");
        }

        Ocurrencia ocurrencia = new Ocurrencia();
        ocurrencia.setCodigo(codigo);
        ocurrencia.setDescripcion(descripcion);
        ocurrencia.setTipo(tipo);
        ocurrencia.setAlmuerzo(dto.getAlmuerzo());
        ocurrencia.setColor(color);
        ocurrencia.setOrden(orden);
        ocurrencia.setEnabled(true);

        try {
            // saveAndFlush: la violación de unicidad salta aquí y no al confirmar la transacción
            Ocurrencia guardada = ocurrenciaRepository.saveAndFlush(ocurrencia);
            log.info("Ocurrencia creada: {} ({})", guardada.getCodigo(), guardada.getDescripcion());
            return guardada;
        } catch (DataIntegrityViolationException e) {
            // Respaldo: otro usuario pudo crear el mismo código entre la comprobación y el guardado
            log.warn("Violación de integridad al crear la ocurrencia {}: {}", codigo, e.getMessage());
            throw new IllegalArgumentException("Ya existe una ocurrencia con el código " + codigo + ".");
        }
    }

    @Override
    @Transactional
    public Ocurrencia actualizar(Integer id, OcurrenciaEditarDTO dto) {
        Ocurrencia ocurrencia = ocurrenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la ocurrencia solicitada."));

        String descripcion = validarDescripcion(dto.getDescripcion());
        String color = validarColor(dto.getColor());
        Integer orden = dto.getOrden() != null ? validarOrden(dto.getOrden()) : ocurrencia.getOrden();

        avisarSiIntentaCambiarNoEditables(ocurrencia, dto);

        // Solo estos tres campos; código, tipo, almuerzo y enabled se conservan
        ocurrencia.setDescripcion(descripcion);
        ocurrencia.setColor(color);
        ocurrencia.setOrden(orden);

        Ocurrencia guardada = ocurrenciaRepository.save(ocurrencia);
        log.info("Ocurrencia actualizada: {} ({})", guardada.getCodigo(), guardada.getDescripcion());
        return guardada;
    }

    private void avisarSiIntentaCambiarNoEditables(Ocurrencia actual, OcurrenciaEditarDTO dto) {
        if (dto.getCodigo() != null && !dto.getCodigo().trim().equalsIgnoreCase(actual.getCodigo())) {
            log.warn("Se ignoró un cambio de código en la ocurrencia {}: llegó '{}'", actual.getCodigo(), dto.getCodigo());
        }
        if (dto.getTipo() != null && !dto.getTipo().trim().equals(actual.getTipo())) {
            log.warn("Se ignoró un cambio de tipo en la ocurrencia {}: llegó '{}'", actual.getCodigo(), dto.getTipo());
        }
        if (dto.getAlmuerzo() != null && !Objects.equals(dto.getAlmuerzo(), actual.getAlmuerzo())) {
            log.warn("Se ignoró un cambio de almuerzo en la ocurrencia {}: llegó '{}'", actual.getCodigo(), dto.getAlmuerzo());
        }
        if (dto.getEnabled() != null && !Objects.equals(dto.getEnabled(), actual.getEnabled())) {
            log.warn("Se ignoró un cambio de enabled en la ocurrencia {}: llegó '{}'", actual.getCodigo(), dto.getEnabled());
        }
    }

    private String normalizarCodigo(String codigo) {
        String valor = codigo != null ? codigo.trim().toUpperCase(Locale.ROOT) : "";
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("El código es obligatorio.");
        }
        if (!PATRON_CODIGO.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "El código debe tener de 2 a 10 letras (sin tildes ni ñ) o números, sin espacios.");
        }
        return valor;
    }

    private String validarDescripcion(String descripcion) {
        String valor = descripcion != null ? descripcion.trim() : "";
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }
        if (valor.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException("La descripción no puede superar los 50 caracteres.");
        }
        return valor;
    }

    private String validarTipo(String tipo) {
        String valor = tipo != null ? tipo.trim() : "";
        if (!TIPOS.contains(valor)) {
            throw new IllegalArgumentException("El tipo debe ser DISPONIBLE o DESCUENTO.");
        }
        return valor;
    }

    private String validarColor(String color) {
        String valor = color != null ? color.trim() : "";
        if (!PATRON_COLOR.matcher(valor).matches()) {
            throw new IllegalArgumentException("El color debe tener el formato #RRGGBB, por ejemplo #1F6BE0.");
        }
        return valor.toUpperCase(Locale.ROOT);
    }

    private Integer validarOrden(Integer orden) {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden debe ser un número entero mayor o igual a 1.");
        }
        return orden;
    }
}
