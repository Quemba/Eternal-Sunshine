(function (global) {
    'use strict';

    let abierto = false;

    function crear(tag, clase, texto) {
        const e = document.createElement(tag);
        if (clase) e.className = clase;
        if (texto) e.textContent = texto;
        return e;
    }

    function preguntar(opciones, alConfirmar, alCancelar) {
        if (abierto) return;
        abierto = true;

        const anterior = document.activeElement;

        const capa = crear('div', 'confirmar-capa');
        const caja = crear('div', 'confirmar-caja');
        caja.setAttribute('role', 'dialog');
        caja.setAttribute('aria-modal', 'true');

        const icono = crear('div', 'confirmar-icono');
        const nombreIcono = opciones.icono || 'ti-help-circle';

        if (/^ti-/.test(nombreIcono)) {
            const glifo = document.createElement('i');
            glifo.className = 'ti ' + nombreIcono;
            glifo.setAttribute('aria-hidden', 'true');
            icono.appendChild(glifo);
        } else {
            icono.textContent = nombreIcono;
        }

        const titulo = crear('h3', 'confirmar-titulo', opciones.titulo || '¿Estás seguro?');
        const mensaje = crear('p', 'confirmar-mensaje', opciones.mensaje || '');
        const botones = crear('div', 'confirmar-botones');

        const cancelar = crear('button', 'confirmar-btn confirmar-cancelar', opciones.textoCancelar || 'Cancelar');
        cancelar.type = 'button';

        const aceptar = crear('button', 'confirmar-btn confirmar-aceptar', opciones.textoConfirmar || 'Aceptar');
        aceptar.type = 'button';

        if (opciones.tipo === 'peligro') {
            aceptar.classList.add('confirmar-peligro');
            icono.classList.add('confirmar-icono-peligro');
        } else if (opciones.tipo === 'exito') {
            aceptar.classList.add('confirmar-exito');
            icono.classList.add('confirmar-icono-exito');
        }

        botones.appendChild(cancelar);
        botones.appendChild(aceptar);

        caja.appendChild(icono);
        caja.appendChild(titulo);
        caja.appendChild(mensaje);
        caja.appendChild(botones);
        capa.appendChild(caja);

        function cerrar(confirmado) {
            document.removeEventListener('keydown', teclas, true);

            if (capa.parentNode) {
                capa.parentNode.removeChild(capa);
            }

            abierto = false;

            if (anterior && anterior.focus) {
                anterior.focus();
            }

            if (!confirmado && alCancelar) {
                alCancelar();
            }
        }

        function teclas(e) {
            if (e.key === 'Escape') {
                e.preventDefault();
                cerrar(false);
                return;
            }

            if (e.key === 'Tab') {
                e.preventDefault();

                const focos = [cancelar, aceptar];
                const actual = focos.indexOf(document.activeElement);
                let siguiente;

                if (e.shiftKey) {
                    siguiente = actual <= 0 ? focos.length - 1 : actual - 1;
                } else {
                    siguiente = actual === focos.length - 1 ? 0 : actual + 1;
                }

                focos[siguiente].focus();
            }
        }

        cancelar.addEventListener('click', function () {
            cerrar(false);
        });

        aceptar.addEventListener('click', function () {
            cerrar(true);
            if (alConfirmar) alConfirmar();
        });

        capa.addEventListener('click', function (e) {
            if (e.target === capa) {
                cerrar(false);
            }
        });

        document.body.appendChild(capa);
        document.addEventListener('keydown', teclas, true);

        if (opciones.enfocarAceptar) {
            aceptar.focus();
        } else {
            cancelar.focus();
        }
    }

    function textosDe(elemento) {
        return {
            titulo: elemento.getAttribute('data-titulo'),
            mensaje: elemento.getAttribute('data-confirmar'),
            textoConfirmar: elemento.getAttribute('data-boton'),
            icono: elemento.getAttribute('data-icono'),
            tipo: elemento.getAttribute('data-tipo')
        };
    }

    document.addEventListener('click', function (e) {
        const enlace = e.target && e.target.closest ? e.target.closest('a[data-confirmar]') : null;

        if (!enlace) return;

        e.preventDefault();

        const destino = enlace.getAttribute('href');

        preguntar(textosDe(enlace), function () {
            if (destino) {
                global.location.href = destino;
            }
        });
    });

    document.addEventListener('submit', function (e) {
        const formulario = e.target;

        if (!formulario || !formulario.hasAttribute || !formulario.hasAttribute('data-confirmar')) return;

        e.preventDefault();

        preguntar(textosDe(formulario), function () {
            formulario.submit();
        });
    });

    global.Confirmar = { preguntar: preguntar };
})(window);