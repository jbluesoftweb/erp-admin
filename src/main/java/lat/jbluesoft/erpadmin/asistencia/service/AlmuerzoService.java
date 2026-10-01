package lat.jbluesoft.erpadmin.asistencia.service;

import lat.jbluesoft.erpadmin.asistencia.dto.AlmuerzoDTO;

import java.time.LocalDate;

/**
 * Service: AlmuerzoService
 * Lógica de negocio para el módulo de Almuerzo
 * Maneja la lista de personal que recibe almuerzo
 */
public interface AlmuerzoService {

    /**
     * Obtiene el almuerzo de un departamento para una fecha específica
     * SOLO incluye personal PRESENTE (ocurrencia = ASISTIÓ)
     *
     * @param fecha Fecha del almuerzo
     * @param idDepartamento ID del departamento
     * @return AlmuerzoDTO con lista de presentes y totales
     */
    AlmuerzoDTO obtenerAlmuerzoPorDepartamento(LocalDate fecha, Integer idDepartamento);
}