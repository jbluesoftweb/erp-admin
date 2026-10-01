package lat.jbluesoft.erpadmin.core.service.impl;

import jakarta.annotation.PostConstruct;
import lat.jbluesoft.erpadmin.core.model.Etiqueta;
import lat.jbluesoft.erpadmin.core.repository.EtiquetaRepository;
import lat.jbluesoft.erpadmin.core.service.EtiquetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EtiquetaServiceImpl
 * Implementación con caché en memoria de EtiquetaService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EtiquetaServiceImpl implements EtiquetaService {

    private final EtiquetaRepository etiquetaRepository;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void cargarCache() {
        log.debug("Cargando caché de etiquetas");
        for (Etiqueta etiqueta : etiquetaRepository.findAll()) {
            String valor = (etiqueta.getValorPersonalizado() != null && !etiqueta.getValorPersonalizado().isBlank())
                    ? etiqueta.getValorPersonalizado()
                    : etiqueta.getValorDefecto();
            cache.put(etiqueta.getClave(), valor);
        }
    }

    @Override
    public String get(String clave) {
        return cache.getOrDefault(clave, clave);
    }

    @Override
    public Map<String, String> getTodas() {
        return Collections.unmodifiableMap(cache);
    }

    @Override
    public void refrescarCache() {
        cargarCache();
    }

    @Override
    public List<Etiqueta> listarTodas() {
        return etiquetaRepository.findAll(Sort.by("descripcion", "clave"));
    }

    @Override
    @Transactional
    public void actualizar(String clave, String nuevoValorPersonalizado, String usuario) {
        Etiqueta etiqueta = etiquetaRepository.findById(clave)
                .orElseThrow(() -> new IllegalArgumentException("No existe la etiqueta con clave: " + clave));

        String valorNuevo = nuevoValorPersonalizado != null ? nuevoValorPersonalizado.trim() : null;
        if (valorNuevo != null && valorNuevo.isEmpty()) {
            valorNuevo = null;
        }
        if (valorNuevo != null && valorNuevo.length() > 100) {
            throw new IllegalArgumentException("El valor no puede superar los 100 caracteres");
        }

        String valorAnterior = etiqueta.getValorPersonalizado();

        etiqueta.setValorPersonalizado(valorNuevo);
        etiqueta.setUpdatedBy(usuario);
        etiqueta.setUpdatedAt(LocalDateTime.now());
        etiquetaRepository.save(etiqueta);

        log.info("Etiqueta actualizada: clave={}, valorAnterior={}, valorNuevo={}, usuario={}",
                clave, valorAnterior, valorNuevo, usuario);

        // Refrescar la caché en la misma operación: findAll() dentro de esta
        // transacción ve la entidad ya modificada (mismo contexto de persistencia)
        cargarCache();
    }
}
