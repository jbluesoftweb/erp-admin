package lat.jbluesoft.erpadmin.asistencia.controller;

import lat.jbluesoft.erpadmin.asistencia.model.Ocurrencia;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lat.jbluesoft.erpadmin.core.config.SecurityConfig;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetailsService;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lat.jbluesoft.erpadmin.core.util.MensajesError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Seguridad y manejo de errores de OcurrenciaAdminController (/admin/ocurrencias):
 * solo ADMIN_SYS, y los errores inesperados solo muestran MensajesError.GENERICO.
 */
@WebMvcTest(OcurrenciaAdminController.class)
@Import(SecurityConfig.class)
class OcurrenciaAdminControllerTest {

    private static final String SECRETO = "detalle técnico secreto: tabla iam.usuario";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcurrenciaService ocurrenciaService;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ---------- Acceso ----------

    @Test
    void listar_sinAutenticar_redirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void listar_conAdminSys_devuelve200YMuestraDatos() throws Exception {
        when(ocurrenciaService.listarParaAdministracion()).thenReturn(List.of(
                new Ocurrencia(1, "ASI", "Asistió", "DISPONIBLE", "#28a745", 1, true, true),
                new Ocurrencia(2, "PER", "Permiso", "DESCUENTO", "#ffc107", 2, true, false)));

        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ocurrencias"))
                .andExpect(content().string(containsString("Base del registro de asistencia")))
                .andExpect(content().string(containsString("#28A745")))
                .andExpect(content().string(containsString("/admin/ocurrencias/2/editar")))
                .andExpect(content().string(not(containsString("enabled"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void listar_conAdminApp_devuelve403() throws Exception {
        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_DPTO")
    void listar_conAdminDpto_devuelve403() throws Exception {
        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void crear_conAdminApp_devuelve403YNoCrea() throws Exception {
        mockMvc.perform(post("/admin/ocurrencias/crear").with(csrf())
                        .param("codigo", "VAC").param("descripcion", "Vacaciones")
                        .param("tipo", "DESCUENTO").param("almuerzo", "false")
                        .param("color", "#123456"))
                .andExpect(status().isForbidden());
        verify(ocurrenciaService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_DPTO")
    void actualizar_conAdminDpto_devuelve403YNoActualiza() throws Exception {
        mockMvc.perform(post("/admin/ocurrencias/2/actualizar").with(csrf())
                        .param("descripcion", "X").param("color", "#123456"))
                .andExpect(status().isForbidden());
        verify(ocurrenciaService, never()).actualizar(any(), any());
    }

    // ---------- Formularios ----------

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void nueva_conAdminSys_devuelve200ConAviso() throws Exception {
        mockMvc.perform(get("/admin/ocurrencias/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ocurrencia-form"))
                .andExpect(content().string(containsString(
                        "El código, el tipo y la marca de almuerzo no se podrán cambiar después de crear la ocurrencia.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void editar_asi_muestraDatosFijosYNotaSinCamposNoEditables() throws Exception {
        when(ocurrenciaService.obtenerPorId(1)).thenReturn(Optional.of(
                new Ocurrencia(1, "ASI", "Asistió", "DISPONIBLE", "#28a745", 1, true, true)));

        mockMvc.perform(get("/admin/ocurrencias/1/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Esta ocurrencia es la base del registro de asistencia y no se puede desactivar ni eliminar.")))
                .andExpect(content().string(containsString(
                        "Estos datos definen cómo se cuenta la ocurrencia en los reportes y no se pueden cambiar.")))
                .andExpect(content().string(not(containsString("name=\"codigo\""))))
                .andExpect(content().string(not(containsString("name=\"tipo\""))))
                .andExpect(content().string(not(containsString("name=\"almuerzo\""))))
                .andExpect(content().string(not(containsString("name=\"enabled\""))));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void crear_conErroresDeValidacion_vuelveAlFormularioConservandoLoEscrito() throws Exception {
        mockMvc.perform(post("/admin/ocurrencias/crear").with(csrf())
                        .param("codigo", "A-1").param("descripcion", "Mi texto escrito")
                        .param("tipo", "OTRO").param("almuerzo", "false")
                        .param("color", "rojo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ocurrencia-form"))
                .andExpect(model().attributeHasFieldErrors("form", "codigo", "tipo", "color"))
                .andExpect(content().string(containsString("value=\"Mi texto escrito\"")));
        verify(ocurrenciaService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void crear_valido_redirigeConMensajeDeExito() throws Exception {
        when(ocurrenciaService.crear(any())).thenReturn(
                new Ocurrencia(5, "VAC", "Vacaciones", "DESCUENTO", "#123456", 5, true, false));

        mockMvc.perform(post("/admin/ocurrencias/crear").with(csrf())
                        .param("codigo", "vac").param("descripcion", "Vacaciones")
                        .param("tipo", "DESCUENTO").param("almuerzo", "false")
                        .param("color", "#123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ocurrencias"))
                .andExpect(flash().attribute("success", containsString("VAC")));
    }

    // ---------- Errores inesperados: solo el mensaje genérico ----------

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void crear_conErrorInesperado_noMuestraDetalleTecnico() throws Exception {
        when(ocurrenciaService.crear(any())).thenThrow(new RuntimeException(SECRETO));

        mockMvc.perform(post("/admin/ocurrencias/crear").with(csrf())
                        .param("codigo", "VAC").param("descripcion", "Vacaciones")
                        .param("tipo", "DESCUENTO").param("almuerzo", "false")
                        .param("color", "#123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", allOf(
                        containsString(MensajesError.GENERICO),
                        not(containsString("detalle técnico secreto")))));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void actualizar_conErrorInesperado_noMuestraDetalleTecnico() throws Exception {
        when(ocurrenciaService.actualizar(eq(2), any())).thenThrow(new RuntimeException(SECRETO));

        mockMvc.perform(post("/admin/ocurrencias/2/actualizar").with(csrf())
                        .param("descripcion", "Permiso").param("color", "#123456").param("orden", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", allOf(
                        containsString(MensajesError.GENERICO),
                        not(containsString("detalle técnico secreto")))));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void listar_conErrorInesperado_noMuestraDetalleTecnico() throws Exception {
        when(ocurrenciaService.listarParaAdministracion()).thenThrow(new RuntimeException(SECRETO));

        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", allOf(
                        containsString(MensajesError.GENERICO),
                        not(containsString("detalle técnico secreto")))))
                .andExpect(content().string(not(containsString("detalle técnico secreto"))));
    }
}
