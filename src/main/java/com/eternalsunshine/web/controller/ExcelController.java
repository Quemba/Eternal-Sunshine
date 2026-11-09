package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.InventarioPdfService;
import com.eternalsunshine.web.service.InventarioReporteService;
import com.eternalsunshine.web.service.InventarioReporteService.FilaInventario;
import com.eternalsunshine.web.service.InventarioReporteService.Reporte;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ExcelController {

    private static final Logger log = LoggerFactory.getLogger(ExcelController.class);

    private static final byte[] ROSA = rgb(0xDF, 0x67, 0x94);
    private static final byte[] CREMA = rgb(0xF3, 0xE8, 0xD3);
    private static final byte[] ZEBRA = rgb(0xFB, 0xF6, 0xEC);
    private static final byte[] BLANCO = rgb(0xFF, 0xFF, 0xFF);
    private static final byte[] VINO = rgb(0x4A, 0x10, 0x20);
    private static final byte[] GRIS = rgb(0x6B, 0x5B, 0x60);
    private static final byte[] LINEA = rgb(0xE6, 0xDA, 0xC4);
    private static final byte[] ROJO = rgb(0xB8, 0x32, 0x55);
    private static final byte[] AMBAR = rgb(0x9A, 0x6A, 0x00);
    private static final byte[] VERDE = rgb(0x1F, 0x6B, 0x3A);
    private static final byte[] MORADO = rgb(0x8A, 0x5C, 0xB8);

    private static final int FILA_ENCABEZADO = 4;
    private static final int PRIMERA_FILA_DATOS = 5;
    private static final int TOTAL_COLUMNAS = 11;
    private static final int EMU_POR_PIXEL = 9525;

    @Autowired
    private InventarioReporteService reporteService;

    @Autowired
    private InventarioPdfService pdfService;

    private static byte[] rgb(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    private XSSFFont fuente(XSSFWorkbook libro, int tamano, boolean negrita, byte[] color) {
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Calibri");
        fuente.setFontHeightInPoints((short) tamano);
        fuente.setBold(negrita);
        fuente.setColor(new XSSFColor(color, null));
        return fuente;
    }

    private XSSFCellStyle estilo(XSSFWorkbook libro, byte[] fondo, XSSFFont fuente,
                                 HorizontalAlignment horizontal, String formato, boolean bordes) {

        XSSFCellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(horizontal);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);

        if (fondo != null) {
            estilo.setFillForegroundColor(new XSSFColor(fondo, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        if (formato != null) {
            estilo.setDataFormat(libro.createDataFormat().getFormat(formato));
        }

        if (bordes) {
            XSSFColor linea = new XSSFColor(LINEA, null);

            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);

            estilo.setTopBorderColor(linea);
            estilo.setBottomBorderColor(linea);
            estilo.setLeftBorderColor(linea);
            estilo.setRightBorderColor(linea);
        }

        return estilo;
    }

    private Cell celda(Row fila, int columna, XSSFCellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellStyle(estilo);
        return celda;
    }

    private int tipoImagen(byte[] b) {

        if (b.length > 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
            return Workbook.PICTURE_TYPE_PNG;
        }

        if (b.length > 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8) {
            return Workbook.PICTURE_TYPE_JPEG;
        }

        return -1;
    }

    private void agregarLogo(XSSFWorkbook libro, XSSFSheet hoja) {

        try (InputStream entrada = new ClassPathResource("static/images/logo.png").getInputStream()) {

            byte[] bytes = StreamUtils.copyToByteArray(entrada);
            int tipo = tipoImagen(bytes);

            if (tipo < 0) {
                log.warn("El logo del Excel no es PNG ni JPEG; el archivo se genera sin logo.");
                return;
            }

            double proporcion = 1.0;

            try {
                BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(bytes));

                if (imagen != null && imagen.getHeight() > 0) {
                    proporcion = (double) imagen.getWidth() / imagen.getHeight();
                }
            } catch (Exception e) {
                log.warn("No se pudo leer el tamaño del logo, se usa un recuadro cuadrado: {}", e.toString());
            }

            int alto = 62;
            int ancho = (int) Math.round(alto * proporcion);

            if (ancho > 120) {
                ancho = 120;
                alto = (int) Math.round(ancho / proporcion);
            }

            int x1 = 8;
            int y1 = 5;
            int x2 = x1 + ancho;
            double y2 = y1 + alto;

            double alturaFilaCeroPx = 32 * 96.0 / 72.0;

            int filaFin;
            double desplazamientoFin;

            if (y2 <= alturaFilaCeroPx) {
                filaFin = 0;
                desplazamientoFin = y2;
            } else {
                filaFin = 1;
                desplazamientoFin = Math.min(y2 - alturaFilaCeroPx, 25.0);
            }

            XSSFClientAnchor ancla = new XSSFClientAnchor(
                    x1 * EMU_POR_PIXEL,
                    y1 * EMU_POR_PIXEL,
                    x2 * EMU_POR_PIXEL,
                    (int) Math.round(desplazamientoFin * EMU_POR_PIXEL),
                    0, 0, 0, filaFin);

            ancla.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);

            int indice = libro.addPicture(bytes, tipo);
            XSSFDrawing dibujo = hoja.createDrawingPatriarch();
            dibujo.createPicture(ancla, indice);

        } catch (Exception e) {
            log.warn("No se pudo agregar el logo al Excel: {}", e.toString());
        }
    }

    private void construirInventario(XSSFWorkbook libro, Reporte reporte, Usuario usuario, LocalDateTime ahora) {

        XSSFSheet hoja = libro.createSheet("Inventario");
        hoja.setDisplayGridlines(false);

        List<FilaInventario> filas = reporte.getFilas();
        int cantidadProductos = filas.size();

        XSSFFont fBase = fuente(libro, 11, false, VINO);
        XSSFFont fNegrita = fuente(libro, 11, true, VINO);
        XSSFFont fEncabezado = fuente(libro, 11, true, BLANCO);
        XSSFFont fTitulo = fuente(libro, 20, true, VINO);
        XSSFFont fSubtitulo = fuente(libro, 11, false, GRIS);
        XSSFFont fRojo = fuente(libro, 11, true, ROJO);
        XSSFFont fAmbar = fuente(libro, 11, true, AMBAR);
        XSSFFont fVerde = fuente(libro, 11, true, VERDE);
        XSSFFont fRosa = fuente(libro, 11, true, ROSA);
        XSSFFont fMorado = fuente(libro, 11, true, MORADO);
        XSSFFont fGris = fuente(libro, 11, true, GRIS);

        XSSFCellStyle banda = estilo(libro, CREMA, fSubtitulo, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle titulo = estilo(libro, CREMA, fTitulo, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle encabezado = estilo(libro, ROSA, fEncabezado, HorizontalAlignment.CENTER, null, true);

        XSSFCellStyle[] texto = new XSSFCellStyle[2];
        XSSFCellStyle[] centro = new XSSFCellStyle[2];
        XSSFCellStyle[] dinero = new XSSFCellStyle[2];
        XSSFCellStyle[] entero = new XSSFCellStyle[2];
        XSSFCellStyle[] porcentaje = new XSSFCellStyle[2];
        XSSFCellStyle[] decimal = new XSSFCellStyle[2];
        XSSFCellStyle[] estadoRojo = new XSSFCellStyle[2];
        XSSFCellStyle[] estadoAmbar = new XSSFCellStyle[2];
        XSSFCellStyle[] estadoVerde = new XSSFCellStyle[2];
        XSSFCellStyle[] claseA = new XSSFCellStyle[2];
        XSSFCellStyle[] claseB = new XSSFCellStyle[2];
        XSSFCellStyle[] claseC = new XSSFCellStyle[2];

        for (int i = 0; i < 2; i++) {
            byte[] fondo = (i == 0) ? BLANCO : ZEBRA;

            texto[i] = estilo(libro, fondo, fBase, HorizontalAlignment.LEFT, null, true);
            centro[i] = estilo(libro, fondo, fBase, HorizontalAlignment.CENTER, null, true);
            dinero[i] = estilo(libro, fondo, fBase, HorizontalAlignment.RIGHT, "\"$\" #,##0", true);
            entero[i] = estilo(libro, fondo, fBase, HorizontalAlignment.CENTER, "#,##0", true);
            porcentaje[i] = estilo(libro, fondo, fBase, HorizontalAlignment.CENTER, "0.0%", true);
            decimal[i] = estilo(libro, fondo, fBase, HorizontalAlignment.CENTER, "0.0", true);
            estadoRojo[i] = estilo(libro, fondo, fRojo, HorizontalAlignment.CENTER, null, true);
            estadoAmbar[i] = estilo(libro, fondo, fAmbar, HorizontalAlignment.CENTER, null, true);
            estadoVerde[i] = estilo(libro, fondo, fVerde, HorizontalAlignment.CENTER, null, true);
            claseA[i] = estilo(libro, fondo, fRosa, HorizontalAlignment.CENTER, null, true);
            claseB[i] = estilo(libro, fondo, fMorado, HorizontalAlignment.CENTER, null, true);
            claseC[i] = estilo(libro, fondo, fGris, HorizontalAlignment.CENTER, null, true);
        }

        XSSFCellStyle totalTexto = estilo(libro, CREMA, fNegrita, HorizontalAlignment.RIGHT, null, true);
        XSSFCellStyle totalEntero = estilo(libro, CREMA, fNegrita, HorizontalAlignment.CENTER, "#,##0", true);
        XSSFCellStyle totalDinero = estilo(libro, CREMA, fNegrita, HorizontalAlignment.RIGHT, "\"$\" #,##0", true);
        XSSFCellStyle totalPorcentaje = estilo(libro, CREMA, fNegrita, HorizontalAlignment.CENTER, "0.0%", true);
        XSSFCellStyle totalVacio = estilo(libro, CREMA, fNegrita, HorizontalAlignment.LEFT, null, true);

        String fecha = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String descripcion = (reporte.getFiltro() != null)
                ? "Filtro aplicado: «" + reporte.getFiltro() + "» · " + cantidadProductos
                        + (cantidadProductos == 1 ? " producto" : " productos")
                : cantidadProductos + (cantidadProductos == 1 ? " producto registrado" : " productos registrados");

        Row fila0 = hoja.createRow(0);
        Row fila1 = hoja.createRow(1);
        Row fila2 = hoja.createRow(2);
        fila0.setHeightInPoints(32);
        fila1.setHeightInPoints(20);
        fila2.setHeightInPoints(20);

        for (int c = 0; c < TOTAL_COLUMNAS; c++) {
            celda(fila0, c, banda);
            celda(fila1, c, banda);
            celda(fila2, c, banda);
        }

        fila0.getCell(1).setCellStyle(titulo);
        fila0.getCell(1).setCellValue("Eternal Sunshine · Inventario de productos");
        fila1.getCell(1).setCellValue("Generado el " + fecha + " por " + usuario.getUsername());
        fila2.getCell(1).setCellValue(descripcion);

        hoja.addMergedRegion(new CellRangeAddress(0, 0, 1, TOTAL_COLUMNAS - 1));
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 1, TOTAL_COLUMNAS - 1));
        hoja.addMergedRegion(new CellRangeAddress(2, 2, 1, TOTAL_COLUMNAS - 1));

        hoja.createRow(3).setHeightInPoints(10);

        String[] titulos = {"Código", "Nombre", "Categoría", "Precio (COP)", "Cantidad", "Valor en inventario",
                "% del valor", "Vendidas (30 d)", "Días de stock", "Clase ABC", "Estado"};

        Row filaEncabezado = hoja.createRow(FILA_ENCABEZADO);
        filaEncabezado.setHeightInPoints(26);

        for (int c = 0; c < titulos.length; c++) {
            celda(filaEncabezado, c, encabezado).setCellValue(titulos[c]);
        }

        int anchoNombre = 30;
        int anchoCategoria = 18;

        for (int i = 0; i < cantidadProductos; i++) {

            FilaInventario f = filas.get(i);
            int indiceFila = PRIMERA_FILA_DATOS + i;
            int numeroExcel = indiceFila + 1;
            int variante = i % 2;

            Row fila = hoja.createRow(indiceFila);
            fila.setHeightInPoints(21);

            XSSFCellStyle estiloEstado = "Agotado".equals(f.getEstado())
                    ? estadoRojo[variante]
                    : ("Stock bajo".equals(f.getEstado()) ? estadoAmbar[variante] : estadoVerde[variante]);

            XSSFCellStyle estiloClase = "A".equals(f.getClaseAbc())
                    ? claseA[variante]
                    : ("B".equals(f.getClaseAbc()) ? claseB[variante] : claseC[variante]);

            celda(fila, 0, texto[variante]).setCellValue(f.getCodigo());
            celda(fila, 1, texto[variante]).setCellValue(f.getNombre());
            celda(fila, 2, texto[variante]).setCellValue(f.getCategoria());
            celda(fila, 3, dinero[variante]).setCellValue(f.getPrecio());
            celda(fila, 4, entero[variante]).setCellValue(f.getCantidad());
            celda(fila, 5, dinero[variante]).setCellFormula("D" + numeroExcel + "*E" + numeroExcel);
            celda(fila, 6, porcentaje[variante]).setCellValue(f.getParticipacion());
            celda(fila, 7, entero[variante]).setCellValue(f.getVendidas());

            if (f.getDiasStock() == null) {
                celda(fila, 8, centro[variante]).setCellValue("Sin ventas");
            } else {
                celda(fila, 8, decimal[variante]).setCellValue(Math.round(f.getDiasStock() * 10.0) / 10.0);
            }

            celda(fila, 9, estiloClase).setCellValue(f.getClaseAbc());
            celda(fila, 10, estiloEstado).setCellValue(f.getEstado());

            anchoNombre = Math.max(anchoNombre, Math.min(f.getNombre().length() + 4, 60));
            anchoCategoria = Math.max(anchoCategoria, Math.min(f.getCategoria().length() + 4, 34));
        }

        int ultimaFilaDatos = PRIMERA_FILA_DATOS + cantidadProductos - 1;

        if (cantidadProductos == 0) {
            Row vacia = hoja.createRow(PRIMERA_FILA_DATOS);
            vacia.setHeightInPoints(28);

            for (int c = 0; c < TOTAL_COLUMNAS; c++) {
                celda(vacia, c, texto[0]);
            }

            vacia.getCell(0).setCellValue("No hay productos para exportar.");
            hoja.addMergedRegion(new CellRangeAddress(PRIMERA_FILA_DATOS, PRIMERA_FILA_DATOS, 0, TOTAL_COLUMNAS - 1));

        } else {
            int primeraExcel = PRIMERA_FILA_DATOS + 1;
            int ultimaExcel = ultimaFilaDatos + 1;

            Row filaTotal = hoja.createRow(ultimaFilaDatos + 1);
            filaTotal.setHeightInPoints(24);

            for (int c = 0; c < TOTAL_COLUMNAS; c++) {
                celda(filaTotal, c, totalVacio);
            }

            celda(filaTotal, 3, totalTexto).setCellValue("Total");
            celda(filaTotal, 4, totalEntero).setCellFormula("SUM(E" + primeraExcel + ":E" + ultimaExcel + ")");
            celda(filaTotal, 5, totalDinero).setCellFormula("SUM(F" + primeraExcel + ":F" + ultimaExcel + ")");
            celda(filaTotal, 6, totalPorcentaje).setCellFormula("SUM(G" + primeraExcel + ":G" + ultimaExcel + ")");
            celda(filaTotal, 7, totalEntero).setCellFormula("SUM(H" + primeraExcel + ":H" + ultimaExcel + ")");

            hoja.setAutoFilter(new CellRangeAddress(FILA_ENCABEZADO, ultimaFilaDatos, 0, TOTAL_COLUMNAS - 1));
        }

        hoja.setColumnWidth(0, 18 * 256);
        hoja.setColumnWidth(1, anchoNombre * 256);
        hoja.setColumnWidth(2, anchoCategoria * 256);
        hoja.setColumnWidth(3, 19 * 256);
        hoja.setColumnWidth(4, 15 * 256);
        hoja.setColumnWidth(5, 25 * 256);
        hoja.setColumnWidth(6, 16 * 256);
        hoja.setColumnWidth(7, 20 * 256);
        hoja.setColumnWidth(8, 18 * 256);
        hoja.setColumnWidth(9, 16 * 256);
        hoja.setColumnWidth(10, 18 * 256);

        hoja.createFreezePane(0, PRIMERA_FILA_DATOS);

        hoja.getPrintSetup().setLandscape(true);
        hoja.setFitToPage(true);
        hoja.getPrintSetup().setFitWidth((short) 1);
        hoja.getPrintSetup().setFitHeight((short) 0);
        hoja.setRepeatingRows(CellRangeAddress.valueOf((FILA_ENCABEZADO + 1) + ":" + (FILA_ENCABEZADO + 1)));
        hoja.getFooter().setLeft("Eternal Sunshine");
        hoja.getFooter().setRight("Página &P de &N");

        agregarLogo(libro, hoja);
    }

    private void construirResumen(XSSFWorkbook libro, Reporte reporte, LocalDateTime ahora) {

        XSSFSheet hoja = libro.createSheet("Resumen");
        hoja.setDisplayGridlines(false);

        int cantidadProductos = reporte.getProductos();

        XSSFFont fBase = fuente(libro, 12, false, VINO);
        XSSFFont fNegrita = fuente(libro, 12, true, VINO);
        XSSFFont fEncabezado = fuente(libro, 12, true, BLANCO);
        XSSFFont fTitulo = fuente(libro, 18, true, VINO);
        XSSFFont fSeccion = fuente(libro, 14, true, ROSA);
        XSSFFont fSubtitulo = fuente(libro, 11, false, GRIS);

        XSSFCellStyle titulo = estilo(libro, CREMA, fTitulo, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle subtitulo = estilo(libro, CREMA, fSubtitulo, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle seccion = estilo(libro, null, fSeccion, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle nota = estilo(libro, null, fSubtitulo, HorizontalAlignment.LEFT, null, false);
        XSSFCellStyle encabezado = estilo(libro, ROSA, fEncabezado, HorizontalAlignment.CENTER, null, true);

        XSSFCellStyle[] etiqueta = new XSSFCellStyle[2];
        XSSFCellStyle[] numero = new XSSFCellStyle[2];
        XSSFCellStyle[] moneda = new XSSFCellStyle[2];
        XSSFCellStyle[] porcentaje = new XSSFCellStyle[2];
        XSSFCellStyle[] centro = new XSSFCellStyle[2];

        for (int i = 0; i < 2; i++) {
            byte[] fondo = (i == 0) ? BLANCO : ZEBRA;

            etiqueta[i] = estilo(libro, fondo, fBase, HorizontalAlignment.LEFT, null, true);
            numero[i] = estilo(libro, fondo, fNegrita, HorizontalAlignment.CENTER, "#,##0", true);
            moneda[i] = estilo(libro, fondo, fNegrita, HorizontalAlignment.RIGHT, "\"$\" #,##0", true);
            porcentaje[i] = estilo(libro, fondo, fBase, HorizontalAlignment.CENTER, "0.0%", true);
            centro[i] = estilo(libro, fondo, fNegrita, HorizontalAlignment.CENTER, null, true);
        }

        for (int r = 0; r < 2; r++) {
            Row fila = hoja.createRow(r);
            fila.setHeightInPoints(r == 0 ? 32 : 20);

            for (int c = 0; c < 5; c++) {
                celda(fila, c, r == 0 ? titulo : subtitulo);
            }
        }

        hoja.getRow(0).getCell(0).setCellValue("Resumen del inventario");
        hoja.getRow(1).getCell(0).setCellValue(
                "Generado el " + ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

        int indice = 3;

        Row filaSeccion = hoja.createRow(indice++);
        filaSeccion.setHeightInPoints(24);
        celda(filaSeccion, 0, seccion).setCellValue("Indicadores");

        Row filaEncabezado = hoja.createRow(indice++);
        filaEncabezado.setHeightInPoints(26);
        celda(filaEncabezado, 0, encabezado).setCellValue("Indicador");
        celda(filaEncabezado, 1, encabezado).setCellValue("Valor");

        String[] indicadores = {
                "Productos registrados",
                "Unidades en stock",
                "Valor del inventario",
                "Productos agotados",
                "Productos con stock bajo",
                "Unidades vendidas (últimos 30 días)"
        };

        int primera = PRIMERA_FILA_DATOS + 1;
        int ultima = PRIMERA_FILA_DATOS + cantidadProductos;

        String[] formulas = new String[6];

        if (cantidadProductos > 0) {
            formulas[0] = "COUNTA(Inventario!A" + primera + ":A" + ultima + ")";
            formulas[1] = "SUM(Inventario!E" + primera + ":E" + ultima + ")";
            formulas[2] = "SUM(Inventario!F" + primera + ":F" + ultima + ")";
            formulas[3] = "COUNTIF(Inventario!K" + primera + ":K" + ultima + ",\"Agotado\")";
            formulas[4] = "COUNTIF(Inventario!K" + primera + ":K" + ultima + ",\"Stock bajo\")";
            formulas[5] = "SUM(Inventario!H" + primera + ":H" + ultima + ")";
        }

        for (int i = 0; i < indicadores.length; i++) {

            Row fila = hoja.createRow(indice++);
            fila.setHeightInPoints(24);

            int variante = i % 2;

            celda(fila, 0, etiqueta[variante]).setCellValue(indicadores[i]);

            Cell valor = celda(fila, 1, i == 2 ? moneda[variante] : numero[variante]);

            if (cantidadProductos > 0) {
                valor.setCellFormula(formulas[i]);
            } else {
                valor.setCellValue(0);
            }
        }

        indice++;

        Row filaSeccionCategorias = hoja.createRow(indice++);
        filaSeccionCategorias.setHeightInPoints(24);
        celda(filaSeccionCategorias, 0, seccion).setCellValue("Por categoría");

        Row encabezadoCategorias = hoja.createRow(indice++);
        encabezadoCategorias.setHeightInPoints(26);

        String[] titulosCategorias = {"Categoría", "Productos", "Unidades", "Valor", "% del valor"};

        for (int c = 0; c < titulosCategorias.length; c++) {
            celda(encabezadoCategorias, c, encabezado).setCellValue(titulosCategorias[c]);
        }

        int numeroFila = 0;

        for (InventarioReporteService.ResumenCategoria categoria : reporte.getCategorias()) {

            Row fila = hoja.createRow(indice++);
            fila.setHeightInPoints(22);

            int variante = (numeroFila++) % 2;

            celda(fila, 0, etiqueta[variante]).setCellValue(categoria.getCategoria());
            celda(fila, 1, numero[variante]).setCellValue(categoria.getProductos());
            celda(fila, 2, numero[variante]).setCellValue(categoria.getUnidades());
            celda(fila, 3, moneda[variante]).setCellValue(categoria.getValor());
            celda(fila, 4, porcentaje[variante]).setCellValue(categoria.getParticipacion());
        }

        indice++;

        Row filaSeccionAbc = hoja.createRow(indice++);
        filaSeccionAbc.setHeightInPoints(24);
        celda(filaSeccionAbc, 0, seccion).setCellValue("Clasificación ABC");

        Row encabezadoAbc = hoja.createRow(indice++);
        encabezadoAbc.setHeightInPoints(26);

        String[] titulosAbc = {"Clase", "Productos", "Valor", "% del valor", "Criterio"};

        for (int c = 0; c < titulosAbc.length; c++) {
            celda(encabezadoAbc, c, encabezado).setCellValue(titulosAbc[c]);
        }

        numeroFila = 0;

        for (InventarioReporteService.ResumenAbc abc : reporte.getAbc()) {

            Row fila = hoja.createRow(indice++);
            fila.setHeightInPoints(22);

            int variante = (numeroFila++) % 2;

            celda(fila, 0, centro[variante]).setCellValue(abc.getClase());
            celda(fila, 1, numero[variante]).setCellValue(abc.getProductos());
            celda(fila, 2, moneda[variante]).setCellValue(abc.getValor());
            celda(fila, 3, porcentaje[variante]).setCellValue(abc.getParticipacion());
            celda(fila, 4, etiqueta[variante]).setCellValue(abc.getCriterio());
        }

        indice++;

        Row filaNota = hoja.createRow(indice);
        celda(filaNota, 0, nota).setCellValue(
                "Las tablas de este resumen se calculan sobre los productos incluidos en el archivo. "
                        + "El % del valor y la clase ABC de cada producto se calculan sobre todo el inventario.");

        hoja.setColumnWidth(0, 38 * 256);
        hoja.setColumnWidth(1, 16 * 256);
        hoja.setColumnWidth(2, 16 * 256);
        hoja.setColumnWidth(3, 20 * 256);
        hoja.setColumnWidth(4, 40 * 256);

        hoja.setFitToPage(true);
        hoja.getPrintSetup().setFitWidth((short) 1);
        hoja.getPrintSetup().setFitHeight((short) 0);
        hoja.getFooter().setLeft("Eternal Sunshine");
    }

    private byte[] construirExcel(Reporte reporte, Usuario usuario, LocalDateTime ahora) throws IOException {

        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try (XSSFWorkbook libro = new XSSFWorkbook()) {

            construirInventario(libro, reporte, usuario, ahora);
            construirResumen(libro, reporte, ahora);

            libro.getProperties().getCoreProperties().setTitle("Inventario de productos");
            libro.getProperties().getCoreProperties().setCreator("Eternal Sunshine");

            libro.getCreationHelper().createFormulaEvaluator().evaluateAll();
            libro.setActiveSheet(0);

            libro.write(salida);
        }

        return salida.toByteArray();
    }

    @GetMapping("/productos/exportar")
    public void exportar(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "excel") String formato,
            HttpServletResponse response,
            HttpSession session)
            throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null || !Rol.ADMIN.equals(usuario.getRol())) {
            response.sendRedirect("/productos");
            return;
        }

        Reporte reporte = reporteService.generar(buscar, categoria, estado);

        LocalDateTime ahora = LocalDateTime.now();
        String fechaArchivo = ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        boolean esPdf = "pdf".equalsIgnoreCase(formato);

        byte[] contenido = esPdf
                ? pdfService.generar(reporte, usuario.getUsername(), ahora)
                : construirExcel(reporte, usuario, ahora);

        String nombreArchivo = "inventario-eternal-sunshine-" + fechaArchivo + (esPdf ? ".pdf" : ".xlsx");

        response.setContentType(esPdf
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        response.setContentLength(contenido.length);

        response.getOutputStream().write(contenido);
        response.getOutputStream().flush();
    }
}