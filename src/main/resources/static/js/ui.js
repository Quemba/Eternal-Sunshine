(function () {
    'use strict';

    if (window.__uiIniciado) return;
    window.__uiIniciado = true;

    var reducido = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function ponerViewport() {
        if (document.querySelector('meta[name="viewport"]')) return;

        var meta = document.createElement('meta');
        meta.name = 'viewport';
        meta.content = 'width=device-width, initial-scale=1';
        document.head.appendChild(meta);
    }

    ponerViewport();

    function ponerFavicon() {
        if (document.querySelector('link[rel~="icon"]')) return;

        var enlace = document.createElement('link');
        enlace.rel = 'icon';
        enlace.type = 'image/png';
        enlace.href = '/images/logo.png';
        document.head.appendChild(enlace);
    }

    function iconoPara(texto) {
        var t = texto.toLowerCase();

        if (t.indexOf('carrito') !== -1) return 'ti-shopping-cart';
        if (t.indexOf('coincid') !== -1 || t.indexOf('filtros') !== -1 || t.indexOf('encontr') !== -1) return 'ti-search';
        if (t.indexOf('venta') !== -1) return 'ti-receipt';
        if (t.indexOf('entrada') !== -1 || t.indexOf('llegó') !== -1 || t.indexOf('mercanc') !== -1) return 'ti-package-import';
        if (t.indexOf('movimiento') !== -1) return 'ti-history';
        if (t.indexOf('solicitud') !== -1) return 'ti-clipboard-check';
        if (t.indexOf('producto') !== -1) return 'ti-package';
        if (t.indexOf('suficiente') !== -1) return 'ti-mood-smile';

        return 'ti-inbox';
    }

    function decorarVacios() {
        document.querySelectorAll('.vacio, .sin-resultados, .ok-msg').forEach(function (elemento) {
            if (elemento.hasAttribute('data-ui-ico')) return;

            elemento.setAttribute('data-ui-ico', '1');

            var icono = document.createElement('i');
            icono.className = 'ti ' + iconoPara(elemento.textContent || '') + ' ui-vacio-icono';
            icono.setAttribute('aria-hidden', 'true');

            elemento.insertBefore(icono, elemento.firstChild);
        });
    }

    function dibujarVistos() {
        document.querySelectorAll('.alerta-ok .alerta-icono').forEach(function (contenedor) {
            if (contenedor.hasAttribute('data-ui-visto')) return;

            contenedor.setAttribute('data-ui-visto', '1');

            contenedor.innerHTML =
                '<svg class="visto-svg" viewBox="0 0 52 52" width="26" height="26" aria-hidden="true">' +
                '<circle class="visto-circulo" cx="26" cy="26" r="23" fill="none" stroke="currentColor" stroke-width="4"></circle>' +
                '<path class="visto-marca" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" d="M15 27l8 8 15-17"></path>' +
                '</svg>';
        });
    }

    var pendiente = false;

    function programarDecorado() {
        if (pendiente) return;

        pendiente = true;

        requestAnimationFrame(function () {
            pendiente = false;
            decorarVacios();
            dibujarVistos();
        });
    }

    function formatear(numero) {
        return String(numero).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    }

    function contar() {
        if (reducido) return;

        document.querySelectorAll('[data-contar]').forEach(function (elemento) {
            var texto = (elemento.textContent || '').trim();
            var partes = texto.match(/^(\D*)([\d.]+)(\D*)$/);

            if (!partes) return;

            var destino = parseInt(partes[2].replace(/\./g, ''), 10);

            if (!isFinite(destino) || destino <= 0) return;

            var antes = partes[1];
            var despues = partes[3];
            var inicio = null;
            var duracion = 800;

            function paso(t) {
                if (inicio === null) inicio = t;

                var avance = Math.min(1, (t - inicio) / duracion);
                var valor = Math.round(destino * (1 - Math.pow(1 - avance, 3)));

                elemento.textContent = antes + formatear(valor) + despues;

                if (avance < 1) {
                    requestAnimationFrame(paso);
                }
            }

            elemento.textContent = antes + '0' + despues;
            requestAnimationFrame(paso);
        });
    }

    function barraProgreso() {
        try {
            var barra = document.createElement('div');
            barra.className = 'barra-progreso';
            barra.setAttribute('aria-hidden', 'true');
            document.body.appendChild(barra);

            var avance = null;
            var seguridad = null;

            function reiniciar() {
                clearTimeout(avance);
                clearTimeout(seguridad);
                barra.classList.remove('activa');
                barra.style.width = '0';
            }

            function iniciar() {
                reiniciar();

                void barra.offsetWidth;

                barra.classList.add('activa');
                barra.style.width = '35%';

                avance = setTimeout(function () {
                    barra.style.width = '65%';

                    avance = setTimeout(function () {
                        barra.style.width = '85%';
                    }, 900);
                }, 400);

                seguridad = setTimeout(reiniciar, 15000);
            }

            function esNavegacion(enlace) {
                var destino = enlace.getAttribute('href');

                if (!destino || destino.charAt(0) === '#') return false;
                if (enlace.target === '_blank' || enlace.hasAttribute('download')) return false;
                if (enlace.hasAttribute('data-pagina') || enlace.hasAttribute('data-limpiar-todo')) return false;
                if (enlace.origin !== window.location.origin) return false;
                if (enlace.pathname.indexOf('/exportar') !== -1) return false;
                if (/^(mailto|tel|javascript):/i.test(destino)) return false;

                return true;
            }

            document.addEventListener('click', function (e) {
                var enlace = e.target.closest ? e.target.closest('a[href]') : null;

                if (!enlace) return;
                if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
                if (!esNavegacion(enlace)) return;

                setTimeout(function () {
                    if (!e.defaultPrevented) {
                        iniciar();
                    }
                }, 0);
            });

            document.addEventListener('submit', function (e) {
                var formulario = e.target;

                if (!formulario || formulario.target === '_blank') return;

                setTimeout(function () {
                    if (!e.defaultPrevented) {
                        iniciar();
                    }
                }, 0);
            });

            window.addEventListener('pageshow', reiniciar);
            window.addEventListener('pagehide', reiniciar);
        } catch (e) { }
    }

    function iniciarMenuMovil() {
        try {
            var barra = document.querySelector('.sidebar');

            if (!barra) return;

            if (!barra.querySelector('.nav')) {
                barra.classList.add('sin-menu');
                return;
            }

            if (document.querySelector('.btn-menu')) return;

            if (!barra.id) {
                barra.id = 'barra-lateral';
            }

            var boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'btn-menu';
            boton.setAttribute('aria-label', 'Abrir menú');
            boton.setAttribute('aria-expanded', 'false');
            boton.setAttribute('aria-controls', barra.id);
            boton.innerHTML = '<i class="ti ti-menu-2" aria-hidden="true"></i>';

            var capa = document.createElement('div');
            capa.className = 'menu-capa';

            var cerrarBoton = document.createElement('button');
            cerrarBoton.type = 'button';
            cerrarBoton.className = 'menu-cerrar';
            cerrarBoton.setAttribute('aria-label', 'Cerrar menú');
            cerrarBoton.innerHTML = '<i class="ti ti-x" aria-hidden="true"></i>';

            barra.appendChild(cerrarBoton);
            document.body.appendChild(boton);
            document.body.appendChild(capa);

            function abrir() {
                document.body.classList.add('menu-abierto');
                boton.setAttribute('aria-expanded', 'true');
            }

            function cerrar() {
                document.body.classList.remove('menu-abierto');
                boton.setAttribute('aria-expanded', 'false');
            }

            boton.addEventListener('click', function () {
                if (document.body.classList.contains('menu-abierto')) {
                    cerrar();
                } else {
                    abrir();
                }
            });

            capa.addEventListener('click', cerrar);
            cerrarBoton.addEventListener('click', cerrar);

            barra.addEventListener('click', function (e) {
                if (e.target.closest && e.target.closest('a')) {
                    cerrar();
                }
            });

            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') {
                    cerrar();
                }
            });

            if (window.matchMedia) {
                var ancha = window.matchMedia('(min-width: 1025px)');

                if (ancha.addEventListener) {
                    ancha.addEventListener('change', function (e) {
                        if (e.matches) {
                            cerrar();
                        }
                    });
                }
            }
        } catch (e) { }
    }

    function barraFlotante(opciones) {
        try {
            var boton = document.getElementById(opciones.boton);
            var principal = document.getElementById(opciones.principal);
            var secundario = opciones.secundario ? document.getElementById(opciones.secundario) : null;

            if (!boton || !principal) return;

            var barra = document.createElement('div');
            barra.className = 'barra-flotante';
            barra.setAttribute('role', 'region');
            barra.setAttribute('aria-label', opciones.etiqueta);

            var texto = document.createElement('div');
            var fuerte = document.createElement('strong');
            var pequeno = document.createElement('small');

            texto.appendChild(fuerte);
            texto.appendChild(pequeno);

            var ir = document.createElement('button');
            ir.type = 'button';
            ir.textContent = opciones.textoBoton;

            barra.appendChild(texto);
            barra.appendChild(ir);
            document.body.appendChild(barra);

            var enVista = true;

            function actualizar() {
                fuerte.textContent = principal.textContent;
                pequeno.textContent = secundario ? secundario.textContent : '';
                pequeno.style.display = secundario ? '' : 'none';

                barra.classList.toggle('visible', !boton.disabled && !enVista);
            }

            ir.addEventListener('click', function () {
                boton.scrollIntoView({ behavior: reducido ? 'auto' : 'smooth', block: 'center' });
            });

            var observador = new MutationObserver(actualizar);
            var opcionesTexto = { childList: true, characterData: true, subtree: true };

            observador.observe(principal, opcionesTexto);

            if (secundario) {
                observador.observe(secundario, opcionesTexto);
            }

            observador.observe(boton, { attributes: true, attributeFilter: ['disabled'] });

            if ('IntersectionObserver' in window) {
                new IntersectionObserver(function (entradas) {
                    enVista = entradas[0].isIntersecting;
                    actualizar();
                }).observe(boton);
            } else {
                enVista = false;
            }

            actualizar();
        } catch (e) { }
    }

    function iniciar() {
        ponerFavicon();
        decorarVacios();
        dibujarVistos();
        contar();
        iniciarMenuMovil();
        barraProgreso();

        barraFlotante({
            boton: 'btn-finalizar',
            principal: 'total-venta',
            secundario: 'unidades-venta',
            textoBoton: 'Ver carrito',
            etiqueta: 'Resumen del carrito'
        });

        barraFlotante({
            boton: 'btn-registrar',
            principal: 'resumen-lista',
            secundario: null,
            textoBoton: 'Ver lista',
            etiqueta: 'Resumen de la entrada'
        });

        new MutationObserver(programarDecorado).observe(document.documentElement, {
            childList: true,
            subtree: true
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();