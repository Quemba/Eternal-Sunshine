(function () {
    'use strict';

    function cerrar(alerta) {
        alerta.classList.add('alerta-salida');
        setTimeout(function () {
            if (alerta.parentNode) {
                alerta.parentNode.removeChild(alerta);
            }
        }, 260);
    }

    document.addEventListener('click', function (e) {
        const boton = e.target && e.target.closest ? e.target.closest('.alerta-cerrar') : null;

        if (!boton) return;

        const alerta = boton.closest('.alerta');

        if (alerta) {
            cerrar(alerta);
        }
    });

    document.querySelectorAll('.alerta-ok').forEach(function (alerta) {
        setTimeout(function () {
            cerrar(alerta);
        }, 6000);
    });
})();