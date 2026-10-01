package lat.jbluesoft.erpadmin.rrhh.service.impl;

import lat.jbluesoft.erpadmin.rrhh.model.Nivel;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import lat.jbluesoft.erpadmin.rrhh.repository.NivelRepository;
import lat.jbluesoft.erpadmin.rrhh.repository.TipoPersonalRepository;
import lat.jbluesoft.erpadmin.rrhh.service.NivelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * NivelServiceImpl
 * Implementación de lógica de negocio para niveles
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NivelServiceImpl implements NivelService {

    private final NivelRepository nivelRepository;
    private final TipoPersonalRepository tipoPersonalRepository;



    @Override
    public List<Nivel> listarPorTipoPersonal(Integer idTipoPersonal) {
        log.debug("Listando niveles por tipo de personal: {}", idTipoPersonal);
        TipoPersonal tipoPersonal = tipoPersonalRepository.findById(idTipoPersonal)
                .orElseThrow(() -> new IllegalArgumentException("TipoPersonal no encontrado: " + idTipoPersonal));
        return nivelRepository.findByTipoPersonalOrderByOrdenAsc(tipoPersonal);
    }

    @Override
    public Optional<Nivel> buscarPorId(Integer id) {
        log.debug("Buscando nivel con ID: {}", id);
        return nivelRepository.findById(id);
    }

    @Override
    public List<TipoPersonal> listarTipoPersonales() {
        log.debug("Listando tipos de persona");
        return tipoPersonalRepository.findAll();
    }
    @Override
    public List<Nivel> listarTodos() {
        log.debug("Listando todos los niveles con tipoPersonal");
        return nivelRepository.findAllConTipoPersonal();
    }
}