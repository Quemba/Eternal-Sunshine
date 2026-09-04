package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.ProductoRepository;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;

@Controller
public class ExcelController {

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/productos/exportar")
    public void exportarExcel(
            HttpServletResponse response)
            throws IOException {

        response.setContentType(
                "application/octet-stream");

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=productos.xlsx");

        Workbook workbook =
                new XSSFWorkbook();

        Sheet sheet =
                workbook.createSheet("Productos");

        Row header =
                sheet.createRow(0);

        header.createCell(0)
                .setCellValue("Código");

        header.createCell(1)
                .setCellValue("Nombre");

        header.createCell(2)
                .setCellValue("Categoría");

        header.createCell(3)
                .setCellValue("Precio");

        header.createCell(4)
                .setCellValue("Cantidad");

        List<Producto> productos =
                productoRepository.findAll();

        int fila = 1;

        for (Producto p : productos) {

            Row row =
                    sheet.createRow(fila++);

            row.createCell(0)
                    .setCellValue(p.getCodigo());

            row.createCell(1)
                    .setCellValue(p.getNombre());

            row.createCell(2)
                    .setCellValue(p.getCategoria());

            row.createCell(3)
                    .setCellValue(p.getPrecio());

            row.createCell(4)
                    .setCellValue(p.getCantidad());
        }

        ServletOutputStream output =
                response.getOutputStream();

        workbook.write(output);

        workbook.close();

        output.close();
    }
}