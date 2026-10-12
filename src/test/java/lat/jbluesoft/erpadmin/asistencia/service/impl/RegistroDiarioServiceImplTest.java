package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.dto.AsistenciaRegistroDTO;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.model.RegistroDiario;
import lat.jbluesoft.erpadmin.asistencia.repository.RegistroDiarioRepository;
import lat.jbluesoft.erpadmin.rrhh.model.Nivel;
import lat.jbluesoft.erpadmin.rrhh.model.Personal;
import lat.jbluesoft.erpadmin.rrhh.model.TipoPersonal;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import lat.jbluesoft.erpadmin.rrhh.service.PersonalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la separación en Grupo 1 / Grupo 2 / Ocurrencias
 * que hace prepararRegistroDiario a partir del último registro.
 * Sin Spring ni base de datos: las dependencias son mocks de Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RegistroDiarioServiceImplTest {

    private static final Integer ID_DEPARTAMENTO = 10;
    private static final LocalDate FECHA_ANTERIOR = LocalDate.of(2026, 10, 9);
    private static final LocalDate FECHA_REGISTRO = LocalDate.of(2026, 10, 12);

    @Mock
    private RegistroDiarioRepository registroDiarioRepository;

    @Mock
    private PersonalService personalService;

    @Mock
    private DepartamentoService departamentoService;

    @InjectMocks
    private RegistroDiarioServiceImpl service;

    // ---------- Casos ----------

    @Test
    @DisplayName("Asistió (ASI) con id 7 y Permiso (PER) con id 1: se separa por código, no por id")
    void asistenciaConIdDistintoDeUno_seSeparaPorCodigo() {
        verificarSeparacion(ocurrencia(7, "ASI"), ocurrencia(1, "PER"));
    }

    @Test
    @DisplayName("Caso normal (ASI id 1, PER id 2) da el mismo resultado")
    void casoNormal_asistenciaIdUno_mismoResultado() {
        verificarSeparacion(ocurrencia(1, "ASI"), ocurrencia(2, "PER"));
    }

    @Test
    @DisplayName("Persona que no estaba en el último registro va a su grupo según tipo de personal")
    void personaNueva_sinRegistroAnterior_vaASuGrupo() {
        Personal antiguo = persona("P001", 1);
        Personal nuevoGrupo1 = persona("P002", 1);
        Personal nuevoGrupo2 = persona("P003", 2);

        when(personalService.listarPorDepartamentoConRelaciones(ID_DEPARTAMENTO))
                .thenReturn(List.of(antiguo, nuevoGrupo1, nuevoGrupo2));
        when(registroDiarioRepository.findFechaUltimoRegistroPorDepartamento(ID_DEPARTAMENTO))
                .thenReturn(Optional.of(FECHA_ANTERIOR));
        when(registroDiarioRepository.findUltimoRegistroPorDepartamento(ID_DEPARTAMENTO))
                .thenReturn(List.of(registro(antiguo, ocurrencia(7, "ASI"))));

        AsistenciaRegistroDTO dto = service.prepararRegistroDiario(ID_DEPARTAMENTO, FECHA_REGISTRO);

        assertEquals(List.of(antiguo, nuevoGrupo1), dto.getGrupo1());
        assertEquals(List.of(nuevoGrupo2), dto.getGrupo2());
        assertEquals(List.of(), dto.getOcurrencias());
    }

    @Test
    @DisplayName("Departamento sin ningún registro anterior: todo el efectivo va a su grupo")
    void departamentoSinRegistroAnterior_todosVanASuGrupo() {
        Personal grupo1 = persona("P001", 1);
        Personal grupo2 = persona("P002", 2);

        when(personalService.listarPorDepartamentoConRelaciones(ID_DEPARTAMENTO))
                .thenReturn(List.of(grupo1, grupo2));
        when(registroDiarioRepository.findFechaUltimoRegistroPorDepartamento(ID_DEPARTAMENTO))
                .thenReturn(Optional.empty());

        AsistenciaRegistroDTO dto = service.prepararRegistroDiario(ID_DEPARTAMENTO, FECHA_REGISTRO);

        assertEquals(List.of(grupo1), dto.getGrupo1());
        assertEquals(List.of(grupo2), dto.getGrupo2());
        assertEquals(List.of(), dto.getOcurrencias());
    }

    // ---------- Apoyo ----------

    /**
     * Personal de Grupo 1 y Grupo 2 con Asistió, más una persona con Permiso:
     * los que asistieron van a su grupo y la de Permiso a ocurrencias.
     */
    private void verificarSeparacion(Ocurrencia asistio, Ocurrencia permiso) {
        Personal asistioGrupo1 = persona("P001", 1);
        Personal asistioGrupo2 = persona("P002", 2);
        Personal conPermiso = persona("P003", 1);

        RegistroDiario registroPermiso = registro(conPermiso, permiso);

        when(personalService.listarPorDepartamentoConRelaciones(ID_DEPARTAMENTO))
                .thenReturn(List.of(asistioGrupo1, asistioGrupo2, conPermiso));
        when(registroDiarioRepository.findFechaUltimoRegistroPorDepartamento(ID_DEPARTAMENTO))
                .thenReturn(Optional.of(FECHA_ANTERIOR));
        when(registroDiarioRepository.findUltimoRegistroPorDepartamento(ID_DEPARTAMENTO))
                .thenReturn(List.of(
                        registro(asistioGrupo1, asistio),
                        registro(asistioGrupo2, asistio),
                        registroPermiso));

        AsistenciaRegistroDTO dto = service.prepararRegistroDiario(ID_DEPARTAMENTO, FECHA_REGISTRO);

        assertEquals(List.of(asistioGrupo1), dto.getGrupo1());
        assertEquals(List.of(asistioGrupo2), dto.getGrupo2());
        assertEquals(List.of(registroPermiso), dto.getOcurrencias());
    }

    private static Ocurrencia ocurrencia(Integer id, String codigo) {
        Ocurrencia o = new Ocurrencia();
        o.setIdOcurrencia(id);
        o.setCodigo(codigo);
        o.setDescripcion(codigo);
        return o;
    }

    private static Personal persona(String codigo, Integer idTipoPersonal) {
        TipoPersonal tipo = new TipoPersonal();
        tipo.setIdTipoPersonal(idTipoPersonal);

        Nivel nivel = new Nivel();
        nivel.setTipoPersonal(tipo);

        Personal p = new Personal();
        p.setCodigo(codigo);
        p.setNivel(nivel);
        return p;
    }

    private static RegistroDiario registro(Personal persona, Ocurrencia ocurrencia) {
        RegistroDiario r = new RegistroDiario();
        r.setPersonal(persona);
        r.setCodigo(persona.getCodigo());
        r.setNivel(persona.getNivel());
        r.setOcurrencia(ocurrencia);
        return r;
    }
}
