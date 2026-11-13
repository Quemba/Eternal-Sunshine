package com.eternalsunshine.web.service;

import com.eternalsunshine.web.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las cifras del dashboard y del escape de caracteres en la búsqueda.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService servicio;

    @Test
    @DisplayName("La búsqueda escapa %, _ y \\ para que no actúen como comodines")
    void buscarEscapaComodines() {
        when(productoRepository.buscar("50\\%\\_\\\\")).thenReturn(List.of());

        servicio.buscar("50%_\\");

        verify(productoRepository).buscar("50\\%\\_\\\\");
    }

    @Test
    @DisplayName("Stock bajo cuenta los productos con 1 a 5 unidades")
    void stockBajo() {
        when(productoRepository.countByCantidadBetween(1, 5)).thenReturn(4L);

        assertEquals(4L, servicio.obtenerStockBajo());
    }

    @Test
    @DisplayName("Agotados cuenta los productos con 0 unidades")
    void agotados() {
        when(productoRepository.countByCantidad(0)).thenReturn(2L);

        assertEquals(2L, servicio.obtenerAgotados());
    }

    @Test
    @DisplayName("El total de productos y el valor del inventario vienen del repositorio")
    void totales() {
        when(productoRepository.count()).thenReturn(40L);
        when(productoRepository.calcularValorInventario()).thenReturn(1250000L);

        assertEquals(40L, servicio.obtenerTotalProductos());
        assertEquals(1250000L, servicio.obtenerValorInventario());
    }
}
