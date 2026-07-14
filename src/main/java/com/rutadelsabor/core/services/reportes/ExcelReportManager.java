package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelReportManager {

    private final SedeRepository sedeRepository;

    public ExcelReportManager(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public byte[] generarReporteVentas(List<VwDashboardVentas> datos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Reporte Consolidado");

            // 1. ESTILOS PREMIUM
            // Estilo para el Título Principal
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilo para la Cabecera de la Tabla
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Estilo para Celdas de Dinero (Formato Moneda)
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("\"S/ \"#,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);
            currencyStyle.setBorderLeft(BorderStyle.THIN);
            currencyStyle.setBorderRight(BorderStyle.THIN);

            // Estilo normal con bordes
            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);
            normalStyle.setAlignment(HorizontalAlignment.CENTER);

            // 2. DATOS DE LA SEDE
            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "Todas las Sedes (Consolidado)";
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId)
                        .orElseThrow(() -> new ReglaNegocioException("Sede no encontrada"));
                nombreLocal = "Sucursal: " + sede.getNombre();
            }

            // 3. CONSTRUCCIÓN DEL EXCEL
            // Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VENTAS - " + nombreLocal.toUpperCase());
            titleCell.setCellStyle(titleStyle);
            
            // Fila vacía de separación
            sheet.createRow(1);

            // Cabeceras
            Row headerRow = sheet.createRow(2);
            String[] columnas = {"Fecha de Operación", "Tickets Emitidos", "Ingresos Totales"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowIdx = 3;
            double sumaIngresos = 0;
            int sumaPedidos = 0;

            for (VwDashboardVentas venta : datos) {
                Row row = sheet.createRow(rowIdx++);
                
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(venta.getFecha().toString());
                cell0.setCellStyle(normalStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(venta.getCantidadPedidos());
                cell1.setCellStyle(normalStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(venta.getTotalIngresos().doubleValue());
                cell2.setCellStyle(currencyStyle);

                sumaIngresos += venta.getTotalIngresos().doubleValue();
                sumaPedidos += venta.getCantidadPedidos();
            }

            // Fila de Totales
            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL GLOBAL:");
            totalLabel.setCellStyle(headerStyle);

            Cell totalPeds = totalRow.createCell(1);
            totalPeds.setCellValue(sumaPedidos);
            totalPeds.setCellStyle(headerStyle);

            Cell totalIngs = totalRow.createCell(2);
            totalIngs.setCellValue(sumaIngresos);
            totalIngs.setCellStyle(headerStyle); // Lo pintamos azul para que resalte

            // 4. AUTO-AJUSTE DE COLUMNAS
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("Error interno al generar el Excel", e);
        }
    }
}