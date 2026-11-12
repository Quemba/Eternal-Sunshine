/* ---------- Recorrido para tiendas nuevas ----------
   Se activa solo cuando alguien acaba de crear su tienda (el registro deja la marca),
   o al abrir cualquier pantalla con ?tutorial para repetirlo.
   Guía paso a paso por las pantallas reales: producto, entrada de mercancía y venta. */
(function () {
    'use strict';

    var CLAVE = 'es-tutorial';
    var TOTAL = 8;
    var HECHO = 'es-tutorial-hecho';

    // Cuentas (en este navegador) que ya vieron el recorrido, para no repetirlo
    function yaHecho(usuario) {
        try {
            var lista = JSON.parse(window.localStorage.getItem(HECHO) || '[]');
            return lista.indexOf(usuario) !== -1;
        } catch (e) {
            return false;
        }
    }

    function marcarHecho(usuario) {
        if (!usuario) return;

        try {
            var lista = JSON.parse(window.localStorage.getItem(HECHO) || '[]');
            if (lista.indexOf(usuario) === -1) {
                lista.push(usuario);
                window.localStorage.setItem(HECHO, JSON.stringify(lista));
            }
        } catch (e) { /* nada */ }
    }

    function esAdministrador() {
        var nodo = document.querySelector('.nav-usuario-datos span');
        return !!nodo && /admin/i.test(nodo.textContent);
    }

    function leer() {
        try {
            var texto = window.localStorage.getItem(CLAVE);
            return texto ? JSON.parse(texto) : null;
        } catch (e) {
            return null;
        }
    }

    function guardar(estado) {
        try {
            window.localStorage.setItem(CLAVE, JSON.stringify(estado));
        } catch (e) { /* sin almacenamiento: el recorrido simplemente no continúa */ }
    }

    function borrar() {
        try {
            window.localStorage.removeItem(CLAVE);
        } catch (e) { /* nada */ }
    }

    function usuarioActual() {
        var nodo = document.querySelector('.nav-usuario-datos strong');
        return nodo ? nodo.textContent.trim().toLowerCase() : '';
    }

    function nombreTienda() {
        var nodos = document.querySelectorAll('.nav-usuario-datos span');
        return nodos.length > 1 ? nodos[1].textContent.trim() : '';
    }

    function ruta() {
        return window.location.pathname.replace(/\/+$/, '') || '/';
    }

    function hayExito() {
        return !!document.querySelector('.alerta-ok');
    }

    /* ---------- Los pasos ---------- */

    var PASOS = {
        1: {
            titulo: 'Empecemos por tus productos',
            texto: 'Aquí vive tu inventario. Abre «Productos» en el menú.',
            objetivo: '.nav a[href="/productos"]',
            destino: '/productos',
            en: function () { return true; }
        },
        2: {
            titulo: 'Crea tu primer producto',
            texto: 'Pulsa «Nuevo producto».',
            objetivo: 'a[href="/productos/nuevo"]',
            destino: '/productos',
            en: function (p) { return p === '/productos'; }
        },
        3: {
            titulo: 'Cuéntanos qué vendes',
            texto: 'Escribe un código (por ejemplo 001) o escanéalo con la cámara, y completa el nombre, la categoría y el precio. '
                + 'Deja la cantidad en 0: el stock lo sumaremos en el siguiente paso. '
                + 'Luego pulsa «Guardar Producto».',
            objetivo: 'form[action="/productos/guardar"]',
            destino: '/productos/nuevo',
            en: function (p) { return p === '/productos/nuevo'; }
        },
        4: {
            titulo: '¡Tu primer producto está creado!',
            texto: 'Ahora imagina que llegó mercancía. Abre «Recibir mercancía» en el menú.',
            objetivo: '.nav a[href="/entradas/nueva"]',
            destino: '/entradas/nueva',
            en: function () { return true; }
        },
        5: {
            titulo: 'Suma stock a tu producto',
            texto: 'Busca tu producto por nombre o código (o usa «Escanear» para leer el código con la cámara), '
                + 'agrégalo a la lista, indica cuántas unidades llegaron y pulsa «Registrar entrada».',
            objetivo: '#buscador',
            destino: '/entradas/nueva',
            en: function (p) { return p === '/entradas/nueva'; }
        },
        6: {
            titulo: '¡Ya tienes stock!',
            texto: 'Ahora vamos a vender. Abre «Nueva venta» en el menú.',
            objetivo: '.nav a[href="/ventas/nueva"]',
            destino: '/ventas/nueva',
            en: function () { return true; }
        },
        7: {
            titulo: 'Tu primera venta',
            texto: 'Busca el producto o escanéalo con la cámara, agrégalo al carrito y pulsa «Finalizar venta».',
            objetivo: '#buscador',
            destino: '/ventas/nueva',
            en: function (p) { return p === '/ventas/nueva'; }
        },
        8: {
            titulo: 'Este es tu recibo',
            texto: 'Cada venta genera un comprobante con su propio número, y el stock ya se descontó. '
                + 'Puedes imprimirlo cuando quieras.',
            objetivo: '.ticket',
            destino: null,
            siguiente: true,
            en: function (p) { return /^\/ventas\/\d+$/.test(p); }
        }
    };

    /* ---------- Qué paso toca según dónde está la persona ---------- */

    function avanzar(estado) {
        var p = ruta();
        var busqueda = window.location.search;
        var cambio = true;
        var vueltas = 0;

        while (cambio && vueltas < 10) {
            cambio = false;
            vueltas++;

            var n = estado.paso;
            var nuevo = n;

            if (n === 1 && p === '/productos') nuevo = 2;
            else if ((n === 1 || n === 2) && p === '/productos/nuevo') nuevo = 3;
            else if (n === 2 && p === '/productos' && hayExito()) nuevo = 4;
            else if (n === 3 && p === '/productos') nuevo = 4;
            else if (n <= 4 && p === '/entradas/nueva') nuevo = 5;
            else if (n <= 6 && p === '/ventas/nueva') nuevo = 7;
            else if (n === 5 && p === '/entradas') nuevo = /[?&]registradas=/.test(busqueda) ? 6 : 4;
            else if (n === 7 && /^\/ventas\/\d+$/.test(p) && /[?&]ok=/.test(busqueda)) nuevo = 8;
            else if (n === 7 && p === '/ventas') nuevo = 6;

            if (nuevo !== n) {
                estado.paso = nuevo;
                if (nuevo !== 3) estado.enviado = false;
                cambio = true;
            }
        }

        return estado;
    }

    /* ---------- Estilos ---------- */

    function estilos() {
        if (document.getElementById('tu-estilos')) return;

        var estilo = document.createElement('style');
        estilo.id = 'tu-estilos';
        estilo.textContent = '' +
            '.tu-pantalla { position: fixed; inset: 0; z-index: 9500; display: flex; align-items: center; justify-content: center;' +
            '  padding: 20px; box-sizing: border-box; overflow-y: auto;' +
            '  background: linear-gradient(to top, #ffe2b0 0%, #f7a8c0 55%, #e6b3e0 100%);' +
            '  opacity: 0; transition: opacity .4s ease; }' +
            '.tu-pantalla.tu-visible { opacity: 1; }' +

            '.tu-bienvenida { position: relative; width: 100%; max-width: 460px; padding: 32px 28px 26px; box-sizing: border-box;' +
            '  background: #ffffff; border-radius: 24px; text-align: center; color: #4a1020;' +
            '  box-shadow: 0 14px 40px rgba(74,16,32,.28); font-family: inherit; }' +
            '.tu-bienvenida img.tu-casa { display: block; width: 130px; max-width: 40%; height: auto; margin: 0 auto 12px;' +
            '  animation: tu-latido 2.6s ease-in-out infinite; }' +
            '.tu-bienvenida img.tu-sol { display: block; width: 84px; height: auto; margin: 0 auto 10px;' +
            '  animation: tu-girar 24s linear infinite; }' +
            '.tu-bienvenida h2 { margin: 0 0 10px; font-size: 26px; line-height: 1.2; color: #df6794; }' +
            '.tu-bienvenida p { margin: 0 0 12px; font-size: 16px; line-height: 1.55; }' +
            '.tu-bienvenida .tu-nota { font-size: 13px; color: #8a6a73; font-style: italic; margin-bottom: 20px; }' +

            '.tu-botones { display: flex; flex-direction: column; gap: 8px; }' +
            '.tu-btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; padding: 13px 22px;' +
            '  border: 0; border-radius: 12px; background: #df6794; color: #ffffff; font: inherit; font-size: 16px;' +
            '  font-weight: 800; cursor: pointer; text-decoration: none; }' +
            '.tu-btn:hover { background: #cf4f81; }' +
            '.tu-btn:focus-visible, .tu-omitir:focus-visible { outline: 3px solid #c29be7; outline-offset: 2px; }' +
            '.tu-omitir { padding: 8px; border: 0; background: transparent; color: #8a6a73; font: inherit; font-size: 14px;' +
            '  cursor: pointer; text-decoration: underline; }' +
            '.tu-omitir:hover { color: #4a1020; }' +

            '.tu-foco { position: fixed; z-index: 9400; pointer-events: none; border-radius: 14px;' +
            '  box-shadow: 0 0 0 4px #df6794, 0 0 0 9999px rgba(36,18,56,.58);' +
            '  transition: top .25s ease, left .25s ease, width .25s ease, height .25s ease; animation: tu-pulso 1.8s ease-in-out infinite; }' +

            '.tu-card { position: fixed; z-index: 9450; width: 320px; max-width: calc(100vw - 24px); box-sizing: border-box;' +
            '  padding: 18px 18px 14px; background: #ffffff; border-radius: 18px; color: #4a1020; font-family: inherit;' +
            '  box-shadow: 0 10px 30px rgba(74,16,32,.32); animation: tu-entra .35s ease both; }' +
            '.tu-card .tu-paso { margin: 0 0 4px; font-size: 12px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; color: #c29be7; }' +
            '.tu-card h3 { margin: 0 0 6px; font-size: 19px; line-height: 1.25; color: #df6794; }' +
            '.tu-card p.tu-texto { margin: 0 0 12px; font-size: 15px; line-height: 1.5; }' +
            '.tu-card .tu-btn { width: 100%; padding: 11px 16px; font-size: 15px; margin-bottom: 4px; }' +
            '.tu-card .tu-pie { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 6px; }' +
            '.tu-card .tu-barra { flex: 1; height: 6px; border-radius: 6px; background: #f3e3ea; overflow: hidden; }' +
            '.tu-card .tu-barra span { display: block; height: 100%; border-radius: 6px; background: #df6794; transition: width .4s ease; }' +
            '.tu-card .tu-omitir { padding: 4px 0; font-size: 13px; white-space: nowrap; }' +

            '@keyframes tu-pulso { 0%, 100% { box-shadow: 0 0 0 4px #df6794, 0 0 0 9999px rgba(36,18,56,.58); }' +
            '  50% { box-shadow: 0 0 0 8px rgba(223,103,148,.55), 0 0 0 9999px rgba(36,18,56,.58); } }' +
            '@keyframes tu-entra { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }' +
            '@keyframes tu-latido { 0%, 100% { transform: scale(1); } 8% { transform: scale(1.12); } 20% { transform: scale(1); }' +
            '  50% { transform: scale(1); } 58% { transform: scale(1.12); } 70% { transform: scale(1); } }' +
            '@keyframes tu-girar { to { transform: rotate(360deg); } }' +

            '@media (max-width: 640px) {' +
            '  .tu-card { left: 12px !important; right: 12px !important; width: auto; max-width: none; top: auto !important; bottom: 12px !important; }' +
            '}' +
            '@media (prefers-reduced-motion: reduce) {' +
            '  .tu-foco, .tu-card, .tu-bienvenida img { animation: none !important; transition: none !important; }' +
            '  .tu-pantalla { transition-duration: .01s; }' +
            '}';

        document.head.appendChild(estilo);
    }

    /* ---------- Pantallas grandes: bienvenida y final ---------- */

    var elementos = [];

    function limpiarUI() {
        elementos.forEach(function (nodo) {
            if (nodo.parentNode) nodo.parentNode.removeChild(nodo);
        });
        elementos = [];
    }

    function terminar() {
        marcarHecho(usuarioActual());
        borrar();
        limpiarUI();
    }

    function pantallaGrande(config) {
        limpiarUI();
        estilos();

        var pantalla = document.createElement('div');
        pantalla.className = 'tu-pantalla';
        pantalla.setAttribute('role', 'dialog');
        pantalla.setAttribute('aria-modal', 'true');
        pantalla.setAttribute('aria-label', config.titulo);

        var tarjeta = document.createElement('div');
        tarjeta.className = 'tu-bienvenida';

        var imagen = document.createElement('img');
        imagen.className = config.claseImagen;
        imagen.src = config.imagen;
        imagen.alt = '';

        var titulo = document.createElement('h2');
        titulo.textContent = config.titulo;

        var texto = document.createElement('p');
        texto.textContent = config.texto;

        var nota = document.createElement('p');
        nota.className = 'tu-nota';
        nota.textContent = config.nota;

        var botones = document.createElement('div');
        botones.className = 'tu-botones';

        var principal = document.createElement('button');
        principal.type = 'button';
        principal.className = 'tu-btn';
        principal.textContent = config.boton;
        principal.addEventListener('click', config.alPrincipal);
        botones.appendChild(principal);

        if (config.botonSecundario) {
            var secundario = document.createElement('button');
            secundario.type = 'button';
            secundario.className = 'tu-omitir';
            secundario.textContent = config.botonSecundario;
            secundario.addEventListener('click', config.alSecundario);
            botones.appendChild(secundario);
        }

        tarjeta.appendChild(imagen);
        tarjeta.appendChild(titulo);
        tarjeta.appendChild(texto);
        tarjeta.appendChild(nota);
        tarjeta.appendChild(botones);
        pantalla.appendChild(tarjeta);
        document.body.appendChild(pantalla);
        elementos.push(pantalla);

        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                pantalla.classList.add('tu-visible');
                principal.focus();
            });
        });
    }

    function mostrarBienvenida(estado) {
        var tienda = nombreTienda();

        pantallaGrande({
            claseImagen: 'tu-casa',
            imagen: '/images/anim-casa.png',
            titulo: tienda ? '¡Bienvenido a ' + tienda + '!' : '¡Bienvenido a tu tienda!',
            texto: 'Te mostramos cómo funciona Eternal Sunshine con un recorrido corto: crearás un producto, '
                + 'recibirás mercancía y harás tu primera venta.',
            nota: 'Son unos 3 minutos. Puedes omitirlo cuando quieras.',
            boton: 'Empezar el recorrido',
            botonSecundario: 'Ahora no',
            alPrincipal: function () {
                estado.paso = 1;
                guardar(estado);
                mostrarPaso(estado);
            },
            alSecundario: terminar
        });
    }

    function mostrarFinal() {
        pantallaGrande({
            claseImagen: 'tu-sol',
            imagen: '/images/anim-sol.png',
            titulo: '¡Listo! Ya puedes usar tu inventario',
            texto: 'Ya sabes crear productos, recibir mercancía y vender. Todo queda registrado en el historial.',
            nota: 'Lo que se anota, no se olvida.',
            boton: 'Ir al dashboard',
            alPrincipal: function () {
                terminar();
                window.location.href = '/';
            }
        });
    }

    /* ---------- Paso con foco y tarjeta ---------- */

    var enCurso = null;

    function visible(nodo) {
        if (!nodo) return false;

        var caja = nodo.getBoundingClientRect();

        if (caja.width === 0 || caja.height === 0) return false;
        if (caja.right <= 0 || caja.left >= window.innerWidth) return false;

        return true;
    }

    function colocar() {
        if (!enCurso) return;

        var foco = enCurso.foco;
        var tarjeta = enCurso.tarjeta;
        var objetivo = enCurso.objetivo ? document.querySelector(enCurso.objetivo) : null;

        if (!visible(objetivo)) {
            foco.style.display = 'none';
            tarjeta.style.left = '';
            tarjeta.style.top = '';
            tarjeta.style.right = '20px';
            tarjeta.style.bottom = '20px';
            enCurso.sinFoco = true;
            return;
        }

        enCurso.sinFoco = false;

        var margen = 8;
        var caja = objetivo.getBoundingClientRect();

        foco.style.display = 'block';
        foco.style.top = (caja.top - margen) + 'px';
        foco.style.left = (caja.left - margen) + 'px';
        foco.style.width = (caja.width + margen * 2) + 'px';
        foco.style.height = (caja.height + margen * 2) + 'px';

        var anchoTarjeta = tarjeta.offsetWidth;
        var altoTarjeta = tarjeta.offsetHeight;
        var ancho = window.innerWidth;
        var alto = window.innerHeight;

        tarjeta.style.right = 'auto';
        tarjeta.style.bottom = 'auto';

        var grande = caja.height > alto * 0.55 || caja.width > ancho * 0.7;

        if (!grande && caja.right + 18 + anchoTarjeta <= ancho) {
            tarjeta.style.left = (caja.right + 18) + 'px';
            tarjeta.style.top = Math.max(12, Math.min(caja.top, alto - altoTarjeta - 12)) + 'px';
        } else if (!grande && caja.bottom + 16 + altoTarjeta <= alto) {
            tarjeta.style.left = Math.max(12, Math.min(caja.left, ancho - anchoTarjeta - 12)) + 'px';
            tarjeta.style.top = (caja.bottom + 16) + 'px';
        } else if (!grande && caja.top - 16 - altoTarjeta >= 0) {
            tarjeta.style.left = Math.max(12, Math.min(caja.left, ancho - anchoTarjeta - 12)) + 'px';
            tarjeta.style.top = (caja.top - 16 - altoTarjeta) + 'px';
        } else {
            tarjeta.style.left = 'auto';
            tarjeta.style.top = 'auto';
            tarjeta.style.right = '20px';
            tarjeta.style.bottom = '20px';
        }
    }

    function mostrarPaso(estado) {
        limpiarUI();
        estilos();

        var paso = PASOS[estado.paso];

        if (!paso) {
            return;
        }

        var p = ruta();
        var aqui = paso.en(p);

        var foco = document.createElement('div');
        foco.className = 'tu-foco';
        foco.style.display = 'none';

        var tarjeta = document.createElement('div');
        tarjeta.className = 'tu-card';
        tarjeta.setAttribute('role', 'status');
        tarjeta.setAttribute('aria-live', 'polite');

        var contador = document.createElement('p');
        contador.className = 'tu-paso';
        contador.textContent = 'Paso ' + estado.paso + ' de ' + TOTAL;

        var titulo = document.createElement('h3');
        titulo.textContent = paso.titulo;

        var texto = document.createElement('p');
        texto.className = 'tu-texto';
        texto.textContent = aqui ? paso.texto : paso.texto + ' (Esto se hace en otra pantalla.)';

        tarjeta.appendChild(contador);
        tarjeta.appendChild(titulo);
        tarjeta.appendChild(texto);

        // «Siguiente» siempre está: el recorrido avanza solo cuando detecta la acción, pero nunca se queda trabado
        var boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'tu-btn';
        boton.textContent = estado.paso === 8 ? 'Terminar \u2192' : 'Siguiente \u2192';
        boton.addEventListener('click', function () {
            siguientePaso(estado);
        });
        tarjeta.appendChild(boton);

        var pie = document.createElement('div');
        pie.className = 'tu-pie';

        var barra = document.createElement('div');
        barra.className = 'tu-barra';
        var relleno = document.createElement('span');
        relleno.style.width = Math.round(((estado.paso - 1) / TOTAL) * 100) + '%';
        barra.appendChild(relleno);

        var omitir = document.createElement('button');
        omitir.type = 'button';
        omitir.className = 'tu-omitir';
        omitir.textContent = 'Omitir tutorial';
        omitir.addEventListener('click', function () {
            enCurso = null;
            terminar();
        });

        pie.appendChild(barra);
        pie.appendChild(omitir);
        tarjeta.appendChild(pie);

        document.body.appendChild(foco);
        document.body.appendChild(tarjeta);
        elementos.push(foco);
        elementos.push(tarjeta);

        enCurso = { foco: foco, tarjeta: tarjeta, objetivo: aqui ? paso.objetivo : null, boton: boton, paso: paso, aqui: aqui };

        var objetivo = aqui ? document.querySelector(paso.objetivo) : null;

        if (objetivo && typeof objetivo.scrollIntoView === 'function') {
            try {
                objetivo.scrollIntoView({ block: 'center', behavior: 'auto' });
            } catch (e) { /* nada */ }
        }

        // Paso 3: se anota que el formulario se envió para saber que el producto quedó guardado
        if (estado.paso === 3) {
            var formulario = document.querySelector('form[action="/productos/guardar"]');
            if (formulario) {
                formulario.addEventListener('submit', function () {
                    estado.enviado = true;
                    guardar(estado);
                });
            }
        }

        colocar();
    }

    function reposicionar() {
        if (!enCurso) return;

        colocar();
    }

    function siguientePaso(estado) {
        var n = estado.paso;

        // Del paso 7 (la venta) se pasa al cierre; del 8 (el recibo) también
        if (n >= 7) {
            estado.paso = 9;
            estado.enviado = false;
            guardar(estado);
            mostrarFinal();
            return;
        }

        estado.paso = n + 1;
        estado.enviado = false;
        guardar(estado);

        var nuevo = PASOS[estado.paso];

        // Si el siguiente paso pertenece a otra pantalla, se lleva a la persona allí
        if (nuevo.destino && !nuevo.en(ruta())) {
            window.location.href = nuevo.destino;
            return;
        }

        mostrarPaso(estado);
    }

    /* ---------- Arranque ---------- */

    function iniciar() {
        if (!document.querySelector('.sidebar')) return;

        if (/[?&]tutorial\b/.test(window.location.search)) {
            guardar({ paso: 0, u: usuarioActual() });

            try {
                var limpia = window.location.pathname
                    + window.location.search.replace(/([?&])tutorial\b=?[^&]*&?/, '$1').replace(/[?&]$/, '')
                    + window.location.hash;
                window.history.replaceState(null, '', limpia);
            } catch (e) { /* nada */ }
        }

        var estado = leer();
        var yo = usuarioActual();

        // Cuentas de administrador que aún no han visto el recorrido: se les ofrece al entrar al dashboard
        if (!estado && yo && ruta() === '/' && esAdministrador() && !yaHecho(yo)
                && !/[?&]demo/.test(window.location.search)) {
            estado = { paso: 0, u: yo };
        }

        if (!estado || typeof estado.paso !== 'number') return;

        // El recorrido es de quien creó la tienda: si en este navegador entra otra persona, se descarta
        if (estado.u && yo && estado.u !== yo) {
            borrar();
            return;
        }

        if (!estado.u && yo) {
            estado.u = yo;
        }

        if (estado.paso === 0) {
            guardar(estado);
            mostrarBienvenida(estado);
            return;
        }

        if (estado.paso >= 9) {
            mostrarFinal();
            return;
        }

        estado = avanzar(estado);
        guardar(estado);
        mostrarPaso(estado);

        window.addEventListener('resize', reposicionar);
        window.addEventListener('scroll', reposicionar, true);
        window.setInterval(reposicionar, 600);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();