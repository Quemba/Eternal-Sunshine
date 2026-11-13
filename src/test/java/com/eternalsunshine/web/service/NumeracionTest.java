package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas de la numeración de documentos (comprobantes). No necesitan Spring ni base de datos.
 */
class NumeracionTest {

    @Test
    @DisplayName("El periodo se arma como año-mes")
    void periodoFormato() {
        assertEquals("2026-11", NumeracionService.periodo(LocalDateTime.of(2026, 11, 13, 9, 30)));
        assertEquals("2026-01", NumeracionService.periodo(LocalDateTime.of(2026, 1, 5, 0, 0)));
    }

    @Test
    @DisplayName("El código de la venta lleva prefijo, periodo y consecutivo de 4 cifras")
    void codigoVenta() {
        assertEquals("VEN-2026-11-0007", TipoDocumento.VENTA.codigo("2026-11", 7));
    }

    @Test
    @DisplayName("Entradas y conteos usan su propio prefijo")
    void codigoEntradaYConteo() {
        assertEquals("ENT-2026-11-0012", TipoDocumento.ENTRADA.codigo("2026-11", 12));
        assertEquals("CON-2026-11-0001", TipoDocumento.CONTEO.codigo("2026-11", 1));
    }

    @Test
    @DisplayName("Sin periodo o sin consecutivo no hay código")
    void codigoIncompleto() {
        assertNull(TipoDocumento.VENTA.codigo(null, 1));
        assertNull(TipoDocumento.VENTA.codigo("2026-11", null));
    }
}
