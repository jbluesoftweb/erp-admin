package lat.jbluesoft.erpadmin.rrhh.service.impl;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.rrhh.repository.DepartamentoRepository;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DepartamentoServiceImpl
 * Implementación de lógica de negocio para departamentos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartamentoServiceImpl implements DepartamentoService {

    private final DepartamentoRepository departamentoRepository;

    @Override
    public List<Departamento> listarTodas() {
        log.debug("Listando todos los departamentos habilitados");
        return departamentoRepository.findByEnabledTrue();
    }

    @Override
    public Optional<Departamento> buscarPorId(Integer id) {
        log.debug("Buscando departamento con ID: {}", id);
        return departamentoRepository.findById(id);
    }

    @Override
    public Optional<Departamento> buscarPorDescripcionCorta(String descripcionCorta) {
        log.debug("Buscando departamento con descripción: {}", descripcionCorta);
        return departamentoRepository.findByDescripcionCorta(descripcionCorta);
    }

    @Override
    public List<Departamento> listarPorEmpresa(Integer idEmpresa) {
        log.debug("Listando departamentos de la empresa ID: {}", idEmpresa);
        return departamentoRepository.findByEmpresa_IdEmpresaAndEnabledTrueOrderByIdDepartamentoAsc(idEmpresa);
    }
}