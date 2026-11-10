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

/* ---------- Cierre de sesión: el atardecer ---------- */
(function () {
    'use strict';

    let cerrando = false;

    function estilos() {
        if (document.getElementById('sl-estilos')) return;

        const estilo = document.createElement('style');
        estilo.id = 'sl-estilos';
        estilo.textContent = `
            .sl-pantalla { position: fixed; inset: 0; z-index: 10000; overflow: hidden; cursor: pointer;
                background: linear-gradient(to top, #ffe2b0 0%, #f7a8c0 55%, #e6b3e0 100%);
                opacity: 0; transition: opacity .35s ease; }
            .sl-pantalla.sl-visible { opacity: 1; }
            .sl-pantalla.sl-saliendo { opacity: 0; transition-duration: .4s; }

            .sl-noche { position: absolute; inset: 0;
                background: linear-gradient(to top, #4a1020 0%, #5b2b73 50%, #241238 100%);
                opacity: 0; transition: opacity 2.2s ease; }
            .sl-pantalla.sl-ocaso .sl-noche { opacity: 1; }

            .sl-centro { position: absolute; inset: 0; display: flex; flex-direction: column;
                align-items: center; justify-content: center; gap: 28px; padding: 18vh 20px 0; text-align: center; }

            .sl-casas { position: relative; width: clamp(120px, 22vw, 220px);
                filter: drop-shadow(0 8px 18px rgba(74,16,32,.35)); }
            .sl-casas img { display: block; width: 100%; height: auto; }

            /* El sol vive dentro de la casa: baja y queda totalmente detrás de ella */
            .sl-casas .sl-sol { position: absolute; left: 29%; top: 0; width: 42%; z-index: -1;
                transform: translateY(-190%); }
            .sl-pantalla.sl-ocaso .sl-sol { animation: sl-sol-baja 1.8s ease-in forwards; }
            .sl-pantalla.sl-cae .sl-sol { opacity: 0; transition: opacity .4s ease .3s; }
            .sl-casas .sl-rota { position: absolute; left: 50%; bottom: 0; width: 120%; max-width: none;
                margin-left: -60%; opacity: 0; transition: opacity .5s ease .25s; }
            .sl-casas .sl-sana { transition: opacity .5s ease .25s; }
            .sl-pantalla.sl-cae .sl-sana { opacity: 0; }
            .sl-pantalla.sl-cae .sl-rota { opacity: 1; }
            .sl-pantalla.sl-cae .sl-casas { animation: sl-latido .9s ease-in-out 1; }

            .sl-frase { min-height: 96px; margin: 0; color: #ffffff; font-weight: 800;
                font-size: clamp(20px, 5vw, 26px); line-height: 1.35; opacity: 0;
                transform: translateY(8px); transition: opacity .9s ease, transform .9s ease; }
            .sl-frase span { display: block; }
            .sl-frase span + span { margin-top: 4px; font-size: .8em; font-weight: 700; opacity: .85; }
            .sl-pantalla.sl-frase-visible .sl-frase { opacity: 1; transform: translateY(0); }

            @keyframes sl-sol-baja {
                0% { transform: translateY(-190%); }
                100% { transform: translateY(55%); }
            }

            @keyframes sl-latido {
                0% { transform: scale(1); } 30% { transform: scale(1.14); } 100% { transform: scale(1); }
            }

            @media (prefers-reduced-motion: reduce) {
                .sl-pantalla, .sl-noche, .sl-casas img, .sl-frase { transition-duration: .01s !important; }
                .sl-pantalla.sl-ocaso .sl-sol { animation-duration: .01s; }
                .sl-pantalla.sl-cae .sl-casas { animation: none; }
            }`;

        document.head.appendChild(estilo);
    }

    function esperar(ms) {
        return new Promise(function (resolver) { setTimeout(resolver, ms); });
    }

    async function cerrarSesion(demo) {
        if (cerrando) return;

        cerrando = true;
        estilos();

        // La sesión se cierra de verdad mientras suena la animación
        let cierre = Promise.resolve();

        if (!demo) {
            cierre = fetch('/logout', {
                method: 'POST',
                credentials: 'same-origin',
                redirect: 'manual'
            }).catch(function () { return null; });
        }

        const pantalla = document.createElement('div');
        pantalla.className = 'sl-pantalla';
        pantalla.setAttribute('role', 'status');
        pantalla.setAttribute('aria-label', 'Cerrando sesión');

        pantalla.innerHTML =
            '<div class="sl-noche"></div>' +
            '<div class="sl-centro">' +
                '<div class="sl-casas">' +
                    '<div class="sl-sol"><img src="/images/anim-sol.png" alt=""></div>' +
                    '<img class="sl-sana" src="/images/anim-casa.png" alt="">' +
                    '<img class="sl-rota" src="/images/anim-casadestrozada.png" alt="">' +
                '</div>' +
                '<p class="sl-frase"><span>Desear\u00eda haberme quedado.</span><span>De verdad.</span></p>' +
            '</div>';

        document.body.appendChild(pantalla);

        let omitido = false;

        pantalla.addEventListener('click', function () {
            if (demo) {
                // En la vista previa, un clic la repite
                pantalla.remove();
                cerrando = false;
                setTimeout(function () { cerrarSesion(true); }, 300);
                return;
            }

            omitido = true;
        });

        await esperar(30);
        pantalla.classList.add('sl-visible');

        await esperar(500);
        pantalla.classList.add('sl-ocaso');

        await esperar(1900);
        pantalla.classList.add('sl-cae');

        await esperar(900);
        pantalla.classList.add('sl-frase-visible');

        if (demo) return;

        for (let i = 0; i < 25 && !omitido; i++) {
            await esperar(100);
        }

        pantalla.classList.add('sl-saliendo');

        await cierre;

        await esperar(omitido ? 0 : 400);

        window.location.href = '/login?salio';
    }

    // Vista previa: abre cualquier pantalla con ?demo-salida (toca la pantalla para repetirla)
    if (/[?&]demo-salida\b/.test(window.location.search)) {
        cerrarSesion(true);
    }

    document.addEventListener('submit', function (evento) {
        const formulario = evento.target;

        if (!formulario || !formulario.getAttribute) return;

        if ((formulario.getAttribute('action') || '') !== '/logout') return;

        evento.preventDefault();
        cerrarSesion(false);
    }, true);
})();