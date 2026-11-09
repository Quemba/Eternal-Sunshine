package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.ContadorDocumento;
import com.eternalsunshine.web.entity.ConteoInventario;
import com.eternalsunshine.web.entity.EntradaMercancia;
import com.eternalsunshine.web.entity.EstadoConteo;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.ContadorDocumentoRepository;
import com.eternalsunshine.web.repository.ConteoRepository;
import com.eternalsunshine.web.repository.EntradaMercanciaRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NumeracionService {

    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private ContadorDocumentoRepository contadores;

    @Autowired
    private ContadorCreador creador;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private EntradaMercanciaRepository entradaRepository;

    @Autowired
    private ConteoRepository conteoRepository;

    public static String periodo(LocalDateTime fecha) {
        return fecha.format(FORMATO_PERIODO);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int siguiente(TipoDocumento tipo, LocalDateTime fecha) {

        String periodo = periodo(fecha);

        Optional<ContadorDocumento> existente = contadores.buscarParaActualizar(tipo.name(), periodo);

        if (existente.isEmpty()) {
            try {
                creador.crearSiNoExiste(tipo.name(), periodo);
            } catch (DataIntegrityViolationException e) {
                // otra operación lo creó al mismo tiempo; se vuelve a leer
            }

            existente = contadores.buscarParaActualizar(tipo.name(), periodo);
        }

        ContadorDocumento contador = existente.orElseThrow(
                () -> new IllegalStateException("No se pudo obtener el contador de " + tipo.name() + "."));

        int numero = contador.getUltimo() + 1;

        contador.setUltimo(numero);
        contadores.save(contador);

        return numero;
    }

    @Transactional
    public void rellenarPendientes() {
        rellenarVentas();
        rellenarEntradas();
        rellenarConteos();

        sincronizar(TipoDocumento.VENTA, ventaRepository.maximosPorPeriodo());
        sincronizar(TipoDocumento.ENTRADA, entradaRepository.maximosPorPeriodo());
        sincronizar(TipoDocumento.CONTEO, conteoRepository.maximosPorPeriodo());
    }

    private void rellenarVentas() {

        List<Venta> pendientes = ventaRepository.findByConsecutivoIsNullOrderByFechaAscIdAsc();
        Map<String, Integer> ultimos = new HashMap<>();

        for (Venta venta : pendientes) {

            LocalDateTime fecha = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();
            String periodo = periodo(fecha);

            if (!ultimos.containsKey(periodo)) {
                ultimos.put(periodo, ventaRepository.maximoConsecutivo(periodo));
            }

            int numero = ultimos.get(periodo) + 1;
            ultimos.put(periodo, numero);

            venta.setPeriodo(periodo);
            venta.setConsecutivo(numero);
        }

        ventaRepository.saveAll(pendientes);
    }

    private void rellenarEntradas() {

        List<EntradaMercancia> pendientes = entradaRepository.findByConsecutivoIsNullOrderByFechaAscIdAsc();
        Map<String, Integer> ultimos = new HashMap<>();
        Map<String, Integer> porRecepcion = new HashMap<>();

        for (EntradaMercancia entrada : pendientes) {

            LocalDateTime fecha = entrada.getFecha() != null ? entrada.getFecha() : LocalDateTime.now();
            String periodo = periodo(fecha);

            String usuarioId = entrada.getUsuario() != null
                    ? String.valueOf(entrada.getUsuario().getId())
                    : "0";

            String clave = periodo + "|" + fecha + "|" + usuarioId;

            Integer numero = porRecepcion.get(clave);

            if (numero == null) {

                if (!ultimos.containsKey(periodo)) {
                    ultimos.put(periodo, entradaRepository.maximoConsecutivo(periodo));
                }

                numero = ultimos.get(periodo) + 1;
                ultimos.put(periodo, numero);
                porRecepcion.put(clave, numero);
            }

            entrada.setPeriodo(periodo);
            entrada.setConsecutivo(numero);
        }

        entradaRepository.saveAll(pendientes);
    }

    private LocalDateTime fechaDeConteo(ConteoInventario conteo) {

        if (conteo.getFechaEnvio() != null) {
            return conteo.getFechaEnvio();
        }

        if (conteo.getFechaInicio() != null) {
            return conteo.getFechaInicio();
        }

        return LocalDateTime.now();
    }

    private void rellenarConteos() {

        List<ConteoInventario> pendientes = new ArrayList<>(
                conteoRepository.findByConsecutivoIsNullAndEstadoInOrderByFechaInicioAscIdAsc(
                        List.of(EstadoConteo.PENDIENTE_REVISION, EstadoConteo.REVISADO)));

        pendientes.sort(Comparator.comparing(this::fechaDeConteo));

        Map<String, Integer> ultimos = new HashMap<>();

        for (ConteoInventario conteo : pendientes) {

            String periodo = periodo(fechaDeConteo(conteo));

            if (!ultimos.containsKey(periodo)) {
                ultimos.put(periodo, conteoRepository.maximoConsecutivo(periodo));
            }

            int numero = ultimos.get(periodo) + 1;
            ultimos.put(periodo, numero);

            conteo.setPeriodo(periodo);
            conteo.setConsecutivo(numero);
        }

        conteoRepository.saveAll(pendientes);
    }

    private void sincronizar(TipoDocumento tipo, List<Object[]> maximos) {

        for (Object[] fila : maximos) {

            String periodo = (String) fila[0];
            int maximo = ((Number) fila[1]).intValue();

            ContadorDocumento contador = contadores.findByTipoAndPeriodo(tipo.name(), periodo)
                    .orElseGet(() -> {
                        ContadorDocumento nuevo = new ContadorDocumento();
                        nuevo.setTipo(tipo.name());
                        nuevo.setPeriodo(periodo);
                        nuevo.setUltimo(0);
                        return nuevo;
                    });

            if (contador.getUltimo() < maximo) {
                contador.setUltimo(maximo);
            }

            contadores.save(contador);
        }
    }
}