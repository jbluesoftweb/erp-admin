/* Marco de la aplicación: barra lateral, cajón móvil y menú de usuario. */
(function () {
    'use strict';

    var DESKTOP = window.matchMedia('(min-width: 992px)');
    var STORAGE_KEY = 'jb.sidebar';
    var root = document.documentElement;

    var sidebar = document.getElementById('sidebar');
    var overlay = document.getElementById('sidebarOverlay');
    var toggle = document.getElementById('menuToggle');
    var userMenu = document.getElementById('userMenu');
    var userDropdown = document.getElementById('userDropdown');
    var userButton = userMenu ? userMenu.querySelector('.user-trigger') : null;

    function store(value) {
        try { localStorage.setItem(STORAGE_KEY, value); } catch (e) { /* sin almacenamiento: solo esta sesión */ }
    }

    // ---------- Barra contraída (escritorio) ----------
    function isRail() { return root.classList.contains('jb-rail'); }

    // En modo íconos, el nombre de cada ítem se muestra como tooltip (title).
    function syncTooltips() {
        if (!sidebar) { return; }
        var rail = DESKTOP.matches && isRail();
        sidebar.querySelectorAll('.menu-item, .menu-category').forEach(function (el) {
            var label = el.querySelector('.menu-label');
            if (rail && label) { el.setAttribute('title', label.textContent.replace(/\s+/g, ' ').trim()); }
            else { el.removeAttribute('title'); }
        });
    }

    // ---------- Cajón (celular / tablet) ----------
    function isDrawerOpen() { return !!sidebar && sidebar.classList.contains('mobile-show'); }

    function setInert(on) {
        document.querySelectorAll('.main-wrapper, #footer').forEach(function (el) {
            if (on) { el.setAttribute('inert', ''); } else { el.removeAttribute('inert'); }
        });
    }

    function openDrawer() {
        if (!sidebar) { return; }
        sidebar.classList.add('mobile-show');
        if (overlay) { overlay.classList.add('show'); }
        root.classList.add('jb-drawer-open');
        setInert(true);
        syncToggle();
        var first = sidebar.querySelector('.sidebar-menu a, .sidebar-menu button');
        if (first) { first.focus({ preventScroll: true }); }
    }

    function closeDrawer(returnFocus) {
        if (!sidebar || !isDrawerOpen()) { return; }
        sidebar.classList.remove('mobile-show');
        if (overlay) { overlay.classList.remove('show'); }
        root.classList.remove('jb-drawer-open');
        setInert(false);
        syncToggle();
        if (returnFocus && toggle) { toggle.focus(); }
    }

    function syncToggle() {
        if (!toggle) { return; }
        var expanded, label;
        if (DESKTOP.matches) {
            expanded = !isRail();
            label = expanded ? 'Contraer menú lateral' : 'Expandir menú lateral';
        } else {
            expanded = isDrawerOpen();
            label = expanded ? 'Cerrar menú' : 'Abrir menú';
        }
        toggle.setAttribute('aria-expanded', String(expanded));
        toggle.setAttribute('aria-label', label);
        toggle.setAttribute('title', label);
    }

    if (toggle) {
        toggle.addEventListener('click', function () {
            if (DESKTOP.matches) {
                root.classList.toggle('jb-rail');
                store(isRail() ? 'rail' : 'full');
                syncTooltips();
                syncToggle();
            } else if (isDrawerOpen()) {
                closeDrawer(true);
            } else {
                openDrawer();
            }
        });
    }

    if (overlay) {
        overlay.addEventListener('click', function () { closeDrawer(true); });
    }

    if (sidebar) {
        // Navegar desde el cajón lo cierra.
        sidebar.addEventListener('click', function (e) {
            if (!DESKTOP.matches && e.target.closest('a[href]')) { closeDrawer(false); }
        });
    }

    var onBreakpoint = function () {
        if (DESKTOP.matches) { closeDrawer(false); }
        syncTooltips();
        syncToggle();
    };
    if (DESKTOP.addEventListener) { DESKTOP.addEventListener('change', onBreakpoint); }
    else if (DESKTOP.addListener) { DESKTOP.addListener(onBreakpoint); }

    // Al volver con el botón Atrás (bfcache) el cajón debe estar cerrado.
    window.addEventListener('pageshow', function () { closeDrawer(false); });

    // ---------- Categorías colapsables ----------
    function setCategory(button, open) {
        var submenu = document.getElementById(button.getAttribute('aria-controls'));
        button.setAttribute('aria-expanded', String(open));
        if (submenu) { submenu.classList.toggle('collapsed', !open); }
    }

    document.querySelectorAll('.menu-category[aria-controls]').forEach(function (button) {
        button.addEventListener('click', function () {
            setCategory(button, button.getAttribute('aria-expanded') !== 'true');
        });
    });

    // ---------- Ítem activo ----------
    // Se elige el enlace cuyo href coincide con la ruta actual o es su prefijo
    // más largo (p. ej. /asistencia/usuarios/editar/5 → LISTAR usuarios).
    function normalize(path) { return path.replace(/\/+$/, '') || '/'; }

    if (sidebar) {
        var current = normalize(window.location.pathname);
        var best = null, bestLen = -1;
        sidebar.querySelectorAll('a.menu-item[href]').forEach(function (link) {
            var path = normalize(new URL(link.getAttribute('href'), window.location.href).pathname);
            if ((current === path || current.indexOf(path + '/') === 0) && path.length > bestLen) {
                best = link; bestLen = path.length;
            }
        });
        if (best) {
            best.classList.add('active');
            best.setAttribute('aria-current', 'page');
            var submenu = best.closest('.submenu');
            if (submenu && submenu.id) {
                var button = sidebar.querySelector('[aria-controls="' + submenu.id + '"]');
                if (button) { setCategory(button, true); }
            }
        }
    }

    // ---------- Menú de usuario ----------
    function isUserOpen() { return !!userDropdown && userDropdown.classList.contains('show'); }

    function setUserMenu(open, returnFocus) {
        if (!userDropdown || !userButton) { return; }
        userDropdown.classList.toggle('show', open);
        userButton.setAttribute('aria-expanded', String(open));
        if (!open && returnFocus) { userButton.focus(); }
    }

    if (userButton && userDropdown) {
        userButton.addEventListener('click', function () {
            var open = !isUserOpen();
            setUserMenu(open, false);
            if (open) {
                var first = userDropdown.querySelector('a, button');
                if (first) { first.focus(); }
            }
        });

        // Clic fuera cierra.
        document.addEventListener('click', function (e) {
            if (isUserOpen() && !userMenu.contains(e.target)) { setUserMenu(false, false); }
        });

        // Al salir con Tab del menú, se cierra.
        userMenu.addEventListener('focusout', function (e) {
            if (isUserOpen() && e.relatedTarget && !userMenu.contains(e.relatedTarget)) { setUserMenu(false, false); }
        });
    }

    // ---------- Esc ----------
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') { return; }
        if (isUserOpen()) { setUserMenu(false, true); }
        else if (isDrawerOpen()) { closeDrawer(true); }
    });

    syncTooltips();
    syncToggle();

    // Habilita las transiciones después del primer pintado.
    requestAnimationFrame(function () {
        requestAnimationFrame(function () { root.classList.remove('jb-no-anim'); });
    });
})();
