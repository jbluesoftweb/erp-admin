package lat.jbluesoft.erpadmin.rrhh.service.impl;

import lat.jbluesoft.erpadmin.rrhh.model.Especialidad;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import lat.jbluesoft.erpadmin.rrhh.repository.EspecialidadRepository;
import lat.jbluesoft.erpadmin.rrhh.repository.TipoPersonalRepository;
import lat.jbluesoft.erpadmin.rrhh.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * EspecialidadServiceImpl
 * Implementación de lógica de negocio para especialidades
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final TipoPersonalRepository tipoPersonalRepository;


    @Override
    public List<Especialidad> listarPorTipoPersonal(Integer idTipoPersonal) {
        log.debug("Listando especialidades por tipo de personal: {}", idTipoPersonal);
        TipoPersonal tipoPersonal = tipoPersonalRepository.findById(idTipoPersonal)
                .orElseThrow(() -> new IllegalArgumentException("TipoPersonal no encontrado: " + idTipoPersonal));
        return especialidadRepository.findByTipoPersonal(tipoPersonal);
    }

    @Override
    public Optional<Especialidad> buscarPorId(Integer id) {
        log.debug("Buscando especialidad con ID: {}", id);
        return especialidadRepository.findById(id);
    }

    @Override
    public List<Especialidad> listarTodas() {
        log.debug("Listando todas las especialidades con tipoPersonal");
        return especialidadRepository.findAllConTipoPersonal();
    }
}