package lat.jbluesoft.erpadmin.core.config;

import jakarta.servlet.RequestDispatcher;
import lat.jbluesoft.erpadmin.asistencia.controller.OcurrenciaAdminController;
import lat.jbluesoft.erpadmin.asistencia.service.OcurrenciaService;
import lat.jbluesoft.erpadmin.core.controller.HomeController;
import lat.jbluesoft.erpadmin.core.security.CustomUserDetailsService;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lat.jbluesoft.erpadmin.rrhh.service.DepartamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Páginas de error (templates/error/): cada código usa su plantilla, ninguna
 * muestra detalles técnicos y todas llevan "Regresar al comienzo" hacia "/".
 *
 * MockMvc no reenvía a /error cuando la respuesta termina en sendError (eso lo
 * hace el contenedor de servlets). Por eso el renderizado se prueba pidiendo
 * /error con los atributos que el contenedor pone en ese reenvío, y el
 * estado (403, 404) se prueba por separado.
 */
@WebMvcTest({OcurrenciaAdminController.class, HomeController.class})
@Import({SecurityConfig.class, PaginaErrorResolver.class})
class PaginasErrorTest {

    private static final String SECRETO = "detalle técnico secreto: tabla iam.usuario";
    private static final String RUTA_INTERNA = "/ruta/interna/privada";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcurrenciaService ocurrenciaService;

    @MockitoBean
    private DepartamentoService departamentoService;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ---------- Cada código, su plantilla, sin sesión ----------

    @ParameterizedTest(name = "{0} sin sesión -> {1}")
    @CsvSource({
            "404, error/404, No encontramos esa página",
            "403, error/403, No tienes permiso para entrar aquí",
            "500, error/500, Algo salió mal de nuestro lado",
            "400, error/error, No pudimos completar la acción",
            "405, error/error, No pudimos completar la acción"
    })
    void cadaCodigo_sinSesion_usaSuPlantillaSinDetalleTecnico(int codigo, String vista, String titulo) throws Exception {
        pedirPaginaDeError(codigo)
                .andExpect(status().is(codigo))
                .andExpect(view().name(vista))
                .andExpect(content().string(containsString(titulo)))
                .andExpect(content().string(containsString(">" + codigo + "<")))
                .andExpect(content().string(containsString("href=\"/\"")))
                .andExpect(content().string(containsString("Regresar al comienzo")))
                .andExpect(content().string(not(containsString("detalle técnico secreto"))))
                .andExpect(content().string(not(containsString(RUTA_INTERNA))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("java."))));
    }

    @ParameterizedTest(name = "{0} con sesión -> {1}")
    @CsvSource({
            "404, error/404",
            "403, error/403",
            "500, error/500",
            "405, error/error"
    })
    @WithMockUser(roles = "ADMIN_DPTO")
    void cadaCodigo_conSesion_usaSuPlantilla(int codigo, String vista) throws Exception {
        pedirPaginaDeError(codigo)
                .andExpect(status().is(codigo))
                .andExpect(view().name(vista))
                .andExpect(content().string(containsString("Regresar al comienzo")))
                .andExpect(content().string(not(containsString("detalle técnico secreto"))));
    }

    @Test
    void cadaPagina_tieneTituloDePestanaPropio() throws Exception {
        pedirPaginaDeError(404).andExpect(content().string(containsString("<title>No encontrada - erpadmin</title>")));
        pedirPaginaDeError(403).andExpect(content().string(containsString("<title>Sin permiso - erpadmin</title>")));
        pedirPaginaDeError(500).andExpect(content().string(containsString("<title>Error del sistema - erpadmin</title>")));
        pedirPaginaDeError(400).andExpect(content().string(containsString("<title>No se pudo completar - erpadmin</title>")));
    }

    @Test
    void respuestaJson_noIncluyeRutaMensajeExcepcionNiTraza() throws Exception {
        // Comprueba que spring.web.error.* de application.properties se aplica
        mockMvc.perform(get("/error")
                        .accept(MediaType.APPLICATION_JSON)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, RUTA_INTERNA)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException(SECRETO))
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, SECRETO))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("detalle técnico secreto"))));
    }

    // ---------- Seguridad ----------

    @Test
    void sinSesion_recursoEstaticoInexistente_devuelve404SinIrAlLogin() throws Exception {
        mockMvc.perform(get("/css/no-existe.css"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Location", nullValue()));
    }

    @Test
    void sinSesion_rutaProtegidaInexistente_vaAlLogin() throws Exception {
        // Comportamiento buscado, no un fallo: sin sesión no se revela qué rutas
        // existen. Cambiarlo exigiría abrir rutas que hoy piden sesión.
        mockMvc.perform(get("/ruta-que-no-existe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_DPTO")
    void adminDpto_enAdminOcurrencias_recibe403SinIrAlLoginNiLlamarAlServicio() throws Exception {
        mockMvc.perform(get("/admin/ocurrencias"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Location", nullValue()));
        verify(ocurrenciaService, never()).listarParaAdministracion();

        // El contenedor reenvía ese 403 a /error: se muestra la página 403
        pedirPaginaDeError(403)
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    // ---------- Destino del botón ----------

    @Test
    @WithMockUser(roles = "ADMIN_DPTO")
    void raiz_conSesion_llevaAlDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void raiz_sinSesion_llevaAlLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(redirectedUrl("/login"));
    }

    /** Simula el reenvío del contenedor a /error tras un error con detalle técnico. */
    private ResultActions pedirPaginaDeError(int codigo) throws Exception {
        return mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, codigo)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, RUTA_INTERNA)
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException(SECRETO))
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, SECRETO));
    }
}
