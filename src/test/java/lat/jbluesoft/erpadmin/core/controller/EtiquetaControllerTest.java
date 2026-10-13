package lat.jbluesoft.erpadmin.core.controller;

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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de seguridad de EtiquetaController: solo ADMIN_SYS puede
 * administrar etiquetas (/admin/**).
 */
@WebMvcTest(EtiquetaController.class)
@Import(SecurityConfig.class)
class EtiquetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listar_sinAutenticar_redirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/etiquetas"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void listar_conRolSinPermiso_devuelve403() throws Exception {
        mockMvc.perform(get("/admin/etiquetas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void listar_conAdminSys_devuelve200() throws Exception {
        when(etiquetaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/admin/etiquetas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void actualizar_conRolSinPermiso_devuelve403() throws Exception {
        mockMvc.perform(post("/admin/etiquetas/actualizar")
                        .with(csrf())
                        .param("clave", "rrhh.personal.nivel")
                        .param("valorPersonalizado", "Test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SYS")
    void refrescar_conErrorInesperado_noMuestraDetalleTecnico() throws Exception {
        doThrow(new RuntimeException("detalle técnico secreto: tabla iam.usuario"))
                .when(etiquetaService).refrescarCache();

        mockMvc.perform(post("/admin/etiquetas/refrescar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", allOf(
                        containsString(MensajesError.GENERICO),
                        not(containsString("detalle técnico secreto")))));
    }
}
