package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> buscar(String texto) {
        String textoEscapado = texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return productoRepository.buscar(textoEscapado);
    }

    public long obtenerTotalProductos() {
        return productoRepository.count();
    }

    public long obtenerStockBajo() {
        return productoRepository.countByCantidadBetween(1, 5);
    }

    public long obtenerAgotados() {
        return productoRepository.countByCantidad(0);
    }

    public Long obtenerValorInventario() {
        return productoRepository.calcularValorInventario();
    }
}