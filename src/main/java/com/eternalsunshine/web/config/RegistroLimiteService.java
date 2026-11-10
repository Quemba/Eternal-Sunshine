package com.eternalsunshine.web.config;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Component
public class RegistroLimiteService {

    private static final int MAXIMO_POR_HORA = 10;
    private static final long VENTANA_MS = 60L * 60 * 1000;
    private static final int LIMITE_PARA_LIMPIAR = 1000;

    private final Map<String, Deque<Long>> intentos = new HashMap<>();

    public synchronized boolean permitir(String ip) {

        long ahora = System.currentTimeMillis();

        if (intentos.size() > LIMITE_PARA_LIMPIAR) {
            intentos.values().removeIf(cola -> cola.isEmpty() || ahora - cola.peekLast() > VENTANA_MS);
        }

        Deque<Long> cola = intentos.computeIfAbsent(ip, clave -> new ArrayDeque<>());

        while (!cola.isEmpty() && ahora - cola.peekFirst() > VENTANA_MS) {
            cola.pollFirst();
        }

        if (cola.size() >= MAXIMO_POR_HORA) {
            return false;
        }

        cola.addLast(ahora);

        return true;
    }
}
