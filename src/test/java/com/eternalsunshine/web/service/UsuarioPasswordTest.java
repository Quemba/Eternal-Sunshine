package com.eternalsunshine.web.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de contraseña de UsuarioService.validarPassword (no necesita Spring ni base de datos).
 */
class UsuarioPasswordTest {

    private final UsuarioService servicio = new UsuarioService();

    @Test
    @DisplayName("Una contraseña válida se devuelve sin espacios a los lados")
    void passwordValida() {
        assertEquals("Marta2026", servicio.validarPassword("  Marta2026 ", "marta"));
    }

    @Test
    @DisplayName("Menos de 8 caracteres se rechaza")
    void muyCorta() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.validarPassword("Ab1", "marta"));
        assertTrue(e.getMessage().contains("al menos"));
    }

    @Test
    @DisplayName("Más de 64 caracteres se rechaza")
    void muyLarga() {
        String larga = "a1".repeat(33);
        assertThrows(IllegalArgumentException.class, () -> servicio.validarPassword(larga, "marta"));
    }

    @Test
    @DisplayName("No puede ser igual al nombre de usuario")
    void igualAlUsuario() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.validarPassword("Marta2026", "marta2026"));
    }

    @Test
    @DisplayName("Las contraseñas comunes se rechazan")
    void contrasenaComun() {
        assertThrows(IllegalArgumentException.class, () -> servicio.validarPassword("password1", "marta"));
        assertThrows(IllegalArgumentException.class, () -> servicio.validarPassword("12345678", "marta"));
    }

    @Test
    @DisplayName("Debe tener al menos una letra y un número")
    void letraYNumero() {
        assertThrows(IllegalArgumentException.class, () -> servicio.validarPassword("soloLetrasAqui", "marta"));
        assertThrows(IllegalArgumentException.class, () -> servicio.validarPassword("98765432", "marta"));
    }
}
