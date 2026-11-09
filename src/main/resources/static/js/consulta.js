(function () {
    'use strict';

    if (window.__consultaRapidaIniciada) return;
    window.__consultaRapidaIniciada = true;

    function iniciar() {
        try {
            if (!document.querySelector('.sidebar .nav')) return;
            if (document.querySelector('.fab-consulta')) return;

            var ETIQUETAS = { disponible: 'Disponible', bajo: 'Stock bajo', agotado: 'Agotado' };
            var tactil = !!(window.matchMedia && window.matchMedia('(pointer: coarse)').matches);

            var fab = document.createElement('button');
            fab.type = 'button';
            fab.className = 'fab-consulta';
            fab.setAttribute('aria-haspopup', 'dialog');
            fab.setAttribute('aria-expanded', 'false');
            fab.setAttribute('aria-label', 'Consultar precio y stock');
            fab.innerHTML =
                '<img class="fab-img" src="/images/consulta-rapida.png" alt="" width="64" height="64" draggable="false">' +
                '<span class="fab-etiqueta">Consultar precio</span>';

            var imagenFab = fab.querySelector('.fab-img');

            imagenFab.addEventListener('error', function () {
                fab.classList.add('fab-sin-imagen');

                var icono = document.createElement('i');
                icono.className = 'ti ti-barcode';
                icono.setAttribute('aria-hidden', 'true');

                imagenFab.replaceWith(icono);
            });

            var capa = document.createElement('div');
            capa.className = 'consulta-capa';
            capa.hidden = true;

            var panel = document.createElement('section');
            panel.className = 'panel-consulta';
            panel.hidden = true;
            panel.setAttribute('role', 'dialog');
            panel.setAttribute('aria-label', 'Consultar precio y stock');
            panel.innerHTML =
                '<div class="pc-cabecera">' +
                '<h3><img class="pc-sol" src="/images/consulta-rapida.png" alt="" width="30" height="30"> Consultar precio</h3>' +
                '<button type="button" class="pc-cerrar" aria-label="Cerrar"><i class="ti ti-x" aria-hidden="true"></i></button>' +
                '</div>' +
                '<div class="pc-buscar">' +
                '<input type="text" id="pc-buscar" name="pc-buscar" class="pc-input" placeholder="Escanea o escribe código o nombre..." autocomplete="off" aria-label="Buscar producto">' +
                '<button type="button" class="pc-escanear"><i class="ti ti-scan" aria-hidden="true"></i><span>Escanear</span></button>' +
                '</div>' +
                '<div class="pc-camara" style="display:none;"></div>' +
                '<div class="pc-mensaje" role="alert" hidden></div>' +
                '<div class="pc-sugerencia" hidden>' +
                '<p class="pc-sug-texto"></p>' +
                '<div class="pc-sug-botones">' +
                '<button type="button" class="pc-sug-si">Sí, mostrarlo</button>' +
                '<button type="button" class="pc-sug-no">No</button>' +
                '</div>' +
                '</div>' +
                '<div class="pc-lista"></div>' +
                '<div class="pc-vacio"><i class="ti ti-tag" aria-hidden="true"></i><p>Escanea un producto o escribe su nombre para ver el precio y cuántas unidades hay.</p></div>' +
                '<div class="pc-tarjeta" aria-live="polite" hidden>' +
                '<span class="pc-categoria"></span>' +
                '<h4 class="pc-nombre"></h4>' +
                '<p class="pc-codigo"></p>' +
                '<div class="pc-precio"></div>' +
                '<div class="pc-stock"><span class="pc-estado"></span><strong class="pc-unidades"></strong></div>' +
                '</div>';

            var imagenPanel = panel.querySelector('.pc-sol');

            imagenPanel.addEventListener('error', function () {
                imagenPanel.style.display = 'none';
            });

            document.body.appendChild(fab);
            document.body.appendChild(capa);
            document.body.appendChild(panel);

            try {
                if (!sessionStorage.getItem('fab-consulta-visto')) {
                    sessionStorage.setItem('fab-consulta-visto', '1');

                    setTimeout(function () {
                        fab.classList.add('mostrar-etiqueta');
                    }, 900);

                    setTimeout(function () {
                        fab.classList.remove('mostrar-etiqueta');
                    }, 4800);
                }
            } catch (e) { }

            var el = {
                entrada: panel.querySelector('.pc-input'),
                escanear: panel.querySelector('.pc-escanear'),
                camara: panel.querySelector('.pc-camara'),
                mensaje: panel.querySelector('.pc-mensaje'),
                sugerencia: panel.querySelector('.pc-sugerencia'),
                sugTexto: panel.querySelector('.pc-sug-texto'),
                sugSi: panel.querySelector('.pc-sug-si'),
                sugNo: panel.querySelector('.pc-sug-no'),
                lista: panel.querySelector('.pc-lista'),
                vacio: panel.querySelector('.pc-vacio'),
                tarjeta: panel.querySelector('.pc-tarjeta'),
                categoria: panel.querySelector('.pc-categoria'),
                nombre: panel.querySelector('.pc-nombre'),
                codigo: panel.querySelector('.pc-codigo'),
                precio: panel.querySelector('.pc-precio'),
                estado: panel.querySelector('.pc-estado'),
                unidades: panel.querySelector('.pc-unidades'),
                cerrar: panel.querySelector('.pc-cerrar')
            };

            var abierto = false;
            var anterior = null;
            var escaner = null;
            var abriendoCamara = false;
            var actual = null;
            var ultimoExito = 0;
            var ultimoCodigo = '';
            var ultimoTiempo = 0;
            var numeroBusqueda = 0;
            var temporizador = null;
            var temporizadorMensaje = null;
            var sugerido = null;
            var sugeridoDeCamara = false;

            function fmt(n) {
                return '$ ' + String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
            }

            function sonido(nombre) {
                try {
                    if (typeof EscanerCodigo !== 'undefined' && typeof EscanerCodigo[nombre] === 'function') {
                        EscanerCodigo[nombre]();
                    }
                } catch (e) { }
            }

            function esperar(ms) {
                return new Promise(function (resolver) {
                    setTimeout(resolver, ms);
                });
            }

            function ocultarMensaje() {
                clearTimeout(temporizadorMensaje);
                el.mensaje.hidden = true;
            }

            function mostrarMensaje(texto) {
                el.mensaje.textContent = texto;
                el.mensaje.hidden = false;

                clearTimeout(temporizadorMensaje);
                temporizadorMensaje = setTimeout(function () {
                    el.mensaje.hidden = true;
                }, 4000);

                sonido('pitidoError');
            }

            function ponerEscanear(clase, texto) {
                el.escanear.innerHTML = '<i class="ti ' + clase + '" aria-hidden="true"></i><span>' + texto + '</span>';
            }

            function mostrarProducto(p) {
                actual = p;

                el.categoria.textContent = p.categoria || 'Sin categoría';
                el.nombre.textContent = p.nombre;
                el.codigo.textContent = p.codigo;
                el.precio.textContent = fmt(p.precio);

                el.estado.textContent = ETIQUETAS[p.estado] || '';
                el.estado.className = 'pc-estado estado-' + p.estado;

                el.unidades.textContent = p.cantidad + (p.cantidad === 1 ? ' unidad' : ' unidades');

                el.vacio.hidden = true;
                el.lista.innerHTML = '';

                el.tarjeta.hidden = true;
                void el.tarjeta.offsetWidth;
                el.tarjeta.hidden = false;
            }

            function ocultarSugerencia() {
                sugerido = null;
                el.sugerencia.hidden = true;
            }

            function mostrarSugerencia(leido, producto, deCamara) {
                sugerido = producto;
                sugeridoDeCamara = deCamara;

                el.sugTexto.textContent = 'Leí ' + leido + ', que no está registrado, pero termina igual que «' +
                    producto.nombre + '» (' + producto.codigo + '). ¿Es este?';
                el.sugerencia.hidden = false;

                if (deCamara && escaner) {
                    escaner.pausar();
                }

                sonido('pitidoAviso');
            }

            el.sugSi.addEventListener('click', function () {
                var producto = sugerido;
                var deCamara = sugeridoDeCamara;

                ocultarSugerencia();

                if (deCamara && escaner) {
                    escaner.reanudar();
                }

                if (producto) {
                    mostrarProducto(producto);
                    ultimoExito = Date.now();
                    el.entrada.value = '';
                    sonido('pitido');

                    if (deCamara && escaner) {
                        escaner.marcar('ok');
                    }
                }
            });

            el.sugNo.addEventListener('click', function () {
                var deCamara = sugeridoDeCamara;

                ocultarSugerencia();

                if (deCamara && escaner) {
                    escaner.reanudar();
                    escaner.marcar('error');
                }
            });

            async function pedir(url) {
                var respuesta = await fetch(url, {
                    headers: { 'Accept': 'application/json' },
                    credentials: 'same-origin'
                });

                var tipo = respuesta.headers.get('Content-Type') || '';

                if (tipo.indexOf('application/json') === -1) {
                    throw new Error('sesion');
                }

                return { estado: respuesta.status, datos: await respuesta.json() };
            }

            async function consultarCodigo(codigo, deCamara) {
                var respuesta;

                try {
                    respuesta = await pedir('/consulta/codigo?codigo=' + encodeURIComponent(codigo));
                } catch (e) {
                    mostrarMensaje('No se pudo consultar. Revisa tu conexión o vuelve a iniciar sesión.');

                    if (deCamara && escaner) {
                        escaner.marcar('error');
                    }

                    return;
                }

                var datos = respuesta.datos;

                if (datos.ok && datos.producto) {
                    var repetido = actual && actual.id === datos.producto.id && (Date.now() - ultimoExito) < 4000;

                    mostrarProducto(datos.producto);
                    ultimoExito = Date.now();
                    el.entrada.value = '';
                    ocultarMensaje();

                    if (!repetido) {
                        sonido('pitido');

                        if (deCamara && escaner) {
                            escaner.marcar('ok');
                        }
                    }

                    return;
                }

                if (datos.sugerencia) {
                    mostrarSugerencia(codigo, datos.sugerencia, deCamara);
                    return;
                }

                mostrarMensaje(datos.mensaje || 'Producto no encontrado.');

                if (deCamara && escaner) {
                    escaner.marcar('error');
                }
            }

            function pintarLista(productos) {
                el.lista.innerHTML = '';

                if (productos.length === 0) {
                    var vacia = document.createElement('div');
                    vacia.className = 'pc-sin-resultados';
                    vacia.textContent = 'No se encontraron productos.';
                    el.lista.appendChild(vacia);
                    return;
                }

                productos.forEach(function (p) {
                    var fila = document.createElement('button');
                    fila.type = 'button';
                    fila.className = 'pc-item';

                    var info = document.createElement('div');
                    var nombre = document.createElement('strong');
                    nombre.textContent = p.nombre;
                    var detalle = document.createElement('small');
                    detalle.textContent = p.codigo + ' · ' + p.cantidad + (p.cantidad === 1 ? ' unidad' : ' unidades');

                    info.appendChild(nombre);
                    info.appendChild(detalle);

                    var precio = document.createElement('span');
                    precio.className = 'pc-item-precio';
                    precio.textContent = fmt(p.precio);

                    fila.appendChild(info);
                    fila.appendChild(precio);

                    fila.addEventListener('click', function () {
                        mostrarProducto(p);
                        el.entrada.value = '';
                        ocultarMensaje();
                    });

                    el.lista.appendChild(fila);
                });
            }

            async function buscarTexto(texto) {
                var numero = ++numeroBusqueda;

                try {
                    var respuesta = await pedir('/consulta/buscar?q=' + encodeURIComponent(texto));

                    if (numero !== numeroBusqueda) return;

                    pintarLista(respuesta.datos.productos || []);

                } catch (e) {
                    if (numero === numeroBusqueda) {
                        mostrarMensaje('No se pudo buscar. Revisa tu conexión o vuelve a iniciar sesión.');
                    }
                }
            }

            el.entrada.addEventListener('input', function () {
                clearTimeout(temporizador);

                var texto = el.entrada.value.trim();

                if (texto === '') {
                    numeroBusqueda++;
                    el.lista.innerHTML = '';
                    return;
                }

                temporizador = setTimeout(function () {
                    buscarTexto(texto);
                }, 220);
            });

            el.entrada.addEventListener('keydown', async function (e) {
                if (e.key !== 'Enter') return;

                e.preventDefault();

                var texto = el.entrada.value.trim();

                if (texto === '') return;

                clearTimeout(temporizador);

                if (/^\d{6,14}$/.test(texto)) {
                    consultarCodigo(texto, false);
                    return;
                }

                try {
                    var respuesta = await pedir('/consulta/buscar?q=' + encodeURIComponent(texto));
                    var productos = respuesta.datos.productos || [];

                    if (productos.length === 0) {
                        mostrarMensaje('No se encontró ningún producto.');
                    } else {
                        mostrarProducto(productos[0]);
                        el.entrada.value = '';
                    }

                } catch (error) {
                    mostrarMensaje('No se pudo buscar. Revisa tu conexión o vuelve a iniciar sesión.');
                }
            });

            function cargarScript(src) {
                return new Promise(function (resolver, rechazar) {
                    var existente = document.querySelector('script[src="' + src + '"]');

                    if (existente) {
                        resolver();
                        return;
                    }

                    var script = document.createElement('script');
                    script.src = src;
                    script.onload = resolver;
                    script.onerror = rechazar;
                    document.head.appendChild(script);
                });
            }

            function cargarEstilo(href) {
                if (document.querySelector('link[href="' + href + '"]')) return;

                var enlace = document.createElement('link');
                enlace.rel = 'stylesheet';
                enlace.href = href;
                document.head.appendChild(enlace);
            }

            async function abrirCamara() {
                if (abriendoCamara) return;
                if (escaner && escaner.estaActivo()) return;

                abriendoCamara = true;

                try {
                    var cerradas = false;

                    document.querySelectorAll('.escaner-cerrar').forEach(function (boton) {
                        if (!panel.contains(boton) && !boton.disabled) {
                            boton.click();
                            cerradas = true;
                        }
                    });

                    if (cerradas) {
                        await esperar(350);
                    }

                    cargarEstilo('/css/escaner.css');

                    if (typeof Quagga === 'undefined') {
                        await cargarScript('/js/quagga.min.js');
                    }

                    if (typeof EscanerCodigo === 'undefined') {
                        await cargarScript('/js/escaner.js');
                    }

                    if (!escaner) {
                        escaner = EscanerCodigo.crear({
                            montaje: el.camara,
                            alIniciar: function () {
                                el.escanear.disabled = true;
                                ponerEscanear('ti-camera', 'Abriendo...');
                            },
                            alActivar: function () {
                                ponerEscanear('ti-camera', 'Activa');
                            },
                            alCerrar: function () {
                                el.escanear.disabled = false;
                                ponerEscanear('ti-scan', 'Escanear');
                            },
                            alError: function (mensaje) {
                                mostrarMensaje(mensaje);
                            },
                            alLeer: function (codigo) {
                                var ahora = Date.now();

                                if (codigo === ultimoCodigo && (ahora - ultimoTiempo) < 2500) {
                                    return 'pendiente';
                                }

                                ultimoCodigo = codigo;
                                ultimoTiempo = ahora;

                                consultarCodigo(codigo, true);

                                return 'pendiente';
                            }
                        });
                    }

                    escaner.abrir();

                } catch (e) {
                    mostrarMensaje('No se pudo cargar la cámara.');
                }

                abriendoCamara = false;
            }

            el.escanear.addEventListener('click', abrirCamara);

            function limpiarPanel() {
                clearTimeout(temporizador);
                numeroBusqueda++;

                actual = null;
                ultimoCodigo = '';
                ultimoTiempo = 0;

                el.entrada.value = '';
                el.lista.innerHTML = '';
                el.tarjeta.hidden = true;
                el.vacio.hidden = false;

                ocultarSugerencia();
                ocultarMensaje();
            }

            function abrirPanel() {
                if (abierto) return;

                abierto = true;
                anterior = document.activeElement;

                panel.hidden = false;
                capa.hidden = false;

                fab.classList.remove('mostrar-etiqueta');
                document.body.classList.add('consulta-abierta');
                fab.setAttribute('aria-expanded', 'true');

                if (!tactil) {
                    setTimeout(function () {
                        el.entrada.focus();
                    }, 60);
                }
            }

            function cerrarPanel() {
                if (!abierto) return;

                abierto = false;

                if (escaner) {
                    escaner.cerrar();
                }

                panel.hidden = true;
                capa.hidden = true;

                limpiarPanel();

                document.body.classList.remove('consulta-abierta');
                fab.setAttribute('aria-expanded', 'false');

                if (anterior && typeof anterior.focus === 'function') {
                    try {
                        anterior.focus();
                    } catch (e) { }
                }

                anterior = null;
            }

            fab.addEventListener('click', function () {
                if (abierto) {
                    cerrarPanel();
                } else {
                    abrirPanel();
                }
            });

            el.cerrar.addEventListener('click', cerrarPanel);
            capa.addEventListener('click', cerrarPanel);

            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape' && abierto) {
                    cerrarPanel();
                }
            });

            window.addEventListener('pagehide', function () {
                if (escaner) {
                    escaner.cerrar();
                }
            });

        } catch (e) { }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();