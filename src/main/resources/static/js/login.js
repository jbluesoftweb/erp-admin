(function () {
  'use strict';
  var form = document.getElementById('login-form');
  var pwd = document.getElementById('password');
  var toggle = document.getElementById('toggle-password');
  var caps = document.getElementById('caps-hint');
  var btn = document.getElementById('login-submit');

  if (toggle && pwd) {
    toggle.addEventListener('click', function () {
      var show = pwd.type === 'password';
      pwd.type = show ? 'text' : 'password';
      toggle.setAttribute('aria-pressed', String(show));
      toggle.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
      var icon = toggle.querySelector('i');
      if (icon) { icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye'; }
      pwd.focus();
    });
  }

  if (pwd && caps) {
    var check = function (e) {
      if (e.getModifierState) { caps.classList.toggle('on', e.getModifierState('CapsLock')); }
    };
    pwd.addEventListener('keydown', check);
    pwd.addEventListener('keyup', check);
    pwd.addEventListener('blur', function () { caps.classList.remove('on'); });
  }

  if (form && btn) {
    form.addEventListener('submit', function () {
      if (!form.checkValidity()) { return; }
      btn.classList.add('loading');
      btn.setAttribute('aria-busy', 'true');
      // Se deshabilita después del envío para no perder el valor del botón.
      setTimeout(function () { btn.disabled = true; }, 0);
    });
  }
})();
