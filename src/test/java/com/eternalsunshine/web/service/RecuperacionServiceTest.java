package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.TokenRecuperacion;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.TokenRecuperacionRepository;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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
 * Pruebas de la recuperación de contraseña: vida del enlace, uso único y hash del token.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenRecuperacionRepository tokenRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private RecuperacionService servicio;

    private Usuario marta;

    @BeforeEach
    void preparar() {
        marta = new Usuario();
        marta.setId(1);
        marta.setUsername("marta");
        marta.setEmail("marta@correo.com");
        marta.setTiendaId(5);
        marta.setActivo(true);
    }

    private TokenRecuperacion tokenDe(String texto, LocalDateTime expira, boolean usado) {
        TokenRecuperacion t = new TokenRecuperacion(1, RecuperacionService.hash(texto), expira);
        t.setUsado(usado);
        return t;
    }

    // ---------- correo y hash ----------

    @Test
    @DisplayName("El correo se limpia: espacios y mayúsculas")
    void correoSeLimpia() {
        assertEquals("marta@correo.com", RecuperacionService.limpiarCorreo("  Marta@Correo.COM "));
    }

    @Test
    @DisplayName("Un correo inválido se rechaza")
    void correoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> RecuperacionService.limpiarCorreo("sin-arroba"));
        assertThrows(IllegalArgumentException.class, () -> RecuperacionService.limpiarCorreo(""));
        assertThrows(IllegalArgumentException.class, () -> RecuperacionService.limpiarCorreo(null));
    }

    @Test
    @DisplayName("El token se guarda como SHA-256 (valor conocido para «abc»)")
    void hashSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                RecuperacionService.hash("abc"));
    }

    // ---------- solicitar ----------

    @Test
    @DisplayName("Solicitar crea un token que vence en 30 minutos y envía el correo")
    void solicitarCreaTokenYEnvia() {
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(marta);

        servicio.solicitar("marta", "https://eternalsunshine.onrender.com");

        ArgumentCaptor<TokenRecuperacion> captor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(captor.capture());
        verify(tokenRepository).invalidarPendientesDe(1);

        TokenRecuperacion guardado = captor.getValue();
        assertEquals(64, guardado.getTokenHash().length());
        assertTrue(guardado.getExpira().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(guardado.getExpira().isBefore(LocalDateTime.now().plusMinutes(31)));
        assertFalse(guardado.isUsado());

        verify(correoService).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Si la cuenta no existe no se crea token ni se envía nada")
    void solicitarCuentaInexistente() {
        when(usuarioRepository.findByUsernameIgnoreCase("nadie")).thenReturn(null);

        servicio.solicitar("nadie", "https://eternalsunshine.onrender.com");

        verify(tokenRepository, never()).save(any());
        verify(correoService, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Una cuenta sin correo no recibe enlace")
    void solicitarSinCorreo() {
        marta.setEmail(null);
        when(usuarioRepository.findByUsernameIgnoreCase("marta")).thenReturn(marta);

        servicio.solicitar("marta", "https://eternalsunshine.onrender.com");

        verify(tokenRepository, never()).save(any());
    }

    // ---------- validez del token ----------

    @Test
    @DisplayName("Un token vigente y sin usar es válido")
    void tokenVigente() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("tok")))
                .thenReturn(Optional.of(tokenDe("tok", LocalDateTime.now().plusMinutes(10), false)));

        assertTrue(servicio.tokenValido("tok"));
    }

    @Test
    @DisplayName("Un token vencido no es válido")
    void tokenVencido() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("tok")))
                .thenReturn(Optional.of(tokenDe("tok", LocalDateTime.now().minusMinutes(1), false)));

        assertFalse(servicio.tokenValido("tok"));
    }

    @Test
    @DisplayName("Un token ya usado no es válido")
    void tokenUsado() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("tok")))
                .thenReturn(Optional.of(tokenDe("tok", LocalDateTime.now().plusMinutes(10), true)));

        assertFalse(servicio.tokenValido("tok"));
    }

    @Test
    @DisplayName("Un token vacío o nulo no es válido")
    void tokenVacio() {
        assertFalse(servicio.tokenValido(null));
        assertFalse(servicio.tokenValido("   "));
    }

    // ---------- restablecer ----------

    @Test
    @DisplayName("Restablecer con un token inválido lanza TokenInvalidoException")
    void restablecerTokenInvalido() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("malo"))).thenReturn(Optional.empty());

        assertThrows(RecuperacionService.TokenInvalidoException.class,
                () -> servicio.restablecer("malo", "Nueva2026", "Nueva2026"));
    }

    @Test
    @DisplayName("Si la confirmación no coincide no se cambia la contraseña")
    void restablecerNoCoincide() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("tok")))
                .thenReturn(Optional.of(tokenDe("tok", LocalDateTime.now().plusMinutes(10), false)));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(marta));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.restablecer("tok", "Nueva2026", "Otra2026"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Restablecer guarda la contraseña cifrada e invalida los enlaces pendientes")
    void restablecerCorrecto() {
        when(tokenRepository.findByTokenHash(RecuperacionService.hash("tok")))
                .thenReturn(Optional.of(tokenDe("tok", LocalDateTime.now().plusMinutes(10), false)));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(marta));
        when(usuarioService.validarPassword("Nueva2026", "marta")).thenReturn("Nueva2026");
        when(passwordEncoder.encode("Nueva2026")).thenReturn("HASH-CIFRADO");

        Usuario resultado = servicio.restablecer("tok", "Nueva2026", "Nueva2026");

        assertEquals("HASH-CIFRADO", resultado.getPassword());
        verify(usuarioRepository).save(marta);
        verify(tokenRepository).invalidarPendientesDe(1);
    }
}
