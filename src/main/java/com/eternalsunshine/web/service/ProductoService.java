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

public List<Producto> buscar(
        String texto) {

    return productoRepository
            .buscar(texto);
}

public long obtenerTotalProductos() {

    return productoRepository.count();
}

public long obtenerStockBajo() {

    return productoRepository
            .countByCantidadLessThanEqual(5);
}

public long obtenerAgotados() {

    return productoRepository
            .countByCantidad(0);
}

public Double obtenerValorInventario() {

    return productoRepository
            .calcularValorInventario();
}
}