package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelReportManager {

    public byte[] generarReporteVentas(List<VwDashboardVentas> datos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Reporte de Ventas");
            Row headerRow = sheet.createRow(0);

            // Cabeceras
            String[] columnas = {"Fecha", "Cantidad de Pedidos", "Total Ingresos (S/)"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
            }

            // Datos
            int rowIdx = 1;
            for (VwDashboardVentas venta : datos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(venta.getFecha().toString());
                row.createCell(1).setCellValue(venta.getCantidadPedidos());
                row.createCell(2).setCellValue(venta.getTotalIngresos().doubleValue());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // Reemplazamos la excepción genérica por la de negocio de la Fase 1 (Resuelve java:S112)
            throw new ReglaNegocioException("Error interno del servidor al procesar la exportación del archivo Excel analítico", e);
        }
    }
}