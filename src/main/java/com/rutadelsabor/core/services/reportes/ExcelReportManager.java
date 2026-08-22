package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
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

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.ORANGE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
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

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("\"S/ \"#,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);
            currencyStyle.setBorderLeft(BorderStyle.THIN);
            currencyStyle.setBorderRight(BorderStyle.THIN);

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);
            normalStyle.setAlignment(HorizontalAlignment.CENTER);

            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "Todas las Sedes (Consolidado)";
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId)
                        .orElseThrow(() -> new ReglaNegocioException("Sede no encontrada"));
                nombreLocal = "Sucursal: " + sede.getNombre();
            }

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VENTAS - " + nombreLocal.toUpperCase());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            sheet.createRow(1);

            Row headerRow = sheet.createRow(2);
            String[] columnas = {"Fecha de Operacion", "Tickets Emitidos", "Ingresos Totales"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

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

            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL GLOBAL:");
            totalLabel.setCellStyle(headerStyle);

            Cell totalPeds = totalRow.createCell(1);
            totalPeds.setCellValue(sumaPedidos);
            totalPeds.setCellStyle(headerStyle);

            Cell totalIngs = totalRow.createCell(2);
            totalIngs.setCellValue(sumaIngresos);
            totalIngs.setCellStyle(headerStyle); 

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("Error interno al generar el Excel", e);
        }
    }

    public byte[] generarReporteAuditoriaCajas(List<SesionCaja> datos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Auditoria de Cajas");
            
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);
            titleFont.setColor(IndexedColors.ORANGE.getIndex()); 
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle cardTitleStyle = workbook.createCellStyle();
            cardTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cardTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cardTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            Font cardTitleFont = workbook.createFont();
            cardTitleFont.setBold(true);
            cardTitleStyle.setFont(cardTitleFont);

            CellStyle cardValueStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            cardValueStyle.setDataFormat(format.getFormat("\"S/ \"#,##0.00"));
            cardValueStyle.setAlignment(HorizontalAlignment.CENTER);
            Font cardValueFont = workbook.createFont();
            cardValueFont.setBold(true);
            cardValueFont.setFontHeightInPoints((short) 14);
            cardValueStyle.setFont(cardValueFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);
            normalStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(format.getFormat("\"S/ \"#,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);
            currencyStyle.setBorderLeft(BorderStyle.THIN);
            currencyStyle.setBorderRight(BorderStyle.THIN);

            CellStyle badCurrencyStyle = workbook.createCellStyle();
            badCurrencyStyle.cloneStyleFrom(currencyStyle);
            Font badFont = workbook.createFont();
            badFont.setColor(IndexedColors.RED.getIndex());
            badCurrencyStyle.setFont(badFont);

            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "Todas las Sedes";
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId).orElseThrow(() -> new ReglaNegocioException("Sede no encontrada"));
                nombreLocal = "Sucursal: " + sede.getNombre();
            }

            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("REPORTE DE AUDITORIA DE CAJAS - " + nombreLocal.toUpperCase());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 7));

            double totalDeclarado = 0;
            double totalDescuadre = 0;
            for (SesionCaja s : datos) {
                if(s.getEstado().name().equals("CERRADA")) {
                    double dec = s.getMontoFinalDeclarado() != null ? s.getMontoFinalDeclarado().doubleValue() : 0.0;
                    double esp = s.getMontoFinalCalculado() != null ? s.getMontoFinalCalculado().doubleValue() : 0.0;
                    totalDeclarado += dec;
                    totalDescuadre += (dec - esp);
                }
            }

            Row summaryTitleRow = sheet.createRow(3);
            Cell c1 = summaryTitleRow.createCell(2); c1.setCellValue("INGRESOS DECLARADOS"); c1.setCellStyle(cardTitleStyle);
            Cell c2 = summaryTitleRow.createCell(5); c2.setCellValue("DESCUADRE ACUMULADO"); c2.setCellStyle(cardTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 2, 3));
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 5, 6));

            Row summaryValueRow = sheet.createRow(4);
            Cell v1 = summaryValueRow.createCell(2); v1.setCellValue(totalDeclarado); v1.setCellStyle(cardValueStyle);
            Cell v2 = summaryValueRow.createCell(5); v2.setCellValue(totalDescuadre); v2.setCellStyle(totalDescuadre < 0 ? badCurrencyStyle : cardValueStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 2, 3));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 6));

            Row headerRow = sheet.createRow(7);
            String[] columnas = {"ID", "Estado", "Cajero(a)", "Fecha Apertura", "Fecha Cierre", "Fondo Inicial", "Monto Declarado", "Diferencia"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 8;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (SesionCaja sesion : datos) {
                Row row = sheet.createRow(rowIdx++);
                
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(sesion.getId());
                cell0.setCellStyle(normalStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(sesion.getEstado().name());
                cell1.setCellStyle(normalStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(sesion.getCajero().getNombre());
                cell2.setCellStyle(normalStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(sesion.getFechaApertura() != null ? sesion.getFechaApertura().format(dtf) : "-");
                cell3.setCellStyle(normalStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(sesion.getFechaCierre() != null ? sesion.getFechaCierre().format(dtf) : "EN CURSO");
                cell4.setCellStyle(normalStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(sesion.getMontoInicial().doubleValue());
                cell5.setCellStyle(currencyStyle);

                double declarado = sesion.getMontoFinalDeclarado() != null ? sesion.getMontoFinalDeclarado().doubleValue() : 0.0;
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(declarado);
                cell6.setCellStyle(currencyStyle);

                double esperado = sesion.getMontoFinalCalculado() != null ? sesion.getMontoFinalCalculado().doubleValue() : 0.0;
                double diferencia = declarado - esperado;
                
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(diferencia);
                cell7.setCellStyle(diferencia < 0 ? badCurrencyStyle : currencyStyle);
            }

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