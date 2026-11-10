package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.TokenRecuperacion;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.TokenRecuperacionRepository;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class RecuperacionService {

    public static final int MINUTOS_DE_VIDA = 30;

    private static final Pattern CORREO_VALIDO = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");

    private static final SecureRandom AZAR = new SecureRandom();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorreoService correoService;

    public static class TokenInvalidoException extends IllegalArgumentException {

        public TokenInvalidoException() {
            super("Este enlace no es válido o ya venció. Pide uno nuevo.");
        }
    }

    public static String limpiarCorreo(String texto) {

        String correo = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);

        if (correo.length() > 150 || !CORREO_VALIDO.matcher(correo).matches()) {
            throw new IllegalArgumentException("Escribe un correo válido, por ejemplo nombre@correo.com.");
        }

        return correo;
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cuerpoCorreo(String usuario, String enlace) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;padding:24px;"
                + "background:#f7c8d1;border-radius:20px;color:#4a1020;\">"
                + "<h2 style=\"margin:0 0 12px;color:#df6794;\">Eternal Sunshine</h2>"
                + "<p>Hola, <strong>" + usuario + "</strong>.</p>"
                + "<p>Pediste crear una contraseña nueva. Usa este botón; el enlace sirve una sola vez "
                + "y vence en " + MINUTOS_DE_VIDA + " minutos.</p>"
                + "<p style=\"text-align:center;margin:26px 0;\">"
                + "<a href=\"" + enlace + "\" style=\"background:#df6794;color:#ffffff;text-decoration:none;"
                + "font-weight:bold;padding:14px 26px;border-radius:10px;display:inline-block;\">"
                + "Crear contraseña nueva</a></p>"
                + "<p style=\"font-size:13px;\">Si el botón no funciona, copia esta dirección en tu navegador:<br>"
                + "<span style=\"word-break:break-all;\">" + enlace + "</span></p>"
                + "<p style=\"font-size:13px;\">Si no fuiste tú, ignora este mensaje: tu contraseña no cambia.</p>"
                + "<p style=\"font-size:12px;font-style:italic;opacity:.7;margin-top:22px;\">"
                + "Bienaventurados los olvidadizos.</p>"
                + "</div>";
    }

    /**
     * Crea y envía el enlace. No dice nunca si la cuenta existe: quien llama muestra siempre el mismo mensaje.
     */
    @Transactional
    public void solicitar(String dato, String urlBase) {

        String buscado = dato == null ? "" : dato.trim();

        if (buscado.isEmpty() || buscado.length() > 150) {
            return;
        }

        tokenRepository.borrarVencidosAntesDe(LocalDateTime.now().minusDays(1));

        List<Usuario> cuentas = new ArrayList<>();

        if (buscado.contains("@")) {
            cuentas.addAll(usuarioRepository.findByEmailIgnoreCase(buscado));
        } else {
            Usuario uno = usuarioRepository.findByUsernameIgnoreCase(buscado);

            if (uno != null) {
                cuentas.add(uno);
            }
        }

        for (Usuario cuenta : cuentas) {

            boolean puede = cuenta.estaActivo()
                    && cuenta.getTiendaId() != null
                    && cuenta.getEmail() != null
                    && !cuenta.getEmail().isBlank();

            if (!puede) {
                continue;
            }

            tokenRepository.invalidarPendientesDe(cuenta.getId());

            String token = generarToken();

            tokenRepository.save(new TokenRecuperacion(
                    cuenta.getId(),
                    hash(token),
                    LocalDateTime.now().plusMinutes(MINUTOS_DE_VIDA)));

            String enlace = urlBase + "/restablecer?token=" + token;
            String correo = cuenta.getEmail();
            String nombre = cuenta.getUsername();

            Runnable enviar = () -> correoService.enviar(
                    correo,
                    "Crea tu contraseña nueva — Eternal Sunshine",
                    cuerpoCorreo(nombre, enlace),
                    enlace);

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        enviar.run();
                    }
                });
            } else {
                enviar.run();
            }
        }
    }

    public boolean tokenValido(String token) {
        return buscarVigente(token).isPresent();
    }

    private Optional<TokenRecuperacion> buscarVigente(String token) {

        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }

        return tokenRepository.findByTokenHash(hash(token.trim()))
                .filter(t -> !t.isUsado() && t.getExpira().isAfter(LocalDateTime.now()));
    }

    /**
     * Cambia la contraseña con el enlace. Devuelve la cuenta para que quien llama deje constancia.
     */
    @Transactional
    public Usuario restablecer(String token, String nueva, String confirmacion) {

        TokenRecuperacion registro = buscarVigente(token).orElseThrow(TokenInvalidoException::new);

        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId()).orElse(null);

        if (usuario == null || !usuario.estaActivo() || usuario.getTiendaId() == null) {
            throw new TokenInvalidoException();
        }

        String nuevaLimpia = nueva == null ? "" : nueva.trim();
        String confirmacionLimpia = confirmacion == null ? "" : confirmacion.trim();

        if (!nuevaLimpia.equals(confirmacionLimpia)) {
            throw new IllegalArgumentException("La contraseña nueva y su confirmación no coinciden.");
        }

        String clave = usuarioService.validarPassword(nuevaLimpia, usuario.getUsername());

        usuario.setPassword(passwordEncoder.encode(clave));
        usuarioRepository.save(usuario);

        tokenRepository.invalidarPendientesDe(usuario.getId());

        return usuario;
    }

    /** Guarda el correo de una cuenta recién creada (el registro de tienda). */
    @Transactional
    public Usuario fijarCorreo(Integer usuarioId, String correo) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Esa cuenta ya no existe."));

        usuario.setEmail(limpiarCorreo(correo));

        return usuarioRepository.save(usuario);
    }

    /** Cambia el correo desde «Mi cuenta»: pide la contraseña actual. */
    @Transactional
    public Usuario cambiarCorreo(Integer usuarioId, String correo, String passwordActual) {

        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);

        if (usuario == null || !usuario.estaActivo()) {
            throw new IllegalArgumentException("No se pudo guardar el correo. Inicia sesión de nuevo.");
        }

        String actual = passwordActual == null ? "" : passwordActual.trim();

        if (!passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new UsuarioService.PasswordActualIncorrectaException("La contraseña actual no es correcta.");
        }

        usuario.setEmail(limpiarCorreo(correo));

        return usuarioRepository.save(usuario);
    }
}
