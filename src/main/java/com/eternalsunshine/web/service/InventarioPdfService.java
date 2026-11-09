package com.eternalsunshine.web.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class InventarioPdfService {

    private static final Logger log = LoggerFactory.getLogger(InventarioPdfService.class);

    private static final Color ROSA = new Color(0xDF, 0x67, 0x94);
    private static final Color CREMA = new Color(0xF3, 0xE8, 0xD3);
    private static final Color ZEBRA = new Color(0xFB, 0xF6, 0xEC);
    private static final Color BLANCO = new Color(0xFF, 0xFF, 0xFF);
    private static final Color VINO = new Color(0x4A, 0x10, 0x20);
    private static final Color GRIS = new Color(0x6B, 0x5B, 0x60);
    private static final Color LINEA = new Color(0xE6, 0xDA, 0xC4);
    private static final Color ROJO = new Color(0xB8, 0x32, 0x55);
    private static final Color AMBAR = new Color(0x9A, 0x6A, 0x00);
    private static final Color VERDE = new Color(0x1F, 0x6B, 0x3A);
    private static final Color MORADO = new Color(0x8A, 0x5C, 0xB8);

    private String cop(long valor) {
        return "$ " + String.format(Locale.US, "%,d", valor).replace(',', '.');
    }

    private String porcentaje(double fraccion) {
        return String.format(Locale.US, "%.1f", fraccion * 100).replace('.', ',') + "%";
    }

    private String decimal(double valor) {
        return String.format(Locale.US, "%.1f", valor).replace('.', ',');
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(5f);
        celda.setBorderColor(LINEA);
        celda.setBorderWidth(0.5f);

        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }

        return celda;
    }

    private byte[] leerLogo() {
        try (InputStream entrada = new ClassPathResource("static/images/logo.png").getInputStream()) {
            return StreamUtils.copyToByteArray(entrada);
        } catch (Exception e) {
            log.warn("No se pudo leer el logo para el PDF: {}", e.toString());
            return null;
        }
    }

    private Paragraph titulo(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setSpacingBefore(16f);
        parrafo.setSpacingAfter(6f);
        return parrafo;
    }

    private void agregarIndicador(PdfPTable tabla, String etiqueta, String valor, Font fEtiqueta, Font fValor) {
        PdfPCell celda = new PdfPCell();
        celda.addElement(new Paragraph(etiqueta, fEtiqueta));
        celda.addElement(new Paragraph(valor, fValor));
        celda.setBackgroundColor(BLANCO);
        celda.setBorderColor(ROSA);
        celda.setBorderWidth(0.8f);
        celda.setPadding(8f);
        tabla.addCell(celda);
    }

    private static class PieDePagina extends PdfPageEventHelper {

        private final BaseFont base;
        private final String izquierda;
        private final String centro;
        private PdfTemplate total;

        PieDePagina(BaseFont base, String izquierda, String centro) {
            this.base = base;
            this.izquierda = izquierda;
            this.centro = centro;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {

            PdfContentByte lienzo = writer.getDirectContent();
            float y = document.bottom() - 18;

            lienzo.saveState();
            lienzo.setColorFill(GRIS);

            lienzo.beginText();
            lienzo.setFontAndSize(base, 8);
            lienzo.setTextMatrix(document.left(), y);
            lienzo.showText(izquierda);
            lienzo.endText();

            float anchoCentro = base.getWidthPoint(centro, 8);
            lienzo.beginText();
            lienzo.setFontAndSize(base, 8);
            lienzo.setTextMatrix((document.left() + document.right() - anchoCentro) / 2, y);
            lienzo.showText(centro);
            lienzo.endText();

            String texto = "Página " + writer.getPageNumber() + " de ";
            float ancho = base.getWidthPoint(texto, 8);
            float x = document.right() - ancho - 14;

            lienzo.beginText();
            lienzo.setFontAndSize(base, 8);
            lienzo.setTextMatrix(x, y);
            lienzo.showText(texto);
            lienzo.endText();

            lienzo.addTemplate(total, x + ancho, y);
            lienzo.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(base, 8);
            total.setColorFill(GRIS);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }

    public byte[] generar(InventarioReporteService.Reporte reporte, String usuario, LocalDateTime ahora)
            throws IOException {

        try {
            BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont negrita = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

            Font fTexto = new Font(base, 8.5f, Font.NORMAL, VINO);
            Font fNegrita = new Font(negrita, 8.5f, Font.NORMAL, VINO);
            Font fEncabezado = new Font(negrita, 8.5f, Font.NORMAL, BLANCO);
            Font fTitulo = new Font(negrita, 18f, Font.NORMAL, VINO);
            Font fSubtitulo = new Font(base, 9f, Font.NORMAL, GRIS);
            Font fSeccion = new Font(negrita, 12f, Font.NORMAL, ROSA);
            Font fNota = new Font(base, 7.5f, Font.NORMAL, GRIS);
            Font fIndicadorEtiqueta = new Font(base, 8f, Font.NORMAL, GRIS);
            Font fIndicadorValor = new Font(negrita, 15f, Font.NORMAL, VINO);
            Font fRojo = new Font(negrita, 8.5f, Font.NORMAL, ROJO);
            Font fAmbar = new Font(negrita, 8.5f, Font.NORMAL, AMBAR);
            Font fVerde = new Font(negrita, 8.5f, Font.NORMAL, VERDE);
            Font fRosa = new Font(negrita, 8.5f, Font.NORMAL, ROSA);
            Font fMorado = new Font(negrita, 8.5f, Font.NORMAL, MORADO);
            Font fGris = new Font(negrita, 8.5f, Font.NORMAL, GRIS);

            String fecha = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 44);
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new PieDePagina(base, "Eternal Sunshine", "Generado el " + fecha));

            documento.open();

            PdfPTable cabecera = new PdfPTable(new float[]{1f, 11f});
            cabecera.setWidthPercentage(100);

            PdfPCell celdaLogo;
            byte[] logo = leerLogo();

            try {
                if (logo != null) {
                    Image imagen = Image.getInstance(logo);
                    imagen.scaleToFit(50, 50);
                    celdaLogo = new PdfPCell(imagen, false);
                } else {
                    celdaLogo = new PdfPCell(new Phrase(""));
                }
            } catch (Exception e) {
                log.warn("No se pudo agregar el logo al PDF: {}", e.toString());
                celdaLogo = new PdfPCell(new Phrase(""));
            }

            celdaLogo.setBorder(Rectangle.NO_BORDER);
            celdaLogo.setBackgroundColor(CREMA);
            celdaLogo.setPadding(8f);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cabecera.addCell(celdaLogo);

            String descripcion = (reporte.getFiltro() != null)
                    ? "Filtro aplicado: «" + reporte.getFiltro() + "» · " + reporte.getProductos()
                            + (reporte.getProductos() == 1 ? " producto" : " productos")
                    : reporte.getProductos() + (reporte.getProductos() == 1 ? " producto registrado" : " productos registrados");

            PdfPCell celdaTitulo = new PdfPCell();
            celdaTitulo.addElement(new Paragraph("Eternal Sunshine · Inventario de productos", fTitulo));
            celdaTitulo.addElement(new Paragraph("Generado el " + fecha + " por " + usuario, fSubtitulo));
            celdaTitulo.addElement(new Paragraph(descripcion, fSubtitulo));
            celdaTitulo.setBorder(Rectangle.NO_BORDER);
            celdaTitulo.setBackgroundColor(CREMA);
            celdaTitulo.setPadding(8f);
            cabecera.addCell(celdaTitulo);

            documento.add(cabecera);

            PdfPTable indicadores = new PdfPTable(5);
            indicadores.setWidthPercentage(100);
            indicadores.setSpacingBefore(10f);

            agregarIndicador(indicadores, "Productos", String.valueOf(reporte.getProductos()), fIndicadorEtiqueta, fIndicadorValor);
            agregarIndicador(indicadores, "Unidades en stock", String.format(Locale.US, "%,d", reporte.getUnidades()).replace(',', '.'), fIndicadorEtiqueta, fIndicadorValor);
            agregarIndicador(indicadores, "Valor del inventario", cop(reporte.getValor()), fIndicadorEtiqueta, fIndicadorValor);
            agregarIndicador(indicadores, "Agotados · Stock bajo", reporte.getAgotados() + " · " + reporte.getStockBajo(), fIndicadorEtiqueta, fIndicadorValor);
            agregarIndicador(indicadores, "Vendidas (últimos " + InventarioReporteService.VENTANA_DIAS + " días)", String.valueOf(reporte.getVendidas()), fIndicadorEtiqueta, fIndicadorValor);

            documento.add(indicadores);

            PdfPTable tabla = new PdfPTable(new float[]{1.7f, 3.8f, 2.1f, 1.3f, 0.9f, 1.6f, 1.0f, 1.1f, 1.1f, 0.8f, 1.4f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(12f);
            tabla.setHeaderRows(1);

            String[] encabezados = {"Código", "Producto", "Categoría", "Precio", "Cant.", "Valor",
                    "% valor", "Vend. 30 d", "Días stock", "ABC", "Estado"};

            for (int c = 0; c < encabezados.length; c++) {
                tabla.addCell(celda(encabezados[c], fEncabezado, Element.ALIGN_CENTER, ROSA));
            }

            List<InventarioReporteService.FilaInventario> filas = reporte.getFilas();

            if (filas.isEmpty()) {
                PdfPCell vacia = celda("No hay productos para exportar.", fTexto, Element.ALIGN_CENTER, BLANCO);
                vacia.setColspan(11);
                vacia.setPadding(14f);
                tabla.addCell(vacia);
            }

            for (int i = 0; i < filas.size(); i++) {

                InventarioReporteService.FilaInventario f = filas.get(i);
                Color fondo = (i % 2 == 0) ? BLANCO : ZEBRA;

                Font fEstado = "Agotado".equals(f.getEstado()) ? fRojo
                        : ("Stock bajo".equals(f.getEstado()) ? fAmbar : fVerde);

                Font fClase = "A".equals(f.getClaseAbc()) ? fRosa
                        : ("B".equals(f.getClaseAbc()) ? fMorado : fGris);

                tabla.addCell(celda(f.getCodigo(), fTexto, Element.ALIGN_LEFT, fondo));
                tabla.addCell(celda(f.getNombre(), fTexto, Element.ALIGN_LEFT, fondo));
                tabla.addCell(celda(f.getCategoria(), fTexto, Element.ALIGN_LEFT, fondo));
                tabla.addCell(celda(cop(f.getPrecio()), fTexto, Element.ALIGN_RIGHT, fondo));
                tabla.addCell(celda(String.valueOf(f.getCantidad()), fTexto, Element.ALIGN_CENTER, fondo));
                tabla.addCell(celda(cop(f.getValor()), fTexto, Element.ALIGN_RIGHT, fondo));
                tabla.addCell(celda(porcentaje(f.getParticipacion()), fTexto, Element.ALIGN_CENTER, fondo));
                tabla.addCell(celda(String.valueOf(f.getVendidas()), fTexto, Element.ALIGN_CENTER, fondo));
                tabla.addCell(celda(f.getDiasStock() == null ? "Sin ventas" : decimal(f.getDiasStock()),
                        fTexto, Element.ALIGN_CENTER, fondo));
                tabla.addCell(celda(f.getClaseAbc(), fClase, Element.ALIGN_CENTER, fondo));
                tabla.addCell(celda(f.getEstado(), fEstado, Element.ALIGN_CENTER, fondo));
            }

            if (!filas.isEmpty()) {
                double sumaParticipacion = 0;

                for (InventarioReporteService.FilaInventario f : filas) {
                    sumaParticipacion += f.getParticipacion();
                }

                PdfPCell etiquetaTotal = celda("Total", fNegrita, Element.ALIGN_RIGHT, CREMA);
                etiquetaTotal.setColspan(4);
                tabla.addCell(etiquetaTotal);
                tabla.addCell(celda(String.valueOf(reporte.getUnidades()), fNegrita, Element.ALIGN_CENTER, CREMA));
                tabla.addCell(celda(cop(reporte.getValor()), fNegrita, Element.ALIGN_RIGHT, CREMA));
                tabla.addCell(celda(porcentaje(sumaParticipacion), fNegrita, Element.ALIGN_CENTER, CREMA));
                tabla.addCell(celda(String.valueOf(reporte.getVendidas()), fNegrita, Element.ALIGN_CENTER, CREMA));
                tabla.addCell(celda("", fNegrita, Element.ALIGN_CENTER, CREMA));
                tabla.addCell(celda("", fNegrita, Element.ALIGN_CENTER, CREMA));
                tabla.addCell(celda("", fNegrita, Element.ALIGN_CENTER, CREMA));
            }

            documento.add(tabla);

            if (!reporte.getCategorias().isEmpty()) {

                documento.add(titulo("Resumen por categoría", fSeccion));

                PdfPTable porCategoria = new PdfPTable(new float[]{4f, 1.5f, 1.5f, 2.5f, 1.8f});
                porCategoria.setWidthPercentage(70);
                porCategoria.setHorizontalAlignment(Element.ALIGN_LEFT);
                porCategoria.setHeaderRows(1);

                String[] titulosCategoria = {"Categoría", "Productos", "Unidades", "Valor", "% del valor"};

                for (String t : titulosCategoria) {
                    porCategoria.addCell(celda(t, fEncabezado, Element.ALIGN_CENTER, ROSA));
                }

                int numero = 0;

                for (InventarioReporteService.ResumenCategoria c : reporte.getCategorias()) {
                    Color fondo = (numero++ % 2 == 0) ? BLANCO : ZEBRA;

                    porCategoria.addCell(celda(c.getCategoria(), fTexto, Element.ALIGN_LEFT, fondo));
                    porCategoria.addCell(celda(String.valueOf(c.getProductos()), fTexto, Element.ALIGN_CENTER, fondo));
                    porCategoria.addCell(celda(String.valueOf(c.getUnidades()), fTexto, Element.ALIGN_CENTER, fondo));
                    porCategoria.addCell(celda(cop(c.getValor()), fTexto, Element.ALIGN_RIGHT, fondo));
                    porCategoria.addCell(celda(porcentaje(c.getParticipacion()), fTexto, Element.ALIGN_CENTER, fondo));
                }

                documento.add(porCategoria);

                documento.add(titulo("Clasificación ABC", fSeccion));

                PdfPTable tablaAbc = new PdfPTable(new float[]{1.2f, 1.5f, 2.5f, 1.8f, 5f});
                tablaAbc.setWidthPercentage(70);
                tablaAbc.setHorizontalAlignment(Element.ALIGN_LEFT);
                tablaAbc.setHeaderRows(1);

                String[] titulosAbc = {"Clase", "Productos", "Valor", "% del valor", "Criterio"};

                for (String t : titulosAbc) {
                    tablaAbc.addCell(celda(t, fEncabezado, Element.ALIGN_CENTER, ROSA));
                }

                numero = 0;

                for (InventarioReporteService.ResumenAbc a : reporte.getAbc()) {
                    Color fondo = (numero++ % 2 == 0) ? BLANCO : ZEBRA;

                    tablaAbc.addCell(celda(a.getClase(), fNegrita, Element.ALIGN_CENTER, fondo));
                    tablaAbc.addCell(celda(String.valueOf(a.getProductos()), fTexto, Element.ALIGN_CENTER, fondo));
                    tablaAbc.addCell(celda(cop(a.getValor()), fTexto, Element.ALIGN_RIGHT, fondo));
                    tablaAbc.addCell(celda(porcentaje(a.getParticipacion()), fTexto, Element.ALIGN_CENTER, fondo));
                    tablaAbc.addCell(celda(a.getCriterio(), fTexto, Element.ALIGN_LEFT, fondo));
                }

                documento.add(tablaAbc);
            }

            Paragraph nota = new Paragraph(
                    "Días de stock = stock actual ÷ promedio diario de ventas de los últimos "
                            + InventarioReporteService.VENTANA_DIAS + " días. "
                            + "El porcentaje del valor y la clase ABC se calculan sobre el valor de todo el inventario "
                            + "(A: hasta el 80% acumulado, B: hasta el 95%, C: el resto).",
                    fNota);
            nota.setSpacingBefore(14f);
            documento.add(nota);

            documento.close();

            return salida.toByteArray();

        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF del inventario.", e);
        }
    }
}