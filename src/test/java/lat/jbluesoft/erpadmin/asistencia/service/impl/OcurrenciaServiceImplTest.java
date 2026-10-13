package lat.jbluesoft.erpadmin.asistencia.service.impl;

import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaCrearDTO;
import lat.jbluesoft.erpadmin.asistencia.dto.OcurrenciaEditarDTO;
import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.repository.OcurrenciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de alta y edición de ocurrencias.
 * Sin Spring ni base de datos: el repositorio es un mock de Mockito.
 */
@ExtendWith(MockitoExtension.class)
class OcurrenciaServiceImplTest {

    @Mock
    private OcurrenciaRepository ocurrenciaRepository;

    @InjectMocks
    private OcurrenciaServiceImpl service;

    // ---------- Crear ----------

    @Test
    @DisplayName("Crear válido: normaliza código y color a mayúsculas, recorta la descripción y queda habilitada")
    void crear_valido() {
        when(ocurrenciaRepository.existsByCodigoIgnoreCase("VAC")).thenReturn(false);
        when(ocurrenciaRepository.saveAndFlush(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Ocurrencia creada = service.crear(dtoCrear(" vac ", "  Vacaciones  ", "DESCUENTO", false, "#1f6be0", 4));

        assertEquals("VAC", creada.getCodigo());
        assertEquals("Vacaciones", creada.getDescripcion());
        assertEquals("DESCUENTO", creada.getTipo());
        assertEquals(false, creada.getAlmuerzo());
        assertEquals("#1F6BE0", creada.getColor());
        assertEquals(4, creada.getOrden());
        assertEquals(true, creada.getEnabled());
    }

    @Test
    @DisplayName("Crear con código duplicado en otra capitalización se rechaza")
    void crear_codigoDuplicadoOtraCapitalizacion() {
        when(ocurrenciaRepository.existsByCodigoIgnoreCase("PER")).thenReturn(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("per", "Permiso", "DESCUENTO", false, "#FFC107", 2)));

        assertTrue(e.getMessage().contains("Ya existe"));
        verify(ocurrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear: la violación de unicidad de la base de datos se traduce a un mensaje claro")
    void crear_violacionUnicidadEnBase() {
        when(ocurrenciaRepository.existsByCodigoIgnoreCase("NUE")).thenReturn(false);
        when(ocurrenciaRepository.saveAndFlush(any(Ocurrencia.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key ocurrencia_codigo_key"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("NUE", "Nueva", "DESCUENTO", false, "#123456", 5)));

        assertEquals("Ya existe una ocurrencia con el código NUE.", e.getMessage());
    }

    @Test
    @DisplayName("Crear con caracteres inválidos en el código se rechaza")
    void crear_codigoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("A-1", "Algo", "DESCUENTO", false, "#123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("A", "Algo", "DESCUENTO", false, "#123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("ÑAÑ", "Algo", "DESCUENTO", false, "#123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("ABCDEFGHIJK", "Algo", "DESCUENTO", false, "#123456", 1)));
        verify(ocurrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con tipo distinto de DISPONIBLE/DESCUENTO se rechaza")
    void crear_tipoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "Otra", "OTRO", false, "#123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "Otra", "descuento", false, "#123456", 1)));
        verify(ocurrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con color inválido se rechaza")
    void crear_colorInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "Otra", "DESCUENTO", false, "123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "Otra", "DESCUENTO", false, "#12345G", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "Otra", "DESCUENTO", false, "#FFF", 1)));
        verify(ocurrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con descripción vacía o de más de 50 caracteres se rechaza")
    void crear_descripcionInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "   ", "DESCUENTO", false, "#123456", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("OTR", "x".repeat(51), "DESCUENTO", false, "#123456", 1)));
        verify(ocurrenciaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear con orden vacío asigna el máximo existente + 1")
    void crear_ordenVacioAsignaMaximoMasUno() {
        when(ocurrenciaRepository.existsByCodigoIgnoreCase("LIC")).thenReturn(false);
        when(ocurrenciaRepository.obtenerOrdenMaximo()).thenReturn(7);
        when(ocurrenciaRepository.saveAndFlush(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Ocurrencia creada = service.crear(dtoCrear("LIC", "Licencia", "DESCUENTO", false, "#123456", null));

        assertEquals(8, creada.getOrden());
    }

    @Test
    @DisplayName("Crear con orden menor que 1 se rechaza")
    void crear_ordenMenorQueUno() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(dtoCrear("LIC", "Licencia", "DESCUENTO", false, "#123456", 0)));
    }

    // ---------- Editar ----------

    @Test
    @DisplayName("Editar cambia descripción, color y orden")
    void editar_cambiaDescripcionColorOrden() {
        Ocurrencia per = ocurrencia(2, "PER", "Permiso", "DESCUENTO", false, "#ffc107", 2);
        when(ocurrenciaRepository.findById(2)).thenReturn(Optional.of(per));
        when(ocurrenciaRepository.save(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        OcurrenciaEditarDTO dto = dtoEditar(" Permiso personal ", "#00aa00", 9);
        Ocurrencia guardada = service.actualizar(2, dto);

        assertEquals("Permiso personal", guardada.getDescripcion());
        assertEquals("#00AA00", guardada.getColor());
        assertEquals(9, guardada.getOrden());
    }

    @Test
    @DisplayName("Editar con orden vacío conserva el orden actual")
    void editar_ordenVacioConservaActual() {
        Ocurrencia per = ocurrencia(2, "PER", "Permiso", "DESCUENTO", false, "#FFC107", 2);
        when(ocurrenciaRepository.findById(2)).thenReturn(Optional.of(per));
        when(ocurrenciaRepository.save(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Ocurrencia guardada = service.actualizar(2, dtoEditar("Permiso", "#FFC107", null));

        assertEquals(2, guardada.getOrden());
    }

    @Test
    @DisplayName("Editar valida descripción y color igual que al crear")
    void editar_validaCampos() {
        Ocurrencia per = ocurrencia(2, "PER", "Permiso", "DESCUENTO", false, "#FFC107", 2);
        when(ocurrenciaRepository.findById(2)).thenReturn(Optional.of(per));

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(2, dtoEditar("", "#FFC107", 2)));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(2, dtoEditar("Permiso", "rojo", 2)));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(2, dtoEditar("Permiso", "#FFC107", 0)));
        verify(ocurrenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar una ocurrencia normal ignora código, tipo, almuerzo y enabled distintos")
    void editar_ignoraNoEditables_ocurrenciaNormal() {
        Ocurrencia per = ocurrencia(2, "PER", "Permiso", "DESCUENTO", false, "#FFC107", 2);
        when(ocurrenciaRepository.findById(2)).thenReturn(Optional.of(per));
        when(ocurrenciaRepository.save(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        OcurrenciaEditarDTO dto = dtoEditar("Permiso", "#FFC107", 2);
        dto.setCodigo("XYZ");
        dto.setTipo("DISPONIBLE");
        dto.setAlmuerzo(true);
        dto.setEnabled(false);

        Ocurrencia guardada = service.actualizar(2, dto);

        assertEquals("PER", guardada.getCodigo());
        assertEquals("DESCUENTO", guardada.getTipo());
        assertEquals(false, guardada.getAlmuerzo());
        assertEquals(true, guardada.getEnabled());
    }

    @Test
    @DisplayName("Editar ASI ignora código, tipo, almuerzo y enabled distintos")
    void editar_ignoraNoEditables_asi() {
        Ocurrencia asi = ocurrencia(1, "ASI", "Asistió", "DISPONIBLE", true, "#28A745", 1);
        when(ocurrenciaRepository.findById(1)).thenReturn(Optional.of(asi));
        when(ocurrenciaRepository.save(any(Ocurrencia.class))).thenAnswer(inv -> inv.getArgument(0));

        OcurrenciaEditarDTO dto = dtoEditar("Presente", "#22AA44", 1);
        dto.setCodigo("PRE");
        dto.setTipo("DESCUENTO");
        dto.setAlmuerzo(false);
        dto.setEnabled(false);

        Ocurrencia guardada = service.actualizar(1, dto);

        assertEquals("ASI", guardada.getCodigo());
        assertEquals("DISPONIBLE", guardada.getTipo());
        assertEquals(true, guardada.getAlmuerzo());
        assertEquals(true, guardada.getEnabled());
        assertEquals("Presente", guardada.getDescripcion());
        assertEquals("#22AA44", guardada.getColor());
    }

    @Test
    @DisplayName("Editar una ocurrencia inexistente se rechaza")
    void editar_inexistente() {
        when(ocurrenciaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(99, dtoEditar("X", "#123456", 1)));
    }

    // ---------- Apoyo ----------

    private static OcurrenciaCrearDTO dtoCrear(String codigo, String descripcion, String tipo,
                                               Boolean almuerzo, String color, Integer orden) {
        OcurrenciaCrearDTO dto = new OcurrenciaCrearDTO();
        dto.setCodigo(codigo);
        dto.setDescripcion(descripcion);
        dto.setTipo(tipo);
        dto.setAlmuerzo(almuerzo);
        dto.setColor(color);
        dto.setOrden(orden);
        return dto;
    }

    private static OcurrenciaEditarDTO dtoEditar(String descripcion, String color, Integer orden) {
        OcurrenciaEditarDTO dto = new OcurrenciaEditarDTO();
        dto.setDescripcion(descripcion);
        dto.setColor(color);
        dto.setOrden(orden);
        return dto;
    }

    private static Ocurrencia ocurrencia(Integer id, String codigo, String descripcion, String tipo,
                                         boolean almuerzo, String color, Integer orden) {
        return new Ocurrencia(id, codigo, descripcion, tipo, color, orden, true, almuerzo);
    }
}
