package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.repository.OcurrenciaRepository;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * OcurrenciaServiceImpl
 * Implementación de lógica de negocio para ocurrencias
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OcurrenciaServiceImpl implements OcurrenciaService {

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
        return ocurrenciaRepository.findByCodigo("ASI");
    }
}
