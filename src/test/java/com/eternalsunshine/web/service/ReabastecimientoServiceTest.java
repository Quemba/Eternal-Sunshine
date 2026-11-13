package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.DetalleVentaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la sugerencia de reabastecimiento (promedio de ventas de los últimos 30 días).
 */
@ExtendWith(MockitoExtension.class)
class ReabastecimientoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private ReabastecimientoService servicio;

    private Producto producto(int id, String nombre, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setCantidad(stock);
        p.setPrecio(1000);
        return p;
    }

    private List<Object[]> ventas(Object[]... filas) {
        List<Object[]> lista = new ArrayList<>();
        for (Object[] fila : filas) {
            lista.add(fila);
        }
        return lista;
    }

    /** Primera venta hace 9 días: el promedio se calcula sobre 10 días de datos. */
    private void conDiezDiasDeDatos() {
        when(ventaRepository.primeraVentaDesde(any(LocalDateTime.class)))
                .thenReturn(LocalDate.now().minusDays(9).atStartOfDay());
    }

    @Test
    @DisplayName("Sin ventas no hay sugerencias")
    void sinVentas() {
        assertTrue(servicio.calcular().isEmpty());
    }

    @Test
    @DisplayName("Un producto que se agota pronto se sugiere para 14 días y se marca urgente")
    void sugiereYMarcaUrgente() {
        conDiezDiasDeDatos();
        // 30 unidades en 10 días = 3 por día; con stock 6 quedan 2 días.
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(producto(10, "Arroz", 6)));

        List<ReabastecimientoService.Sugerencia> resultado = servicio.calcular();

        assertEquals(1, resultado.size());
        ReabastecimientoService.Sugerencia s = resultado.get(0);
        assertEquals(30, s.getVendidas());
        assertEquals(3.0, s.getPromedioDiario(), 0.0001);
        assertEquals(2.0, s.getDiasRestantes(), 0.0001);
        assertEquals(36, s.getSugerido());   // 3 por día x 14 días = 42, menos el stock de 6
        assertTrue(s.isUrgente());
    }

    @Test
    @DisplayName("Un producto con stock de sobra no se sugiere")
    void stockSuficienteNoSeSugiere() {
        conDiezDiasDeDatos();
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(producto(10, "Arroz", 100)));

        assertTrue(servicio.calcular().isEmpty());
    }

    @Test
    @DisplayName("Un producto sin ventas recientes no aparece")
    void sinVentasDelProducto() {
        conDiezDiasDeDatos();
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(producto(77, "Sin movimiento", 0)));

        assertTrue(servicio.calcular().isEmpty());
    }

    @Test
    @DisplayName("Entre 4 y 7 días de stock se sugiere pero no es urgente")
    void alertaSinUrgencia() {
        conDiezDiasDeDatos();
        // 3 por día; stock 15 = 5 días restantes.
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(producto(10, "Arroz", 15)));

        List<ReabastecimientoService.Sugerencia> resultado = servicio.calcular();

        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).isUrgente());
        assertEquals(27, resultado.get(0).getSugerido());   // 42 - 15
    }

    @Test
    @DisplayName("Las sugerencias salen ordenadas: primero lo que se acaba antes")
    void ordenadasPorDiasRestantes() {
        conDiezDiasDeDatos();
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}, new Object[]{11, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(
                producto(10, "Arroz", 15),
                producto(11, "Leche", 3)));

        List<ReabastecimientoService.Sugerencia> resultado = servicio.calcular();

        assertEquals(2, resultado.size());
        assertEquals("Leche", resultado.get(0).getProducto().getNombre());
        assertEquals("Arroz", resultado.get(1).getProducto().getNombre());
    }

    @Test
    @DisplayName("Un producto agotado con ventas es siempre urgente")
    void agotadoEsUrgente() {
        conDiezDiasDeDatos();
        when(detalleVentaRepository.unidadesPorProductoDesde(any(LocalDateTime.class)))
                .thenReturn(ventas(new Object[]{10, 30L}));
        when(productoRepository.findAll()).thenReturn(List.of(producto(10, "Arroz", 0)));

        List<ReabastecimientoService.Sugerencia> resultado = servicio.calcular();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isUrgente());
        assertEquals(42, resultado.get(0).getSugerido());
    }
}
