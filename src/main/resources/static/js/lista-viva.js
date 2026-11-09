(function (global) {
    'use strict';

    function iniciar(opciones) {
        var formulario = document.querySelector(opciones.formulario);
        var zona = document.querySelector(opciones.zona);
        var base = opciones.url;
        var retardo = opciones.retardo || 250;

        if (!formulario || !zona || !base) return;

        var botonLimpiar = formulario.querySelector('[data-limpiar]');
        var campos = formulario.querySelectorAll('input, select');

        var temporizador = null;
        var contador = 0;
        var controlador = null;

        function hayFiltros() {
            var alguno = false;

            campos.forEach(function (campo) {
                if (campo.value.trim() !== '') {
                    alguno = true;
                }
            });

            return alguno;
        }

        function actualizarLimpiar() {
            if (botonLimpiar) {
                botonLimpiar.hidden = !hayFiltros();
            }
        }

        function construirUrl(pagina) {
            var parametros = new URLSearchParams();

            new FormData(formulario).forEach(function (valor, nombre) {
                var texto = String(valor).trim();

                if (texto !== '') {
                    parametros.append(nombre, texto);
                }
            });

            if (pagina && pagina > 1) {
                parametros.set('pagina', String(pagina));
            }

            var consulta = parametros.toString();
            return base + (consulta !== '' ? '?' + consulta : '');
        }

        async function cargar(destino, desplazar) {
            var numero = ++contador;

            if (controlador) {
                controlador.abort();
            }

            controlador = new AbortController();
            zona.classList.add('cargando');

            try {
                var respuesta = await fetch(destino, {
                    headers: { 'Accept': 'text/html' },
                    credentials: 'same-origin',
                    signal: controlador.signal
                });

                if (!respuesta.ok) {
                    throw new Error('Estado ' + respuesta.status);
                }

                var html = await respuesta.text();

                if (numero !== contador) {
                    return;
                }

                var documento = new DOMParser().parseFromString(html, 'text/html');
                var nueva = documento.querySelector(opciones.zona);

                if (!nueva) {
                    window.location.href = destino;
                    return;
                }

                zona.innerHTML = nueva.innerHTML;
                history.replaceState(null, '', destino);

                if (desplazar) {
                    zona.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }

                if (typeof opciones.alActualizar === 'function') {
                    opciones.alActualizar(destino);
                }

            } catch (e) {
                if (e.name === 'AbortError') {
                    return;
                }

                window.location.href = destino;

            } finally {
                if (numero === contador) {
                    zona.classList.remove('cargando');
                }
            }
        }

        function limpiarTodo() {
            campos.forEach(function (campo) {
                campo.value = '';
            });

            actualizarLimpiar();
            clearTimeout(temporizador);
            cargar(base, false);
        }

        campos.forEach(function (campo) {
            var inmediato = campo.tagName === 'SELECT' || campo.type === 'date';

            campo.addEventListener(inmediato ? 'change' : 'input', function () {
                actualizarLimpiar();
                clearTimeout(temporizador);

                if (inmediato) {
                    cargar(construirUrl(1), false);
                } else {
                    temporizador = setTimeout(function () {
                        cargar(construirUrl(1), false);
                    }, retardo);
                }
            });
        });

        formulario.addEventListener('submit', function (e) {
            e.preventDefault();
            clearTimeout(temporizador);
            cargar(construirUrl(1), false);
        });

        if (botonLimpiar) {
            botonLimpiar.addEventListener('click', function () {
                limpiarTodo();

                var primerTexto = formulario.querySelector('input[type="text"]');
                if (primerTexto) {
                    primerTexto.focus();
                }
            });
        }

        zona.addEventListener('click', function (e) {
            var enlace = e.target.closest ? e.target.closest('a') : null;

            if (!enlace) return;

            var pagina = enlace.getAttribute('data-pagina');

            if (pagina) {
                e.preventDefault();
                cargar(construirUrl(parseInt(pagina, 10)), true);
                return;
            }

            if (enlace.hasAttribute('data-limpiar-todo')) {
                e.preventDefault();
                limpiarTodo();
            }
        });

        actualizarLimpiar();
    }

    global.ListaViva = { iniciar: iniciar };
})(window);