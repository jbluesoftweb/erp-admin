/* Formulario de ocurrencias: sincroniza el selector de color con el texto
   hexadecimal. Sin JS, el selector queda oculto y se escribe el hexadecimal. */
(function () {
    'use strict';

    var HEX = /^#[0-9A-Fa-f]{6}$/;

    try {
        var selector = document.getElementById('colorSelector');
        var texto = document.getElementById('color');
        if (!selector || !texto) { return; }

        // Algunos navegadores antiguos no tienen <input type="color">: se degrada a texto.
        if (selector.type !== 'color') { return; }

        var inicial = texto.value.trim();
        if (HEX.test(inicial)) { selector.value = inicial.toLowerCase(); }
        selector.hidden = false;

        selector.addEventListener('input', function () {
            texto.value = selector.value.toUpperCase();
        });

        texto.addEventListener('input', function () {
            var valor = texto.value.trim();
            if (HEX.test(valor)) { selector.value = valor.toLowerCase(); }
        });
    } catch (e) {
        // Si algo falla, el campo de texto sigue funcionando por sí solo.
        if (window.console && console.warn) { console.warn('Selector de color no disponible:', e); }
    }
})();
