package lat.jbluesoft.erpadmin.asistencia.controller;

import lat.jbluesoft.erpadmin.asistencia.service.AlmuerzoService;
import lat.jbluesoft.erpadmin.core.config.SecurityConfig;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetails;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetailsService;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lat.jbluesoft.erpadmin.core.util.MensajesError;
import lat.jbluesoft.erpadmin.iam.model.RegistroSistema;
import lat.jbluesoft.erpadmin.iam.model.Rol;
import lat.jbluesoft.erpadmin.iam.model.Usuario;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Ante un error inesperado, AlmuerzoController muestra un mensaje genérico
 * y no el detalle técnico de la excepción.
 */
@WebMvcTest(AlmuerzoController.class)
@Import(SecurityConfig.class)
class AlmuerzoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlmuerzoService almuerzoService;

    @MockitoBean
    private DepartamentoService departamentoService;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void mostrarAlmuerzo_conErrorInesperado_noMuestraDetalleTecnico() throws Exception {
        when(departamentoService.buscarPorId(any()))
                .thenThrow(new RuntimeException("detalle técnico secreto: tabla iam.usuario"));

        mockMvc.perform(get("/almuerzo").with(user(usuarioConRol("ADMIN_DPTO"))))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("error", allOf(
                        containsString(MensajesError.GENERICO),
                        not(containsString("detalle técnico secreto")))));
    }

    private static CustomUserDetails usuarioConRol(String codigoRol) {
        Usuario usuario = new Usuario();
        usuario.setCodigo("12345");
        Rol rol = new Rol();
        rol.setCodigo(codigoRol);
        RegistroSistema registro = new RegistroSistema();
        registro.setRol(rol);
        return new CustomUserDetails(usuario, List.of(registro));
    }
}
