package com.eternalsunshine.web.config;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Limita cuántos enlaces de recuperación se pueden pedir, para que nadie
 * llene un buzón ajeno ni use la pantalla para adivinar cuentas.
 */
@Component
public class RecuperacionLimiteService {

    private static final int MAXIMO_POR_EQUIPO = 10;
    private static final int MAXIMO_POR_CUENTA = 3;
    private static final long VENTANA_MS = 60L * 60 * 1000;
    private static final int LIMITE_PARA_LIMPIAR = 1000;

    private final Map<String, Deque<Long>> pedidos = new HashMap<>();

    private boolean cabe(String clave, int maximo, long ahora) {

        Deque<Long> cola = pedidos.computeIfAbsent(clave, k -> new ArrayDeque<>());

        while (!cola.isEmpty() && ahora - cola.peekFirst() > VENTANA_MS) {
            cola.pollFirst();
        }

        return cola.size() < maximo;
    }

    public synchronized boolean permitir(String ip, String dato) {

        long ahora = System.currentTimeMillis();

        if (pedidos.size() > LIMITE_PARA_LIMPIAR) {
            pedidos.values().removeIf(c -> c.isEmpty() || ahora - c.peekLast() > VENTANA_MS);
        }

        String cuenta = "c|" + (dato == null ? "" : dato.trim().toLowerCase(Locale.ROOT));
        String equipo = "e|" + ip;

        if (!cabe(equipo, MAXIMO_POR_EQUIPO, ahora) || !cabe(cuenta, MAXIMO_POR_CUENTA, ahora)) {
            return false;
        }

        pedidos.get(equipo).addLast(ahora);
        pedidos.get(cuenta).addLast(ahora);

        return true;
    }
}
