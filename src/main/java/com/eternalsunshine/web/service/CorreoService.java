package com.eternalsunshine.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Envía correos con la API web de Brevo (funciona en hosting gratuito, que suele bloquear SMTP).
 * Si no hay BREVO_API_KEY y MAIL_FROM, no envía nada: escribe el enlace en el registro del servidor
 * para poder probar en el computador.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    @Value("${app.correo.brevo-api-key:}")
    private String apiKey;

    @Value("${app.correo.remitente:}")
    private String remitente;

    @Value("${app.correo.nombre:Eternal Sunshine}")
    private String nombreRemitente;

    private final ObjectMapper json = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ExecutorService hilo = Executors.newFixedThreadPool(2, tarea -> {
        Thread t = new Thread(tarea, "correo");
        t.setDaemon(true);
        return t;
    });

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank() && remitente != null && !remitente.isBlank();
    }

    /**
     * Se envía en segundo plano para que la respuesta no tarde distinto
     * cuando la cuenta existe y cuando no.
     */
    public void enviar(String para, String asunto, String html, String enlaceParaPruebas) {

        if (!configurado()) {
            log.warn("Correo NO configurado (falta BREVO_API_KEY o MAIL_FROM). Enlace de recuperación para {}: {}",
                    para, enlaceParaPruebas);
            return;
        }

        hilo.submit(() -> {
            try {
                Map<String, Object> cuerpo = Map.of(
                        "sender", Map.of("name", nombreRemitente, "email", remitente.trim()),
                        "to", List.of(Map.of("email", para)),
                        "subject", asunto,
                        "htmlContent", html);

                HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_BREVO))
                        .timeout(Duration.ofSeconds(15))
                        .header("api-key", apiKey.trim())
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)))
                        .build();

                HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

                if (respuesta.statusCode() / 100 != 2) {
                    log.error("Brevo rechazó el correo (HTTP {}): {}", respuesta.statusCode(), respuesta.body());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                log.error("No se pudo enviar el correo de recuperación: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    void cerrar() {
        hilo.shutdown();
    }
}
