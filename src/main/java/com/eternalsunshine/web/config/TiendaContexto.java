package com.eternalsunshine.web.config;

/**
 * Guarda, para el hilo que atiende la petición, cuál es la tienda de la persona con sesión.
 * Hibernate la consulta para filtrar los datos y para marcar lo que se guarda.
 */
public final class TiendaContexto {

    public static final Integer SIN_TIENDA = 0;

    private static final ThreadLocal<Integer> ACTUAL = new ThreadLocal<>();

    private TiendaContexto() {
    }

    public static Integer actual() {
        Integer id = ACTUAL.get();
        return id == null ? SIN_TIENDA : id;
    }

    public static void fijar(Integer tiendaId) {
        if (tiendaId == null) {
            ACTUAL.remove();
        } else {
            ACTUAL.set(tiendaId);
        }
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    public static void ejecutar(Integer tiendaId, Runnable accion) {

        Integer anterior = ACTUAL.get();

        try {
            fijar(tiendaId);
            accion.run();
        } finally {
            fijar(anterior);
        }
    }
}
