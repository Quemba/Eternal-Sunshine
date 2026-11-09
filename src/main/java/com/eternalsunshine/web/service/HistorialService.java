package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Historial;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.HistorialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistorialService {

    @Autowired
    private HistorialRepository historialRepository;

    public void registrar(
            Usuario usuario,
            String accion) {

        Historial historial =
                new Historial();

        historial.setUsuario(usuario);

        historial.setAccion(accion);

        historial.setFecha(
                LocalDateTime.now());

        historialRepository.save(
                historial);
    }

    public List<Historial> obtenerTodo() {

        return historialRepository.findAll();
    }
}