package com.eternalsunshine.web.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginIntentosService {

    private static final int MAXIMO_POR_USUARIO = 5;
    private static final int MAXIMO_POR_EQUIPO = 20;
    private static final long VENTANA_MS = 15L * 60 * 1000;
    private static final long BLOQUEO_MS = 5L * 60 * 1000;
    private static final int LIMITE_PARA_LIMPIAR = 1000;

    private static class Registro {
        private int fallos;
        private long primerFallo;
        private long bloqueadoHasta;
    }

    private final Map<String, Registro> registros = new HashMap<>();

    private String claveUsuario(String ip, String usuario) {

        String nombre = usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);

        if (nombre.length() > 100) {
            nombre = nombre.substring(0, 100);
        }

        return "u|" + ip + "|" + nombre;
    }

    private String claveEquipo(String ip) {
        return "e|" + ip;
    }

    private long restante(Registro registro, long ahora) {
        return registro == null ? 0 : Math.max(0, registro.bloqueadoHasta - ahora);
    }

    private void limpiar(long ahora) {

        if (registros.size() < LIMITE_PARA_LIMPIAR) {
            return;
        }

        registros.values().removeIf(r ->
                ahora >= r.bloqueadoHasta && ahora - r.primerFallo > VENTANA_MS);
    }

    private void fallo(String clave, int maximo, long ahora) {

        Registro registro = registros.computeIfAbsent(clave, k -> new Registro());

        if (registro.fallos == 0 || ahora - registro.primerFallo > VENTANA_MS) {
            registro.fallos = 0;
            registro.primerFallo = ahora;
        }

        registro.fallos++;

        if (registro.fallos >= maximo) {
            registro.bloqueadoHasta = ahora + BLOQUEO_MS;
            registro.fallos = 0;
        }
    }

    public synchronized long milisegundosBloqueado(String ip, String usuario) {

        long ahora = System.currentTimeMillis();

        limpiar(ahora);

        return Math.max(
                restante(registros.get(claveUsuario(ip, usuario)), ahora),
                restante(registros.get(claveEquipo(ip)), ahora));
    }

    public synchronized void registrarFallo(String ip, String usuario) {

        long ahora = System.currentTimeMillis();

        fallo(claveUsuario(ip, usuario), MAXIMO_POR_USUARIO, ahora);
        fallo(claveEquipo(ip), MAXIMO_POR_EQUIPO, ahora);
    }

    public synchronized void registrarExito(String ip, String usuario) {
        registros.remove(claveUsuario(ip, usuario));
    }
}