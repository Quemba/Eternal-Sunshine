package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    public static final int PASSWORD_MINIMA = 8;
    public static final int PASSWORD_MAXIMA = 64;

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");

    private static final Set<String> COMUNES = new HashSet<>(Arrays.asList(
            "12345678", "123456789", "1234567890", "11111111", "00000000",
            "password", "password1", "qwertyui", "qwerty123", "abcd1234",
            "admin123", "contrasena", "contraseña"));

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HistorialService historialService;

    private volatile String hashFicticio;

    public static class OpcionRol {

        private final String valor;
        private final String etiqueta;

        public OpcionRol(String valor, String etiqueta) {
            this.valor = valor;
            this.etiqueta = etiqueta;
        }

        public String getValor() { return valor; }
        public String getEtiqueta() { return etiqueta; }
    }

    public static class PasswordActualIncorrectaException extends IllegalArgumentException {

        public PasswordActualIncorrectaException(String mensaje) {
            super(mensaje);
        }
    }

    private String hashFicticio() {

        String hash = hashFicticio;

        if (hash == null) {
            hash = passwordEncoder.encode("contraseña-ficticia-para-igualar-tiempos");
            hashFicticio = hash;
        }

        return hash;
    }

    private String recortar(String texto) {
        return texto.length() > 250 ? texto.substring(0, 247) + "..." : texto;
    }

    public static String etiquetaRol(Rol rol) {

        if (rol == null) {
            return "Sin rol";
        }

        if (Rol.ADMIN.equals(rol)) {
            return "Administrador";
        }

        String nombre = rol.name().toLowerCase(Locale.ROOT);

        return nombre.substring(0, 1).toUpperCase(Locale.ROOT) + nombre.substring(1);
    }

    public List<OpcionRol> opcionesRol() {

        List<OpcionRol> lista = new ArrayList<>();

        for (Rol rol : Rol.values()) {
            lista.add(new OpcionRol(rol.name(), etiquetaRol(rol)));
        }

        return lista;
    }

    public Usuario login(String username, String password) {

        String nombre = username == null ? "" : username.trim();
        String clave = password == null ? "" : password.trim();

        Usuario usuario = nombre.isEmpty()
                ? null
                : usuarioRepository.findByUsernameIgnoreCase(nombre);

        if (usuario == null) {
            passwordEncoder.matches(clave, hashFicticio());
            return null;
        }

        boolean coincide = passwordEncoder.matches(clave, usuario.getPassword());

        return (coincide && usuario.estaActivo() && usuario.getTiendaId() != null) ? usuario : null;
    }

    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username);
    }

    public List<Usuario> obtenerUsuarios(Integer tiendaId) {
        return usuarioRepository.findByTiendaId(tiendaId, Sort.by("username"));
    }

    private String limpiarNombre(String texto) {

        String nombre = texto == null ? "" : texto.trim();

        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 50 caracteres y solo puede incluir letras, números, punto, guion y guion bajo.");
        }

        return nombre;
    }

    public String validarPassword(String password, String username) {

        String clave = password == null ? "" : password.trim();

        if (clave.length() < PASSWORD_MINIMA) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + PASSWORD_MINIMA + " caracteres.");
        }

        if (clave.length() > PASSWORD_MAXIMA) {
            throw new IllegalArgumentException("La contraseña no puede superar " + PASSWORD_MAXIMA + " caracteres.");
        }

        if (username != null && clave.equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("La contraseña no puede ser igual al nombre de usuario.");
        }

        if (COMUNES.contains(clave.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Esa contraseña es demasiado común. Elige otra.");
        }

        boolean tieneLetra = clave.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = clave.chars().anyMatch(Character::isDigit);

        if (!tieneLetra || !tieneNumero) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos una letra y un número.");
        }

        return clave;
    }

    private Rol parsearRol(String texto) {

        try {
            return Rol.valueOf(texto == null ? "" : texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Elige un rol válido.");
        }
    }

    private Usuario exigirAdmin(Usuario sesion) {

        Usuario actual = (sesion == null || sesion.getId() == null)
                ? null
                : usuarioRepository.findById(sesion.getId()).orElse(null);

        if (actual == null || !actual.estaActivo() || !Rol.ADMIN.equals(actual.getRol())) {
            throw new IllegalArgumentException("Solo un administrador activo puede hacer esto.");
        }

        return actual;
    }

    private Usuario buscarObjetivo(Integer id, Usuario admin, String mensajeSiEsElMismo) {

        Usuario objetivo = id == null ? null : usuarioRepository.findById(id).orElse(null);

        if (objetivo == null || !Objects.equals(objetivo.getTiendaId(), admin.getTiendaId())) {
            throw new IllegalArgumentException("Ese usuario ya no existe.");
        }

        if (objetivo.getId().equals(admin.getId())) {
            throw new IllegalArgumentException(mensajeSiEsElMismo);
        }

        return objetivo;
    }

    private void exigirAlMenosUnAdminActivo(Integer tiendaId) {

        for (Usuario usuario : usuarioRepository.findByTiendaId(tiendaId)) {
            if (Rol.ADMIN.equals(usuario.getRol()) && usuario.estaActivo()) {
                return;
            }
        }

        throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
    }

    @Transactional
    public Usuario crear(String username, String password, String rolTexto, Usuario sesionAdmin) {

        Usuario admin = exigirAdmin(sesionAdmin);

        String nombre = limpiarNombre(username);
        Rol rol = parsearRol(rolTexto);
        String clave = validarPassword(password, nombre);

        if (usuarioRepository.findByUsernameIgnoreCase(nombre) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(nombre);
        nuevo.setPassword(passwordEncoder.encode(clave));
        nuevo.setRol(rol);
        nuevo.setTiendaId(admin.getTiendaId());
        nuevo.setActivo(true);

        usuarioRepository.save(nuevo);

        historialService.registrar(admin,
                recortar("CREÓ USUARIO: " + nombre + " (" + etiquetaRol(rol) + ")"));

        return nuevo;
    }

    @Transactional
    public Usuario crearAdminDeTienda(String username, String password, Integer tiendaId) {

        if (tiendaId == null) {
            throw new IllegalArgumentException("Falta la tienda del administrador.");
        }

        String nombre = limpiarNombre(username);
        String clave = validarPassword(password, nombre);

        if (usuarioRepository.findByUsernameIgnoreCase(nombre) != null) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso. Elige otro.");
        }

        Usuario admin = new Usuario();
        admin.setUsername(nombre);
        admin.setPassword(passwordEncoder.encode(clave));
        admin.setRol(Rol.ADMIN);
        admin.setTiendaId(tiendaId);
        admin.setActivo(true);

        return usuarioRepository.save(admin);
    }

    @Transactional
    public void cambiarRol(Integer id, String rolTexto, Usuario sesionAdmin) {

        Usuario admin = exigirAdmin(sesionAdmin);
        Usuario objetivo = buscarObjetivo(id, admin, "No puedes cambiar tu propio rol.");

        Rol nuevo = parsearRol(rolTexto);
        Rol anterior = objetivo.getRol();

        if (nuevo.equals(anterior)) {
            throw new IllegalArgumentException("«" + objetivo.getUsername() + "» ya tiene ese rol.");
        }

        objetivo.setRol(nuevo);
        usuarioRepository.save(objetivo);

        exigirAlMenosUnAdminActivo(admin.getTiendaId());

        historialService.registrar(admin,
                recortar("EDITÓ USUARIO: " + objetivo.getUsername() + " — rol "
                        + etiquetaRol(anterior) + " → " + etiquetaRol(nuevo)));
    }

    @Transactional
    public void restablecerPassword(Integer id, String nueva, Usuario sesionAdmin) {

        Usuario admin = exigirAdmin(sesionAdmin);
        Usuario objetivo = buscarObjetivo(id, admin,
                "Para cambiar tu propia contraseña usa «Cambiar contraseña» en el menú.");

        String clave = validarPassword(nueva, objetivo.getUsername());

        objetivo.setPassword(passwordEncoder.encode(clave));
        usuarioRepository.save(objetivo);

        historialService.registrar(admin,
                recortar("EDITÓ USUARIO: " + objetivo.getUsername() + " — restableció la contraseña"));
    }

    @Transactional
    public void cambiarEstado(Integer id, boolean activo, Usuario sesionAdmin) {

        Usuario admin = exigirAdmin(sesionAdmin);
        Usuario objetivo = buscarObjetivo(id, admin, "No puedes desactivar tu propia cuenta.");

        if (objetivo.estaActivo() == activo) {
            throw new IllegalArgumentException("«" + objetivo.getUsername() + "» ya está "
                    + (activo ? "activo." : "desactivado."));
        }

        objetivo.setActivo(activo);
        usuarioRepository.save(objetivo);

        exigirAlMenosUnAdminActivo(admin.getTiendaId());

        historialService.registrar(admin,
                recortar("EDITÓ USUARIO: " + objetivo.getUsername() + " — "
                        + (activo ? "reactivado" : "desactivado")));
    }

    @Transactional
    public Usuario cambiarMiPassword(Integer usuarioId, String actual, String nueva, String confirmacion) {

        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);

        if (usuario == null || !usuario.estaActivo()) {
            throw new IllegalArgumentException("No se pudo cambiar la contraseña. Inicia sesión de nuevo.");
        }

        String actualLimpia = actual == null ? "" : actual.trim();
        String nuevaLimpia = nueva == null ? "" : nueva.trim();
        String confirmacionLimpia = confirmacion == null ? "" : confirmacion.trim();

        if (!passwordEncoder.matches(actualLimpia, usuario.getPassword())) {
            throw new PasswordActualIncorrectaException("La contraseña actual no es correcta.");
        }

        if (!nuevaLimpia.equals(confirmacionLimpia)) {
            throw new IllegalArgumentException("La contraseña nueva y su confirmación no coinciden.");
        }

        if (nuevaLimpia.equals(actualLimpia)) {
            throw new IllegalArgumentException("La contraseña nueva debe ser distinta de la actual.");
        }

        String clave = validarPassword(nuevaLimpia, usuario.getUsername());

        usuario.setPassword(passwordEncoder.encode(clave));
        usuarioRepository.save(usuario);

        historialService.registrar(usuario,
                recortar("EDITÓ USUARIO: " + usuario.getUsername() + " — cambió su contraseña"));

        return usuario;
    }
}