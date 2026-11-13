package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.EntradaMercancia;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.EntradaMercanciaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la recepción de mercancía: suma al stock, validaciones y anulación.
 */
@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private EntradaMercanciaRepository entradaRepository;

    @Mock
    private HistorialService historialService;

    @Mock
    private NumeracionService numeracionService;

    @InjectMocks
    private EntradaService servicio;

    private Usuario admin;
    private Producto leche;

    @BeforeEach
    void preparar() {
        admin = new Usuario();
        admin.setId(1);
        admin.setUsername("marta");
        admin.setRol(Rol.ADMIN);

        leche = new Producto();
        leche.setId(10);
        leche.setNombre("Leche entera");
        leche.setPrecio(4200);
        leche.setCantidad(20);
    }

    private EntradaService.LineaEntrada linea(Integer productoId, Integer cantidad) {
        EntradaService.LineaEntrada l = new EntradaService.LineaEntrada();
        l.setProductoId(productoId);
        l.setCantidad(cantidad);
        return l;
    }

    // ---------- registrar ----------

    @Test
    @DisplayName("Sin productos no se registra la entrada")
    void sinLineas() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(), "", "Distri", "", null, false, admin));
    }

    @Test
    @DisplayName("El proveedor es obligatorio")
    void proveedorObligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(linea(10, 5)), "", "   ", "", null, false, admin));
    }

    @Test
    @DisplayName("La fecha de la factura no puede ser futura")
    void fechaFutura() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(linea(10, 5)), "", "Distri", "F-1",
                        LocalDate.now().plusDays(1), false, admin));
    }

    @Test
    @DisplayName("Una cantidad en cero o mayor a 100.000 es inválida")
    void cantidadInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(linea(10, 0)), "", "Distri", "", null, false, admin));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(linea(10, 100001)), "", "Distri", "", null, false, admin));
    }

    @Test
    @DisplayName("Recibir mercancía suma al stock y guarda el stock antes y después")
    void sumaAlStock() {
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(leche));
        when(numeracionService.siguiente(any(TipoDocumento.class), any(LocalDateTime.class))).thenReturn(3);
        when(entradaRepository.save(any(EntradaMercancia.class))).thenAnswer(i -> i.getArgument(0));

        List<EntradaMercancia> creadas =
                servicio.registrar(List.of(linea(10, 30)), "", "Distri", "", null, false, admin);

        assertEquals(50, leche.getCantidad());
        assertEquals(1, creadas.size());
        assertEquals(30, creadas.get(0).getCantidad());
        assertEquals(3, creadas.get(0).getConsecutivo());
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    @Test
    @DisplayName("No se puede superar el stock máximo de 1.000.000")
    void superaStockMaximo() {
        leche.setCantidad(999990);
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(leche));
        when(numeracionService.siguiente(any(TipoDocumento.class), any(LocalDateTime.class))).thenReturn(1);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(List.of(linea(10, 100)), "", "Distri", "", null, false, admin));

        assertTrue(e.getMessage().contains("máximo permitido"));
        verify(entradaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una factura ya registrada pide confirmación")
    void facturaDuplicada() {
        EntradaMercancia previa = new EntradaMercancia();
        previa.setFecha(LocalDateTime.now().minusDays(2));
        previa.setUsuario(admin);

        when(entradaRepository.buscarFacturaRegistrada("Distri", "F-1")).thenReturn(List.of(previa));

        assertThrows(EntradaService.FacturaDuplicadaException.class,
                () -> servicio.registrar(List.of(linea(10, 5)), "", "Distri", "F-1", null, false, admin));

        verify(entradaRepository, never()).save(any());
    }

    // ---------- anular ----------

    private EntradaMercancia entradaDe(int cantidad) {
        EntradaMercancia e = new EntradaMercancia();
        e.setProducto(leche);
        e.setCantidad(cantidad);
        e.setAnulada(false);
        return e;
    }

    @Test
    @DisplayName("Anular una entrada resta la mercancía del stock")
    void anularRestaStock() {
        EntradaMercancia entrada = entradaDe(5);

        when(entradaRepository.buscarParaActualizar(7)).thenReturn(Optional.of(entrada));
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(leche));

        servicio.anular(7, admin);

        assertEquals(15, leche.getCantidad());
        assertTrue(entrada.getAnulada());
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    @Test
    @DisplayName("No se anula si parte de esa mercancía ya se vendió")
    void anularSinStockSuficiente() {
        leche.setCantidad(2);
        EntradaMercancia entrada = entradaDe(5);

        when(entradaRepository.buscarParaActualizar(7)).thenReturn(Optional.of(entrada));
        when(productoRepository.buscarParaActualizar(10)).thenReturn(Optional.of(leche));

        assertThrows(IllegalArgumentException.class, () -> servicio.anular(7, admin));

        assertEquals(2, leche.getCantidad());
    }

    @Test
    @DisplayName("Una entrada ya anulada no se anula otra vez")
    void anularDosVeces() {
        EntradaMercancia entrada = entradaDe(5);
        entrada.setAnulada(true);

        when(entradaRepository.buscarParaActualizar(7)).thenReturn(Optional.of(entrada));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.anular(7, admin));

        assertEquals("Esa entrada ya fue anulada.", e.getMessage());
    }
}
