package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.DetalleVentaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import com.eternalsunshine.web.service.ProductoService;
import com.eternalsunshine.web.service.ReabastecimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ReabastecimientoService reabastecimientoService;

    @GetMapping("/")
    public String inicio(Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/login";
        }

        boolean admin = Rol.ADMIN.equals(usuario.getRol());

        model.addAttribute("usuario", usuario.getUsername());
        model.addAttribute("totalProductos", productoService.obtenerTotalProductos());
        model.addAttribute("stockBajo", productoService.obtenerStockBajo());
        model.addAttribute("agotados", productoService.obtenerAgotados());
        model.addAttribute("esAdmin", admin);

        model.addAttribute("porAgotarse",
                productoRepository.findTop5ByCantidadLessThanEqualOrderByCantidadAsc(5));

        if (admin) {
            model.addAttribute("valorInventario", productoService.obtenerValorInventario());
            cargarResumenAdmin(model);
        } else {
            cargarResumenEmpleado(model, usuario);
        }

        return "dashboard";
    }

    private void cargarResumenEmpleado(Model model, Usuario usuario) {

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();

        model.addAttribute("misVentasHoy",
                ventaRepository.contarPorUsuarioEntre(usuario.getId(), inicioHoy, inicioManana));
        model.addAttribute("misUnidadesHoy",
                detalleVentaRepository.unidadesPorUsuarioEntre(usuario.getId(), inicioHoy, inicioManana));
        model.addAttribute("miMontoHoy",
                ventaRepository.totalPorUsuarioEntre(usuario.getId(), inicioHoy, inicioManana));

        model.addAttribute("tiendaVentasHoy", ventaRepository.contarEntre(inicioHoy, inicioManana));
        model.addAttribute("tiendaUnidadesHoy", detalleVentaRepository.unidadesEntre(inicioHoy, inicioManana));
    }

    private void cargarResumenAdmin(Model model) {

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = hoy.plusDays(1).atStartOfDay();

        model.addAttribute("vendidoHoy", ventaRepository.totalEntre(inicioHoy, inicioManana));
        model.addAttribute("ventasHoy", ventaRepository.contarEntre(inicioHoy, inicioManana));
        model.addAttribute("unidadesHoy", detalleVentaRepository.unidadesEntre(inicioHoy, inicioManana));

        List<ReabastecimientoService.Sugerencia> sugerencias = reabastecimientoService.calcular();
        model.addAttribute("alertasReabastecimiento", sugerencias.size());
        model.addAttribute("alertasUrgentes",
                sugerencias.stream().filter(ReabastecimientoService.Sugerencia::isUrgente).count());

        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            porDia.put(hoy.minusDays(i), 0L);
        }

        for (Venta v : ventaRepository.ventasVigentesDesde(hoy.minusDays(6).atStartOfDay())) {
            LocalDate dia = v.getFecha().toLocalDate();
            porDia.computeIfPresent(dia, (k, acumulado) -> acumulado + v.getTotal());
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        List<String> etiquetas = new ArrayList<>();
        for (LocalDate dia : porDia.keySet()) {
            etiquetas.add(dia.format(formato));
        }

        model.addAttribute("etiquetasSemana", etiquetas);
        model.addAttribute("valoresSemana", new ArrayList<>(porDia.values()));

        List<Object[]> masVendidos = detalleVentaRepository.masVendidos(
                hoy.minusDays(30).atStartOfDay(), PageRequest.of(0, 5));

        List<String> nombresTop = new ArrayList<>();
        List<Long> cantidadesTop = new ArrayList<>();
        for (Object[] fila : masVendidos) {
            nombresTop.add((String) fila[0]);
            cantidadesTop.add(((Number) fila[1]).longValue());
        }

        model.addAttribute("nombresTop", nombresTop);
        model.addAttribute("cantidadesTop", cantidadesTop);
    }
}