package lat.jbluesoft.erpadmin.core.service;

import lat.jbluesoft.erpadmin.core.model.Etiqueta;

import java.util.List;
import java.util.Map;

/**
 * EtiquetaService
 * Acceso en caché a los títulos/labels administrables (core.etiqueta)
 */
public interface EtiquetaService {

    /**
     * Obtener el valor vigente de una etiqueta por su clave.
     * Si la clave no existe en caché, devuelve la clave misma como fallback.
     */
    String get(String clave);

    /**
     * Obtener una copia inmutable de todas las etiquetas en caché (clave -> valor vigente)
     */
    Map<String, String> getTodas();

    /**
     * Volver a cargar la caché desde la base de datos
     */
    void refrescarCache();

    /**
     * Listar todas las etiquetas completas (para la pantalla de administración),
     * ordenadas por descripción
     */
    List<Etiqueta> listarTodas();

    /**
     * Actualizar el valor personalizado de una etiqueta y refrescar la caché.
     * Un valor nulo o en blanco se guarda como NULL (se usa el valor por defecto).
     *
     * @throws IllegalArgumentException si la clave no existe
     */
    void actualizar(String clave, String nuevoValorPersonalizado, String usuario);
}
