package lat.jbluesoft.erpadmin.rrhh.service.impl;

import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.asistencia.repository.RegistroDiarioRepository;
import lat.jbluesoft.erpadmin.rrhh.repository.PersonalRepository;
import lat.jbluesoft.erpadmin.iam.service.UsuarioService;
import lat.jbluesoft.erpadmin.rrhh.service.PersonalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PersonalServiceImpl
 * Implementación de lógica de negocio para personal
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PersonalServiceImpl implements PersonalService {

    private final PersonalRepository personalRepository;

    private final RegistroDiarioRepository registroDiarioRepository;

    private final UsuarioService usuarioService;

    @Override
    public List<Personal> listarTodos() {
        log.debug("Listando todo el personal habilitado");
        return personalRepository.findByEnabledTrue();
    }

    @Override
    public List<Personal> listarPorDepartamento(Integer idDepartamento) {
        log.debug("Listando personal de departamento: {}", idDepartamento);
        return personalRepository.findByDepartamento_IdDepartamentoAndEnabledTrue(idDepartamento);
    }

    /**
     * Listar personal con relaciones cargadas (para vistas)
     */

    @Override
    @Transactional(readOnly = true)
    public List<Personal> listarPorDepartamentoConRelaciones(Integer idDepartamento) {
        log.debug("Listando personal de departamento con relaciones ordenado por antigüedad: {}", idDepartamento);
        return personalRepository.findByDepartamentoOrdenado(idDepartamento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personal> listarPorEmpresaOrdenadoAntiguedad(Integer idEmpresa) {
        log.debug("Listando personal de la empresa ordenado por antigüedad global: {}", idEmpresa);
        return personalRepository.findByEmpresaOrdenadoAntiguedad(idEmpresa);
    }

    @Override
    public Optional<Personal> buscarPorCodigo(String codigo) {
        log.debug("Buscando personal con código: {}", codigo);
        return personalRepository.findByCodigo(codigo);
    }

    @Override
    public Optional<Personal> buscarPorId(Integer id) {
        log.debug("Buscando personal con ID: {}", id);
        return personalRepository.findById(id);
    }

    /**
     * Obtiene personal por ID con todas las relaciones cargadas
     * IMPORTANTE: Usar este método cuando se necesite acceder a nivel, especialidad, departamento
     * fuera de una transacción activa
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Personal> obtenerPorId(Integer id) {
        log.debug("Obteniendo personal con ID y relaciones: {}", id);
        Optional<Personal> personalOpt = personalRepository.findById(id);
        
        // Forzar carga de relaciones si existe
        personalOpt.ifPresent(p -> {
            p.getNivel().getDescripcionCorta();
            p.getEspecialidad().getDescripcionCorta();
            p.getDepartamento().getDescripcionCorta();
            // Forzar carga de TipoPersonal
            if (p.getNivel().getTipoPersonal() != null) {
                p.getNivel().getTipoPersonal().getIdTipoPersonal();
            }
        });
        
        return personalOpt;
    }

    @Override
    public long contarPorDepartamento(Integer idDepartamento) {
        log.debug("Contando personal de departamento: {}", idDepartamento);
        return personalRepository.countByDepartamento_IdDepartamentoAndEnabledTrue(idDepartamento);
    }

    @Override
    public List<Personal> buscarPorNombre(String nombre) {
        log.debug("Buscando personal por nombre: {}", nombre);
        return personalRepository.searchByNombre(nombre);
    }

    // =============================================
    // MÉTODOS CRUD - PERSONAL
    // =============================================

    @Override
    @Transactional
    public Personal guardar(Personal personal) {
        log.debug("Guardando nuevo personal código: {}", personal.getCodigo());
        if (personalRepository.findByCodigo(personal.getCodigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe personal con código: " + personal.getCodigo());
        }
        if (personalRepository.findByDni(personal.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya existe personal con DNI: " + personal.getDni());
        }
        return personalRepository.save(personal);
    }

    @Override
    @Transactional
    public Personal actualizar(Personal personal) {
        log.debug("Actualizando personal ID: {}", personal.getIdPersonal());

        Personal existente = personalRepository.findById(personal.getIdPersonal())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Personal no encontrado: " + personal.getIdPersonal()));

        // Validar si está cambiando de departamento
        boolean cambiandoCia = !existente.getDepartamento().getIdDepartamento()
                .equals(personal.getDepartamento().getIdDepartamento());

        if (cambiandoCia) {
            List<RegistroDiario> registrosEnConflicto = registroDiarioRepository
                    .findByPersonal_IdPersonalAndFechaRegistroGreaterThanEqualOrderByFechaRegistroAsc(
                            personal.getIdPersonal(), LocalDate.now());

            if (!registrosEnConflicto.isEmpty()) {
                String fechas = registrosEnConflicto.stream()
                        .map(p -> p.getFechaRegistro().toString())
                        .distinct()
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                throw new IllegalStateException(
                        personal.getFullName() + " tiene un registro en " +
                                existente.getDepartamento().getDescripcionCorta() +
                                " para el/los día(s): " + fechas +
                                ". Elimine ese(esos) registro(s) antes de cambiar de departamento.");
            }
        }

        Personal actualizado = personalRepository.save(personal);

        if (cambiandoCia) {
            usuarioService.eliminarPorPersonal(personal.getIdPersonal());
        }

        return actualizado;
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        log.debug("Eliminando (soft delete) personal ID: {}", id);
        Personal personal = personalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado: " + id));
        personal.setEnabled(false);
        personalRepository.save(personal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personal> buscarPorCodigoONombre(String query) {
        log.debug("Buscando personal por código o nombre: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Intentar primero por código exacto
        Optional<Personal> porCodigo = personalRepository.findByCodigoConRelaciones(query.trim());

        if (porCodigo.isPresent()) {
            return List.of(porCodigo.get());
        }

        // Si no, buscar por nombre parcial (JOIN FETCH + ORDER BY incluidos en la query)
        return personalRepository.searchByNombre(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personal> buscarPorCodigoONombreEnEmpresa(String query, Integer idEmpresa) {
        log.debug("Buscando personal por código o nombre en empresa {}: {}", idEmpresa, query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        Optional<Personal> porCodigo = personalRepository.findByCodigoConRelacionesEnEmpresa(query.trim(), idEmpresa);

        if (porCodigo.isPresent()) {
            return List.of(porCodigo.get());
        }

        return personalRepository.searchByNombreEnEmpresa(query.trim(), idEmpresa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personal> buscarEnSinEmpresa(String query) {
        log.debug("Buscando personal en SIN_EMPRESA: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // Intentar primero por código exacto
        Optional<Personal> porCodigo = personalRepository.findByCodigoEnSinEmpresa(query.trim());
        if (porCodigo.isPresent()) {
            return List.of(porCodigo.get());
        }

        // Si no, buscar por nombre parcial
        return personalRepository.searchByNombreEnSinEmpresa(query.trim());
    }

    @Override
    @Transactional
    public Personal moverASinEmpresa(Integer idPersonal, Integer idDepartamentoSinEmpresa) {
        log.debug("Moviendo personal ID: {} a SIN_EMPRESA", idPersonal);

        Personal personal = personalRepository.findById(idPersonal)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Personal no encontrado: " + idPersonal));

        // Validar que no tenga registro desde hoy en adelante
        List<RegistroDiario> registrosEnConflicto = registroDiarioRepository
                .findByPersonal_IdPersonalAndFechaRegistroGreaterThanEqualOrderByFechaRegistroAsc(
                        idPersonal, LocalDate.now());

        if (!registrosEnConflicto.isEmpty()) {
            String departamentoEnRegistro = registrosEnConflicto.get(0).getDepartamento().getDescripcionCorta();
            String fechas = registrosEnConflicto.stream()
                    .map(p -> p.getFechaRegistro().toString())
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw new IllegalStateException(
                    personal.getFullName() + " tiene un registro en " +
                            departamentoEnRegistro + " para el/los día(s): " + fechas +
                            ". Elimine ese(esos) registro(s) antes de mover al personal.");
        }

        Departamento sinDepartamento = new Departamento();
        sinDepartamento.setIdDepartamento(idDepartamentoSinEmpresa);
        personal.setDepartamento(sinDepartamento);

        Personal actualizado = personalRepository.save(personal);

        usuarioService.eliminarPorPersonal(idPersonal);

        return actualizado;
    }

    @Override
    public Optional<Personal> buscarPorDni(String dni) {
        log.debug("Buscando personal con DNI: {}", dni);
        return personalRepository.findByDni(dni);
    }

}
