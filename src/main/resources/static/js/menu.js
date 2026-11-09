(function () {
    'use strict';

    const INTERVALO_MS = 45000;

    const ruta = window.location.pathname.replace(/\/+$/, '') || '/';
    const enlaces = document.querySelectorAll('.nav a.nav-link');

    let elegido = null;
    let largoElegido = -1;

    enlaces.forEach(function (enlace) {
        const destino = (enlace.getAttribute('href') || '').replace(/\/+$/, '') || '/';

        const coincide = destino === '/'
            ? ruta === '/'
            : (ruta === destino || ruta.indexOf(destino + '/') === 0);

        if (coincide && destino.length > largoElegido) {
            elegido = enlace;
            largoElegido = destino.length;
        }
    });

    if (elegido) {
        elegido.classList.add('activo');
        elegido.setAttribute('aria-current', 'page');
    }

    function poner(clave, cantidad) {
        const insignia = document.querySelector('.nav [data-aviso="' + clave + '"]');

        if (!insignia) return;

        const valor = Number(cantidad) || 0;

        insignia.textContent = valor > 99 ? '99+' : String(valor);
        insignia.classList.toggle('oculto', valor === 0);
    }

    function aplicar(avisos) {
        poner('solicitudes', avisos.solicitudesPendientes);
        poner('misSolicitudes', avisos.misSolicitudesNuevas);
        poner('porAgotarse', avisos.porAgotarse);
        poner('reabastecimiento', avisos.reabastecimiento);
        poner('conteos', avisos.conteosPendientes);

        const reabastecimiento = document.querySelector('.nav [data-aviso="reabastecimiento"]');

        if (reabastecimiento) {
            const hayUrgentes = (Number(avisos.urgentes) || 0) > 0;
            reabastecimiento.classList.toggle('nav-badge-rojo', hayUrgentes);
            reabastecimiento.classList.toggle('nav-badge-ambar', !hayUrgentes);
        }
    }

    let temporizador = null;
    let activo = true;

    async function actualizar() {
        if (!activo || document.visibilityState !== 'visible') return;

        try {
            const respuesta = await fetch('/avisos', {
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin'
            });

            const tipo = respuesta.headers.get('Content-Type') || '';

            if (!respuesta.ok || tipo.indexOf('application/json') === -1) {
                activo = false;
                return;
            }

            aplicar(await respuesta.json());

        } catch (e) { }
    }

    if (document.querySelector('.nav [data-aviso]')) {
        temporizador = setInterval(actualizar, INTERVALO_MS);

        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible') {
                actualizar();
            }
        });
    }
})();