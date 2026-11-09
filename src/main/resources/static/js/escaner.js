(function (global) {
    'use strict';

    const UMBRAL_ERROR = 0.15;
    const LECTURAS_REQUERIDAS = 3;
    const VENTANA_MS = 3000;
    const BLOQUEO_MS = 900;
    const BLOQUEO_RECHAZO_MS = 2500;
    const CONFIRMACION_MS = 20000;
    const DESTELLO_MS = 500;
    const BANDA_ARRIBA = 0.25;
    const BANDA_ABAJO = 0.75;
    const BANDA_IZQUIERDA = 0.03;
    const BANDA_DERECHA = 0.97;

    let contextoAudio = null;

    function prepararAudio() {
        try {
            const Contexto = global.AudioContext || global.webkitAudioContext;
            if (!Contexto) return;

            if (!contextoAudio || contextoAudio.state === 'closed') {
                contextoAudio = new Contexto();
            }

            if (contextoAudio.state === 'suspended') {
                contextoAudio.resume();
            }
        } catch (e) { }
    }

    function tono(frecuencia, retraso, duracion, tipo, volumen) {
        const ctx = contextoAudio;
        const inicio = ctx.currentTime + 0.02 + retraso;

        const osc = ctx.createOscillator();
        const vol = ctx.createGain();

        osc.type = tipo;
        osc.frequency.setValueAtTime(frecuencia, inicio);

        vol.gain.setValueAtTime(0.0001, inicio);
        vol.gain.exponentialRampToValueAtTime(volumen, inicio + 0.01);
        vol.gain.exponentialRampToValueAtTime(0.0001, inicio + duracion);

        osc.connect(vol);
        vol.connect(ctx.destination);

        osc.start(inicio);
        osc.stop(inicio + duracion + 0.02);
    }

    function pitido() {
        try {
            prepararAudio();
            if (!contextoAudio) return;
            tono(1046, 0, 0.18, 'sine', 0.3);
        } catch (e) { }
    }

    function pitidoAviso() {
        try {
            prepararAudio();
            if (!contextoAudio) return;
            tono(660, 0, 0.12, 'sine', 0.22);
        } catch (e) { }
    }

    function pitidoError() {
        try {
            prepararAudio();
            if (!contextoAudio) return;
            tono(196, 0, 0.16, 'sawtooth', 0.18);
            tono(147, 0.2, 0.22, 'sawtooth', 0.18);
        } catch (e) { }
    }

    function elemento(tag, clase, texto) {
        const e = document.createElement(tag);
        if (clase) e.className = clase;
        if (texto) e.textContent = texto;
        return e;
    }

    function errorPromedio(resultado) {
        const partes = resultado.codeResult.decodedCodes || [];
        let suma = 0;
        let cuenta = 0;

        partes.forEach(function (parte) {
            if (typeof parte.error === 'number') {
                suma += parte.error;
                cuenta++;
            }
        });

        return cuenta > 0 ? suma / cuenta : 1;
    }

    function dentroDeLaGuia(resultado) {
        try {
            const lienzo = Quagga.canvas.dom.overlay;
            const ancho = parseInt(lienzo.getAttribute('width'), 10);
            const alto = parseInt(lienzo.getAttribute('height'), 10);
            const caja = resultado.box;

            if (!caja || !caja.length || !ancho || !alto) return true;

            let sumaX = 0;
            let sumaY = 0;
            caja.forEach(function (punto) {
                sumaX += punto[0];
                sumaY += punto[1];
            });

            const cx = sumaX / caja.length / ancho;
            const cy = sumaY / caja.length / alto;

            return cy >= BANDA_ARRIBA && cy <= BANDA_ABAJO && cx >= BANDA_IZQUIERDA && cx <= BANDA_DERECHA;
        } catch (e) {
            return true;
        }
    }

    function crear(opciones) {
        const montaje = opciones.montaje;

        let activo = false;
        let iniciando = false;
        let pausado = false;
        let lecturas = [];
        let confirmados = {};
        let bloqueoCodigo = '';
        let bloqueoVisto = 0;
        let bloqueoDuracion = BLOQUEO_MS;
        let primerCodigo = '';
        let primerTiempo = 0;
        let destelloTimer = null;
        let refs = {};

        function avisarError(mensaje) {
            if (opciones.alError) opciones.alError(mensaje);
        }

        function estado(texto) {
            if (refs.lectura) refs.lectura.textContent = texto;
        }

        function destello(tipo) {
            try {
                if (!refs.visor) return;

                refs.visor.classList.remove('escaner-ok', 'escaner-error');
                refs.visor.classList.add(tipo === 'error' ? 'escaner-error' : 'escaner-ok');

                clearTimeout(destelloTimer);
                destelloTimer = setTimeout(function () {
                    if (refs.visor) {
                        refs.visor.classList.remove('escaner-ok', 'escaner-error');
                    }
                }, DESTELLO_MS);
            } catch (e) { }
        }

        function aceptar(codigo) {
            const ahora = Date.now();

            lecturas = [];
            bloqueoCodigo = codigo;
            bloqueoVisto = ahora;
            bloqueoDuracion = BLOQUEO_MS;

            if (opciones.confirmarDosVeces) {
                const vigente = primerCodigo !== '' && (ahora - primerTiempo) <= CONFIRMACION_MS;

                if (!vigente || primerCodigo !== codigo) {
                    primerCodigo = codigo;
                    primerTiempo = ahora;
                    estado('Primera lectura: ' + codigo + '. Apártalo un momento y vuélvelo a mostrar para confirmar.');
                    if (opciones.alPrimeraLectura) opciones.alPrimeraLectura(codigo);
                    return;
                }

                primerCodigo = '';
            }

            let resultado = true;

            if (opciones.alLeer) {
                try {
                    resultado = opciones.alLeer(codigo);
                } catch (e) {
                    resultado = false;
                }
            }

            if (resultado === false) {
                delete confirmados[codigo];
                bloqueoDuracion = BLOQUEO_RECHAZO_MS;
                estado('Código leído: ' + codigo + ' · no se agregó');
                destello('error');
                return;
            }

            if (resultado === 'pendiente') {
                estado('Código leído: ' + codigo);
                return;
            }

            confirmados[codigo] = true;
            estado('Código leído: ' + codigo);
            destello('ok');
        }

        function alDetectar(resultado) {
            try {
                if (pausado) return;
                if (!resultado || !resultado.codeResult || !resultado.codeResult.code) return;

                const codigo = String(resultado.codeResult.code).trim();
                const ahora = Date.now();

                if (codigo === bloqueoCodigo && ahora - bloqueoVisto < bloqueoDuracion) {
                    bloqueoVisto = ahora;
                    return;
                }

                if (!dentroDeLaGuia(resultado)) {
                    return;
                }

                if (errorPromedio(resultado) > UMBRAL_ERROR) {
                    return;
                }

                if (confirmados[codigo] && !opciones.confirmarDosVeces) {
                    aceptar(codigo);
                    return;
                }

                lecturas.push({ codigo: codigo, t: ahora });
                lecturas = lecturas.filter(function (l) { return ahora - l.t <= VENTANA_MS; });

                const veces = lecturas.filter(function (l) { return l.codigo === codigo; }).length;

                if (veces < LECTURAS_REQUERIDAS) {
                    estado('Leyendo código…');
                    return;
                }

                aceptar(codigo);
            } catch (e) { }
        }

        function dibujar(resultado) {
            try {
                const ctx = Quagga.canvas.ctx.overlay;
                const lienzo = Quagga.canvas.dom.overlay;

                if (!resultado || !ctx || !lienzo) return;

                if (resultado.boxes) {
                    ctx.clearRect(0, 0, parseInt(lienzo.getAttribute('width'), 10), parseInt(lienzo.getAttribute('height'), 10));
                    resultado.boxes.filter(function (b) { return b !== resultado.box; }).forEach(function (b) {
                        Quagga.ImageDebug.drawPath(b, { x: 0, y: 1 }, ctx, { color: 'green', lineWidth: 2 });
                    });
                }

                if (resultado.box) {
                    Quagga.ImageDebug.drawPath(resultado.box, { x: 0, y: 1 }, ctx, { color: '#2255ff', lineWidth: 2 });
                }

                if (resultado.codeResult && resultado.codeResult.code) {
                    Quagga.ImageDebug.drawPath(resultado.line, { x: 'x', y: 'y' }, ctx, { color: 'red', lineWidth: 3 });
                }
            } catch (e) { }
        }

        function construir() {
            montaje.innerHTML = '';

            const raiz = elemento('div', 'escaner');
            const visor = elemento('div', 'escaner-visor');
            const lector = elemento('div', 'escaner-lector');

            visor.appendChild(lector);
            visor.appendChild(elemento('div', 'escaner-guia'));
            raiz.appendChild(visor);

            refs = {};
            refs.visor = visor;
            refs.lector = lector;
            refs.lectura = elemento('p', 'escaner-lectura');
            refs.cerrar = elemento('button', 'escaner-cerrar', 'Cerrar cámara');
            refs.cerrar.type = 'button';
            refs.cerrar.disabled = true;
            refs.cerrar.addEventListener('click', cerrar);

            raiz.appendChild(refs.lectura);
            raiz.appendChild(refs.cerrar);
            montaje.appendChild(raiz);
        }

        function cerrar() {
            try {
                if (activo) {
                    Quagga.offDetected(alDetectar);
                    Quagga.offProcessed(dibujar);
                    Quagga.stop();
                }
            } catch (e) { }

            clearTimeout(destelloTimer);

            montaje.innerHTML = '';
            montaje.style.display = 'none';

            activo = false;
            iniciando = false;
            pausado = false;
            lecturas = [];
            confirmados = {};
            bloqueoCodigo = '';
            bloqueoVisto = 0;
            bloqueoDuracion = BLOQUEO_MS;
            primerCodigo = '';
            primerTiempo = 0;
            refs = {};

            if (opciones.alCerrar) opciones.alCerrar();
        }

        function abrir() {
            if (activo || iniciando) return;

            prepararAudio();

            if (typeof Quagga === 'undefined') {
                avisarError('No se pudo cargar el lector de códigos.');
                return;
            }

            if (!global.isSecureContext) {
                avisarError('La cámara solo funciona desde localhost o HTTPS.');
                return;
            }

            iniciando = true;
            construir();
            montaje.style.display = 'block';

            if (opciones.alIniciar) opciones.alIniciar();

            Quagga.init({
                inputStream: {
                    name: 'Live',
                    type: 'LiveStream',
                    target: refs.lector,
                    size: 1280,
                    constraints: {
                        facingMode: 'environment',
                        width: { ideal: 1280 },
                        height: { ideal: 720 }
                    }
                },
                locator: { patchSize: 'medium', halfSample: true },
                numOfWorkers: 0,
                frequency: 8,
                locate: true,
                decoder: {
                    readers: ['ean_reader']
                }
            }, function (error) {
                iniciando = false;

                if (error) {
                    cerrar();
                    avisarError('No se pudo abrir la cámara. Revisa el permiso del navegador y que ninguna otra aplicación la esté usando.');
                    return;
                }

                Quagga.onDetected(alDetectar);
                Quagga.onProcessed(dibujar);
                Quagga.start();

                activo = true;
                if (refs.cerrar) refs.cerrar.disabled = false;

                if (opciones.alActivar) opciones.alActivar();
            });
        }

        function pausar() {
            pausado = true;
            lecturas = [];
        }

        function reanudar() {
            pausado = false;
            lecturas = [];
            bloqueoVisto = Date.now();
            bloqueoDuracion = BLOQUEO_RECHAZO_MS;
        }

        return {
            abrir: abrir,
            cerrar: cerrar,
            pausar: pausar,
            reanudar: reanudar,
            marcar: function (tipo) { destello(tipo); },
            estaActivo: function () { return activo || iniciando; }
        };
    }

    global.EscanerCodigo = {
        crear: crear,
        pitido: pitido,
        pitidoAviso: pitidoAviso,
        pitidoError: pitidoError
    };
})(window);