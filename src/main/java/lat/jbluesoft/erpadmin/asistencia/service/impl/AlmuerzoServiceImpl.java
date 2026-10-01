package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.dto.PersonalAlmuerzoDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.AlmuerzoDTO;
import lat.jbluesoft.erpadmin.rrhh.model.Departamento;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lat.jbluesoft.erpadmin.asistencia.repository.RegistroDiarioRepository;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.asistencia.service.AlmuerzoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ServiceImpl: AlmuerzoServiceImpl
 * Implementación de la lógica de negocio para Almuerzo
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlmuerzoServiceImpl implements AlmuerzoService {

    private final RegistroDiarioRepository registroDiarioRepository;
    private final DepartamentoService departamentoService;

    @Override
    public AlmuerzoDTO obtenerAlmuerzoPorDepartamento(LocalDate fecha, Integer idDepartamento) {
        log.info("Generando almuerzo para departamento ID: {} en fecha: {}", idDepartamento, fecha);

        try {
            // Obtener datos del departamento
            Departamento departamento = departamentoService.buscarPorId(idDepartamento)
                    .orElseThrow(() -> new RuntimeException("Departamento no encontrado: " + idDepartamento));

            // Crear DTO
            AlmuerzoDTO almuerzo = new AlmuerzoDTO();
            almuerzo.setFecha(fecha);
            almuerzo.setDepartamentoDescripcion(departamento.getDescripcionCorta());

            // Obtener descripción de la empresa
            if (departamento.getEmpresa() != null) {
                almuerzo.setEmpresaDescripcion(
                        departamento.getEmpresa().getDescripcionLarga() != null
                                ? departamento.getEmpresa().getDescripcionLarga()
                                : departamento.getEmpresa().getDescripcionCorta()
                );
            } else {
                almuerzo.setEmpresaDescripcion("Sin Unidad");
            }

            // Obtener personal PRESENTE (ocurrencia = ASISTIÓ)
            List<RegistroDiario> registrosPresentes = registroDiarioRepository
                    .findPersonalPresentePorDepartamentoYFecha(fecha, idDepartamento);

            if (registrosPresentes.isEmpty()) {
                log.warn("No hay personal presente registrado para departamento {} en fecha {}",
                        idDepartamento, fecha);
                almuerzo.setGrupo1(new ArrayList<>());
                almuerzo.setGrupo2(new ArrayList<>());
                almuerzo.calcularTotales();
                return almuerzo;
            }

            // UNA SOLA LISTA - ordenada por antigüedad (ya viene ordenado del query)
            List<PersonalAlmuerzoDTO> listaCompleta = new ArrayList<>();

            AtomicInteger contador = new AtomicInteger(1);

            for (RegistroDiario registro : registrosPresentes) {
                PersonalAlmuerzoDTO personal = new PersonalAlmuerzoDTO();
                personal.setNumero(contador.getAndIncrement());
                personal.setNivel(registro.getNivelSnapshot());
                personal.setEspecialidad(registro.getEspecialidadSnapshot());
                personal.setNombreCompleto(registro.getFullName());
                personal.setCodigo(registro.getCodigo());

                listaCompleta.add(personal);
            }

            // Guardar en Grupo 1 (para mantener compatibilidad con el DTO)
            // pero es UNA SOLA LISTA
            almuerzo.setGrupo1(listaCompleta);
            almuerzo.setGrupo2(new ArrayList<>()); // vacío
            almuerzo.calcularTotales();

            log.info("Almuerzo generado - Total: {}",
                    almuerzo.getTotalGeneral());

            return almuerzo;

        } catch (Exception e) {
            log.error("Error al generar almuerzo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar almuerzo: " + e.getMessage(), e);
        }
    }
}