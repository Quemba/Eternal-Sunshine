package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.EstadoConteo;
import com.eternalsunshine.web.entity.EstadoSolicitud;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.ConteoRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvisosService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ReabastecimientoService reabastecimientoService;

    @Autowired
    private ConteoRepository conteoRepository;

    public static class Avisos {

        private final int solicitudesPendientes;
        private final int misSolicitudesNuevas;
        private final int porAgotarse;
        private final int reabastecimiento;
        private final int urgentes;
        private final int conteosPendientes;

        public Avisos(int solicitudesPendientes, int misSolicitudesNuevas,
                      int porAgotarse, int reabastecimiento, int urgentes, int conteosPendientes) {
            this.solicitudesPendientes = solicitudesPendientes;
            this.misSolicitudesNuevas = misSolicitudesNuevas;
            this.porAgotarse = porAgotarse;
            this.reabastecimiento = reabastecimiento;
            this.urgentes = urgentes;
            this.conteosPendientes = conteosPendientes;
        }

        public int getSolicitudesPendientes() { return solicitudesPendientes; }
        public int getMisSolicitudesNuevas() { return misSolicitudesNuevas; }
        public int getPorAgotarse() { return porAgotarse; }
        public int getReabastecimiento() { return reabastecimiento; }
        public int getUrgentes() { return urgentes; }
        public int getConteosPendientes() { return conteosPendientes; }
    }

    public Avisos para(Usuario usuario) {

        if (usuario == null) {
            return new Avisos(0, 0, 0, 0, 0, 0);
        }

        try {
            boolean admin = Rol.ADMIN.equals(usuario.getRol());

            int pendientes = admin
                    ? (int) solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE)
                    : 0;

            int nuevas = admin
                    ? 0
                    : (int) solicitudRepository.contarNuevasParaUsuario(usuario.getId(), EstadoSolicitud.PENDIENTE);

            int conteos = admin
                    ? (int) conteoRepository.countByEstado(EstadoConteo.PENDIENTE_REVISION)
                    : 0;

            int porAgotarse = (int) (productoRepository.countByCantidadBetween(1, 5)
                    + productoRepository.countByCantidad(0));

            List<ReabastecimientoService.Sugerencia> sugerencias = reabastecimientoService.calcular();

            int urgentes = (int) sugerencias.stream()
                    .filter(ReabastecimientoService.Sugerencia::isUrgente)
                    .count();

            return new Avisos(pendientes, nuevas, porAgotarse, sugerencias.size(), urgentes, conteos);

        } catch (Exception e) {
            return new Avisos(0, 0, 0, 0, 0, 0);
        }
    }
}