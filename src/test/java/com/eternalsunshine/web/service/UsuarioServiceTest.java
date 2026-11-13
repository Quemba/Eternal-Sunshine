package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de inicio de sesión y gestión de usuarios (roles, estado y contraseña).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HistorialService historialService;

    @InjectMocks
    private UsuarioService servicio;

    private Usuario admin;
    private Usuario empleado;

    @BeforeEach
    void preparar() {
        admin = new Usuario();
        admin.setId(1);
        admin.setUsername("marta");
        admin.setPassword("HASH");
        admin.setRol(Rol.ADMIN);
        admin.setTiendaId(5);
        admin.setActivo(true);

        empleado = new Usuario();
        empleado.setId(2);
        empleado.setUsername("juan");
        empleado.setPassword("HASH-JUAN");
        empleado.setRol(Rol.EMPLEADO);
        empleado.setTiendaId(5);
        empleado.setActivo(true);
    }

    // ---------- etiquetas ----------

    @Test
    @DisplayName("Las etiquetas de rol se muestran en español")
    void etiquetas() {
        assertEquals("Administrador", UsuarioService.etiquetaRol(Rol.ADMIN));
        assertEquals("Empleado", UsuarioService.etiquetaRol(Rol.EMPLEADO));
        assertEquals("Sin rol", UsuarioService.etiquetaRol(null));
    }

    // ---------- login ----------

    @Test
    @DisplayName("Login correcto devuelve el usuario")
    void loginCorrecto() {
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(admin);
        when(passwordEncoder.matches("Marta2026", "HASH")).thenReturn(true);

        assertEquals(admin, servicio.login("marta", "Marta2026"));
    }

    @Test
    @DisplayName("Contraseña incorrecta no inicia sesión")
    void loginPasswordIncorrecta() {
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(admin);
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        assertNull(servicio.login("marta", "mala"));
    }

    @Test
    @DisplayName("Un usuario desactivado no inicia sesión aunque la contraseña sea correcta")
    void loginUsuarioDesactivado() {
        admin.setActivo(false);
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(admin);
        when(passwordEncoder.matches("Marta2026", "HASH")).thenReturn(true);

        assertNull(servicio.login("marta", "Marta2026"));
    }

    @Test
    @DisplayName("Un usuario sin tienda no inicia sesión")
    void loginSinTienda() {
        admin.setTiendaId(null);
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(admin);
        when(passwordEncoder.matches("Marta2026", "HASH")).thenReturn(true);

        assertNull(servicio.login("marta", "Marta2026"));
    }

    @Test
    @DisplayName("Un usuario que no existe devuelve null")
    void loginUsuarioInexistente() {
        when(usuarioRepository.findByUsernameIgnoreCase("nadie")).thenReturn(null);

        assertNull(servicio.login("nadie", "Cualquiera1"));
    }

    @Test
    @DisplayName("Un nombre de usuario vacío devuelve null sin consultar la base de datos")
    void loginVacio() {
        assertNull(servicio.login("   ", "Cualquiera1"));
        verify(usuarioRepository, never()).findByUsernameIgnoreCase(anyString());
    }

    // ---------- crear ----------

    @Test
    @DisplayName("Un empleado no puede crear usuarios")
    void empleadoNoCreaUsuarios() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("nuevo", "Clave2026", "EMPLEADO", empleado));

        assertEquals("Solo un administrador activo puede hacer esto.", e.getMessage());
    }

    @Test
    @DisplayName("El nombre de usuario debe ser válido")
    void nombreInvalido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("ab", "Clave2026", "EMPLEADO", admin));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("nombre con espacios", "Clave2026", "EMPLEADO", admin));
    }

    @Test
    @DisplayName("No se pueden repetir nombres de usuario")
    void nombreDuplicado() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsernameIgnoreCase("juan")).thenReturn(empleado);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crear("juan", "Clave2026", "EMPLEADO", admin));

        assertEquals("Ya existe un usuario con ese nombre.", e.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("El administrador crea un empleado en su misma tienda con la contraseña cifrada")
    void crearEmpleado() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsernameIgnoreCase("lucia")).thenReturn(null);
        when(passwordEncoder.encode("Clave2026")).thenReturn("HASH-NUEVO");

        Usuario nuevo = servicio.crear("lucia", "Clave2026", "empleado", admin);

        assertEquals("lucia", nuevo.getUsername());
        assertEquals("HASH-NUEVO", nuevo.getPassword());
        assertEquals(Rol.EMPLEADO, nuevo.getRol());
        assertEquals(5, nuevo.getTiendaId());
        assertTrue(nuevo.estaActivo());
        verify(usuarioRepository).save(nuevo);
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    // ---------- estado y rol ----------

    @Test
    @DisplayName("Un administrador no puede desactivar su propia cuenta")
    void noDesactivarseASiMismo() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarEstado(1, false, admin));

        assertEquals("No puedes desactivar tu propia cuenta.", e.getMessage());
    }

    @Test
    @DisplayName("Desactivar a un empleado lo deja inactivo")
    void desactivarEmpleado() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findByTiendaId(5)).thenReturn(List.of(admin, empleado));

        servicio.cambiarEstado(2, false, admin);

        assertFalse(empleado.estaActivo());
        verify(usuarioRepository).save(empleado);
        verify(historialService).registrar(any(Usuario.class), anyString());
    }

    @Test
    @DisplayName("Un usuario que ya está desactivado no se desactiva otra vez")
    void yaDesactivado() {
        empleado.setActivo(false);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));

        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstado(2, false, admin));
    }

    @Test
    @DisplayName("Un administrador de otra tienda no se puede tocar")
    void otraTienda() {
        empleado.setTiendaId(99);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarEstado(2, false, admin));

        assertEquals("Ese usuario ya no existe.", e.getMessage());
    }

    @Test
    @DisplayName("Un administrador no puede cambiar su propio rol")
    void noCambiarPropioRol() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarRol(1, "EMPLEADO", admin));
    }

    @Test
    @DisplayName("Un rol inválido se rechaza")
    void rolInvalido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));

        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarRol(2, "SUPERMAN", admin));
    }

    // ---------- cambiar mi contraseña ----------

    @Test
    @DisplayName("Si la contraseña actual es incorrecta no se cambia nada")
    void miPasswordActualIncorrecta() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(passwordEncoder.matches("equivocada", "HASH-JUAN")).thenReturn(false);

        assertThrows(UsuarioService.PasswordActualIncorrectaException.class,
                () -> servicio.cambiarMiPassword(2, "equivocada", "Nueva2026", "Nueva2026"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("La nueva contraseña y su confirmación deben coincidir")
    void miPasswordNoCoincide() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(passwordEncoder.matches("Vieja2026", "HASH-JUAN")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarMiPassword(2, "Vieja2026", "Nueva2026", "Otra2026"));
    }

    @Test
    @DisplayName("La nueva contraseña debe ser distinta de la actual")
    void miPasswordIgualALaActual() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(passwordEncoder.matches("Vieja2026", "HASH-JUAN")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarMiPassword(2, "Vieja2026", "Vieja2026", "Vieja2026"));
    }

    @Test
    @DisplayName("Cambiar la contraseña correctamente la guarda cifrada")
    void miPasswordCorrecta() {
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(empleado));
        when(passwordEncoder.matches("Vieja2026", "HASH-JUAN")).thenReturn(true);
        when(passwordEncoder.encode("Nueva2026")).thenReturn("HASH-NUEVO");

        Usuario resultado = servicio.cambiarMiPassword(2, "Vieja2026", "Nueva2026", "Nueva2026");

        assertEquals("HASH-NUEVO", resultado.getPassword());
        verify(usuarioRepository).save(empleado);
        verify(historialService).registrar(any(Usuario.class), anyString());
    }
}
