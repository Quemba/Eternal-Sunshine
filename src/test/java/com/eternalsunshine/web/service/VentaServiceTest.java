package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.DetalleVenta;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de las reglas de negocio de VentaService.
 * No usan base de datos: los repositorios son simulados con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private HistorialService historialService;

    @Mock
    private NumeracionService numeracionService;

    @InjectMocks
    private VentaService ventaService;

    private Usuario admin;
    private Usuario empleado;
    private Producto cafe;

    @BeforeEach
    void preparar() {
        admin = new Usuario();
        admin.setId(1);
        admin.setUsername("marta");
        admin.setRol(Rol.ADMIN);

        empleado = new Usuario();
        empleado.setId(2);
        empleado.setUsername("juan");
        empleado.setRol(Rol.EMPLEADO);

        cafe = new Producto();
        cafe.setId(10);
        cafe.setCodigo("7701234567890");
        cafe.setNombre("Café molido");
        cafe.setPrecio(5000);
        cafe.setCantidad(20);
    }

    private VentaService.CarritoItem item(Integer productoId, Integer cantidad) {
        VentaService.CarritoItem item = new VentaService.CarritoItem();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        return item;
    }

    private Venta ventaConUnDetalle(int cantidad) {
        Venta venta = new Venta();
        venta.setId(99);
        venta.setAnulada(false);
        venta.setTotal(cantidad * cafe.getPrecio());

        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(cafe);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(cafe.getPrecio());
        detalle.setSubtotal(cantidad * cafe.getPrecio());

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);
        return venta;
    }

    // ---------- registrarVenta ----------

    @Test
    @DisplayName("Un carrito vacío no se puede vender")
    void carritoVacioLanzaError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ventaService.registrarVenta(new ArrayList<>(), admin));

        assertEquals("El carrito está vacío.", e.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una cantidad en cero o negativa es inválida")
    void cantidadInvalidaLanzaError() {
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.registrarVenta(List.of(item(10, 0)), admin));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.registrarVenta(List.of(item(10, -3)), admin));
    }

    @Test
    @DisplayName("No se puede vender más de lo que hay en stock y el stock no cambia")
    void stockInsuficienteNoVende() {
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(cafe));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ventaService.registrarVenta(List.of(item(10, 21)), admin));

        assertTrue(e.getMessage().contains("No hay suficiente stock"));
        assertEquals(20, cafe.getCantidad());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una venta correcta descuenta el stock y calcula el total")
    void ventaCorrectaDescuentaStock() {
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(cafe));
        when(numeracionService.siguiente(any(TipoDocumento.class), any(LocalDateTime.class))).thenReturn(7);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        Venta venta = ventaService.registrarVenta(List.of(item(10, 3)), admin);

        assertEquals(17, cafe.getCantidad());
        assertEquals(15000, venta.getTotal());
        assertEquals(7, venta.getConsecutivo());
        assertFalse(venta.getAnulada());
        assertEquals(1, venta.getDetalles().size());
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    @Test
    @DisplayName("Si el mismo producto viene dos veces en el carrito, las cantidades se suman")
    void productoRepetidoSeSuma() {
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(cafe));
        when(numeracionService.siguiente(any(TipoDocumento.class), any(LocalDateTime.class))).thenReturn(1);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        Venta venta = ventaService.registrarVenta(List.of(item(10, 2), item(10, 3)), admin);

        assertEquals(15, cafe.getCantidad());
        assertEquals(25000, venta.getTotal());
        assertEquals(1, venta.getDetalles().size());
    }

    @Test
    @DisplayName("Un producto que ya no existe cancela la venta")
    void productoInexistenteCancelaVenta() {
        when(productoRepository.buscarParaActualizar(55)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.registrarVenta(List.of(item(55, 1)), admin));

        verify(ventaRepository, never()).save(any());
    }

    // ---------- anular ----------

    @Test
    @DisplayName("Un empleado no puede anular ventas")
    void empleadoNoPuedeAnular() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ventaService.anular(99, "Error de digitación", empleado));

        assertEquals("Solo el administrador puede anular una venta.", e.getMessage());
    }

    @Test
    @DisplayName("El motivo de anulación necesita mínimo 3 caracteres")
    void motivoCortoLanzaError() {
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.anular(99, "  a ", admin));
    }

    @Test
    @DisplayName("Anular una venta devuelve el stock y la marca como anulada")
    void anularDevuelveStock() {
        cafe.setCantidad(17);
        Venta venta = ventaConUnDetalle(3);

        when(ventaRepository.buscarParaActualizar(99)).thenReturn(Optional.of(venta));
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(cafe));

        Venta resultado = ventaService.anular(99, "Error de digitación", admin);

        assertEquals(20, cafe.getCantidad());
        assertTrue(resultado.getAnulada());
        assertEquals("Error de digitación", resultado.getMotivoAnulacion());
        assertEquals(admin, resultado.getUsuarioAnula());
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    @Test
    @DisplayName("Una venta ya anulada no se puede anular otra vez")
    void noSeAnulaDosVeces() {
        Venta venta = ventaConUnDetalle(1);
        venta.setAnulada(true);

        when(ventaRepository.buscarParaActualizar(99)).thenReturn(Optional.of(venta));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ventaService.anular(99, "Otro motivo", admin));

        assertEquals("Esa venta ya fue anulada.", e.getMessage());
        assertEquals(20, cafe.getCantidad());
    }
}
