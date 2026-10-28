package lat.jbluesoft.erpadmin.core.controller;

import lat.jbluesoft.erpadmin.core.config.SecurityConfig;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetailsService;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pantalla de login: solo título, subtítulo, formulario y mensajes.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void login_muestraTituloSubtituloFormularioYTokenCsrf_sinLogo() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("<title>erp-admin</title>")))
                .andExpect(content().string(containsString("<h1 class=\"jb-title\">erp-admin</h1>")))
                .andExpect(content().string(containsString("Sistemas de Administración Integrado")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"codigo\"")))
                .andExpect(content().string(containsString("autocomplete=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("<img"))))
                .andExpect(content().string(not(containsString("<svg"))))
                .andExpect(content().string(not(containsString("jb-alert"))));
    }

    @Test
    void login_conError_muestraMensajeDeCredenciales() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("Código o contraseña incorrectos")));
    }

    @Test
    void login_trasCerrarSesion_muestraMensaje() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sesión cerrada exitosamente")));
    }
}
