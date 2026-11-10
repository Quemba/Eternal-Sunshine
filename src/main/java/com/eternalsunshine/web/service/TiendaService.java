package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Tienda;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.TiendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class TiendaService {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[\\p{L}\\p{N} .,'&#°-]{3,100}$");

    @Value("${app.registro.habilitado:true}")
    private boolean registroHabilitado;

    @Value("${app.registro.max-tiendas:200}")
    private long maximoTiendas;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private UsuarioService usuarioService;

    public boolean registroAbierto() {
        return registroHabilitado && tiendaRepository.count() < maximoTiendas;
    }

    public String nombreDe(Integer tiendaId) {

        if (tiendaId == null) {
            return "";
        }

        return tiendaRepository.findById(tiendaId)
                .map(Tienda::getNombre)
                .orElse("");
    }

    private String limpiarNombre(String texto) {

        String nombre = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");

        if (!NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "El nombre de la tienda debe tener entre 3 y 100 caracteres (letras, números y signos simples).");
        }

        return nombre;
    }

    @Transactional
    public Usuario registrar(String nombreTienda, String username, String password) {

        if (!registroHabilitado) {
            throw new IllegalArgumentException("El registro de tiendas nuevas está cerrado por ahora.");
        }

        if (tiendaRepository.count() >= maximoTiendas) {
            throw new IllegalArgumentException("Se alcanzó el máximo de tiendas permitidas por ahora.");
        }

        return crearConAdmin(nombreTienda, username, password);
    }

    @Transactional
    public Usuario crearConAdmin(String nombreTienda, String username, String password) {

        String nombre = limpiarNombre(nombreTienda);

        Tienda tienda = tiendaRepository.save(new Tienda(nombre));

        return usuarioService.crearAdminDeTienda(username, password, tienda.getId());
    }
}
