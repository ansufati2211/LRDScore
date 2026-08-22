package com.rutadelsabor.core.services.reportes;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Sede;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.entities.VwDashboardVentas;
import com.rutadelsabor.core.repositories.SedeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelReportManager {

    private final SedeRepository sedeRepository;

    public ExcelReportManager(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public byte[] generarReporteVentas(List<VwDashboardVentas> datos) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Rendimiento de Ventas");

            XSSFColor slate800 = new XSSFColor(new byte[]{(byte) 30, (byte) 41, (byte) 59}, null);
            XSSFColor orange500 = new XSSFColor(new byte[]{(byte) 249, (byte) 115, (byte) 22}, null);
            XSSFColor slate50 = new XSSFColor(new byte[]{(byte) 248, (byte) 250, (byte) 252}, null);
            XSSFColor white = new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null);
            XSSFColor gray300 = new XSSFColor(new byte[]{(byte) 203, (byte) 213, (byte) 225}, null);

            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 22);
            titleFont.setColor(orange500);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleFont.setColor(slate800);
            subtitleStyle.setFont(subtitleFont);
            subtitleStyle.setAlignment(HorizontalAlignment.LEFT);

            XSSFCellStyle cardTitleStyle = workbook.createCellStyle();
            cardTitleStyle.setFillForegroundColor(slate800);
            cardTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cardTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            cardTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont cardTitleFont = workbook.createFont();
            cardTitleFont.setColor(white);
            cardTitleFont.setBold(true);
            cardTitleFont.setFontHeightInPoints((short) 10);
            cardTitleStyle.setFont(cardTitleFont);

            XSSFCellStyle cardValueStyle = workbook.createCellStyle();
            cardValueStyle.setFillForegroundColor(slate50);
            cardValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cardValueStyle.setAlignment(HorizontalAlignment.CENTER);
            cardValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cardValueStyle.setBorderBottom(BorderStyle.THIN);
            cardValueStyle.setBorderLeft(BorderStyle.THIN);
            cardValueStyle.setBorderRight(BorderStyle.THIN);
            cardValueStyle.setBottomBorderColor(gray300);
            cardValueStyle.setLeftBorderColor(gray300);
            cardValueStyle.setRightBorderColor(gray300);
            XSSFFont cardValueFont = workbook.createFont();
            cardValueFont.setBold(true);
            cardValueFont.setFontHeightInPoints((short) 16);
            cardValueStyle.setFont(cardValueFont);

            XSSFCellStyle cardMoneyStyle = workbook.createCellStyle();
            cardMoneyStyle.cloneStyleFrom(cardValueStyle);
            DataFormat format = workbook.createDataFormat();
            cardMoneyStyle.setDataFormat(format.getFormat("_-\"S/\" * #,##0.00_-"));

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(slate800);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFont(cardTitleFont);

            XSSFCellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBottomBorderColor(gray300);
            normalStyle.setAlignment(HorizontalAlignment.CENTER);
            normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(normalStyle);
            moneyStyle.setDataFormat(format.getFormat("_-\"S/\" * #,##0.00_-"));

            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "REPORTE CONSOLIDADO (TODAS LAS SEDES)";
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId)
                        .orElseThrow(() -> new ReglaNegocioException("Sede no encontrada"));
                nombreLocal = "SUCURSAL: " + sede.getNombre().toUpperCase();
            }

            double sumaIngresos = datos.stream().mapToDouble(v -> v.getTotalIngresos().doubleValue()).sum();
            long sumaPedidos = datos.stream().mapToLong(VwDashboardVentas::getCantidadPedidos).sum();

            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Rendimiento Financiero de Ventas");
            titleCell.setCellStyle(titleStyle);

            Row subtitleRow = sheet.createRow(2);
            Cell subtitleCell = subtitleRow.createCell(1);
            subtitleCell.setCellValue(nombreLocal + "  |  Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            subtitleCell.setCellStyle(subtitleStyle);

            Row cardHdrRow = sheet.createRow(4);
            cardHdrRow.setHeightInPoints(20);
            Cell c1 = cardHdrRow.createCell(1); c1.setCellValue("INGRESOS BRUTOS"); c1.setCellStyle(cardTitleStyle);
            Cell c2 = cardHdrRow.createCell(3); c2.setCellValue("TICKETS EMITIDOS"); c2.setCellStyle(cardTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 4));

            Row cardValRow = sheet.createRow(5);
            cardValRow.setHeightInPoints(35);
            Cell v1 = cardValRow.createCell(1); v1.setCellValue(sumaIngresos); v1.setCellStyle(cardMoneyStyle);
            Cell v2 = cardValRow.createCell(3); v2.setCellValue(sumaPedidos); v2.setCellStyle(cardValueStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 3, 4));

            Row headerRow = sheet.createRow(8);
            headerRow.setHeightInPoints(25);
            String[] columnas = {"Fecha de Operación", "Tickets Facturados", "Ingresos Totales"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 9;
            for (VwDashboardVentas venta : datos) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);
                
                Cell cell0 = row.createCell(1);
                cell0.setCellValue(venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                cell0.setCellStyle(normalStyle);

                Cell cell1 = row.createCell(2);
                cell1.setCellValue(venta.getCantidadPedidos());
                cell1.setCellStyle(normalStyle);

                Cell cell2 = row.createCell(3);
                cell2.setCellValue(venta.getTotalIngresos().doubleValue());
                cell2.setCellStyle(moneyStyle);
            }

            sheet.createFreezePane(0, 9);
            
            if (rowIdx > 9) {
                sheet.setAutoFilter(new CellRangeAddress(8, rowIdx - 1, 1, 3));
            }

            sheet.setColumnWidth(0, 1000);
            sheet.setColumnWidth(1, 6500);
            sheet.setColumnWidth(2, 6500);
            sheet.setColumnWidth(3, 7500);
            sheet.setColumnWidth(4, 3000);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("Error interno al generar el Excel", e);
        }
    }

    public byte[] generarReporteAuditoriaCajas(List<SesionCaja> datos) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Auditoría de Cajas");

            XSSFColor slate800 = new XSSFColor(new byte[]{(byte) 30, (byte) 41, (byte) 59}, null);
            XSSFColor orange500 = new XSSFColor(new byte[]{(byte) 249, (byte) 115, (byte) 22}, null);
            XSSFColor slate50 = new XSSFColor(new byte[]{(byte) 248, (byte) 250, (byte) 252}, null);
            XSSFColor white = new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null);
            XSSFColor gray300 = new XSSFColor(new byte[]{(byte) 203, (byte) 213, (byte) 225}, null);
            XSSFColor red600 = new XSSFColor(new byte[]{(byte) 220, (byte) 38, (byte) 38}, null);
            XSSFColor green600 = new XSSFColor(new byte[]{(byte) 22, (byte) 163, (byte) 74}, null);
            XSSFColor red50 = new XSSFColor(new byte[]{(byte) 254, (byte) 242, (byte) 242}, null);
            XSSFColor green50 = new XSSFColor(new byte[]{(byte) 240, (byte) 253, (byte) 244}, null);

            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 22);
            titleFont.setColor(orange500);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle subtitleStyle = workbook.createCellStyle();
            XSSFFont subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleFont.setColor(slate800);
            subtitleStyle.setFont(subtitleFont);
            subtitleStyle.setAlignment(HorizontalAlignment.LEFT);

            XSSFCellStyle cardTitleStyle = workbook.createCellStyle();
            cardTitleStyle.setFillForegroundColor(slate800);
            cardTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cardTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            cardTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont cardTitleFont = workbook.createFont();
            cardTitleFont.setColor(white);
            cardTitleFont.setBold(true);
            cardTitleFont.setFontHeightInPoints((short) 10);
            cardTitleStyle.setFont(cardTitleFont);

            DataFormat format = workbook.createDataFormat();

            XSSFCellStyle cardValueStyle = workbook.createCellStyle();
            cardValueStyle.setFillForegroundColor(slate50);
            cardValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cardValueStyle.setAlignment(HorizontalAlignment.CENTER);
            cardValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cardValueStyle.setBorderBottom(BorderStyle.THIN);
            cardValueStyle.setBorderLeft(BorderStyle.THIN);
            cardValueStyle.setBorderRight(BorderStyle.THIN);
            cardValueStyle.setBottomBorderColor(gray300);
            cardValueStyle.setLeftBorderColor(gray300);
            cardValueStyle.setRightBorderColor(gray300);
            cardValueStyle.setDataFormat(format.getFormat("_-\"S/\" * #,##0.00_-"));
            XSSFFont cardValueFont = workbook.createFont();
            cardValueFont.setBold(true);
            cardValueFont.setFontHeightInPoints((short) 16);
            cardValueStyle.setFont(cardValueFont);

            XSSFCellStyle cardBadStyle = workbook.createCellStyle();
            cardBadStyle.cloneStyleFrom(cardValueStyle);
            cardBadStyle.setFillForegroundColor(red50);
            XSSFFont cardBadFont = workbook.createFont();
            cardBadFont.setBold(true);
            cardBadFont.setFontHeightInPoints((short) 16);
            cardBadFont.setColor(red600);
            cardBadStyle.setFont(cardBadFont);

            XSSFCellStyle cardGoodStyle = workbook.createCellStyle();
            cardGoodStyle.cloneStyleFrom(cardValueStyle);
            cardGoodStyle.setFillForegroundColor(green50);
            XSSFFont cardGoodFont = workbook.createFont();
            cardGoodFont.setBold(true);
            cardGoodFont.setFontHeightInPoints((short) 16);
            cardGoodFont.setColor(green600);
            cardGoodStyle.setFont(cardGoodFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(slate800);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFont(cardTitleFont);

            XSSFCellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBottomBorderColor(gray300);
            normalStyle.setAlignment(HorizontalAlignment.CENTER);
            normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(normalStyle);
            moneyStyle.setDataFormat(format.getFormat("_-\"S/\" * #,##0.00_-"));

            XSSFCellStyle badMoneyStyle = workbook.createCellStyle();
            badMoneyStyle.cloneStyleFrom(moneyStyle);
            badMoneyStyle.setFillForegroundColor(red50);
            badMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont smallBadFont = workbook.createFont();
            smallBadFont.setColor(red600);
            smallBadFont.setBold(true);
            badMoneyStyle.setFont(smallBadFont);

            XSSFCellStyle goodMoneyStyle = workbook.createCellStyle();
            goodMoneyStyle.cloneStyleFrom(moneyStyle);
            goodMoneyStyle.setFillForegroundColor(green50);
            goodMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont smallGoodFont = workbook.createFont();
            smallGoodFont.setColor(green600);
            smallGoodFont.setBold(true);
            goodMoneyStyle.setFont(smallGoodFont);

            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "TODAS LAS SEDES";
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId).orElseThrow(() -> new ReglaNegocioException("Sede no encontrada"));
                nombreLocal = "SUCURSAL: " + sede.getNombre().toUpperCase();
            }

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

            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("Auditoría General de Cajas");
            titleCell.setCellStyle(titleStyle);

            Row subtitleRow = sheet.createRow(2);
            Cell subtitleCell = subtitleRow.createCell(1);
            subtitleCell.setCellValue(nombreLocal + "  |  Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            subtitleCell.setCellStyle(subtitleStyle);

            Row cardHdrRow = sheet.createRow(4);
            cardHdrRow.setHeightInPoints(20);
            Cell c1 = cardHdrRow.createCell(1); c1.setCellValue("INGRESOS DECLARADOS"); c1.setCellStyle(cardTitleStyle);
            Cell c2 = cardHdrRow.createCell(4); c2.setCellValue("DESCUADRE GLOBAL"); c2.setCellStyle(cardTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 4, 6));

            Row cardValRow = sheet.createRow(5);
            cardValRow.setHeightInPoints(35);
            Cell v1 = cardValRow.createCell(1); v1.setCellValue(totalDeclarado); v1.setCellStyle(cardValueStyle);
            Cell v2 = cardValRow.createCell(4); v2.setCellValue(totalDescuadre); 
            v2.setCellStyle(totalDescuadre < 0 ? cardBadStyle : (totalDescuadre > 0 ? cardGoodStyle : cardValueStyle));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 4, 6));

            Row headerRow = sheet.createRow(8);
            headerRow.setHeightInPoints(25);
            String[] columnas = {"Transacción ID", "Estado", "Cajero Encargado", "Apertura", "Cierre", "Fondo Inicial", "Monto Declarado", "Descuadre Final"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 9;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (SesionCaja sesion : datos) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);
                
                Cell cell0 = row.createCell(1);
                cell0.setCellValue("CAJ-" + sesion.getId());
                cell0.setCellStyle(normalStyle);

                Cell cell1 = row.createCell(2);
                cell1.setCellValue(sesion.getEstado().name());
                cell1.setCellStyle(normalStyle);

                Cell cell2 = row.createCell(3);
                cell2.setCellValue(sesion.getCajero().getNombre());
                cell2.setCellStyle(normalStyle);

                Cell cell3 = row.createCell(4);
                cell3.setCellValue(sesion.getFechaApertura() != null ? sesion.getFechaApertura().format(dtf) : "-");
                cell3.setCellStyle(normalStyle);

                Cell cell4 = row.createCell(5);
                cell4.setCellValue(sesion.getFechaCierre() != null ? sesion.getFechaCierre().format(dtf) : "ACTIVA");
                cell4.setCellStyle(normalStyle);

                Cell cell5 = row.createCell(6);
                cell5.setCellValue(sesion.getMontoInicial().doubleValue());
                cell5.setCellStyle(moneyStyle);

                double declarado = sesion.getMontoFinalDeclarado() != null ? sesion.getMontoFinalDeclarado().doubleValue() : 0.0;
                Cell cell6 = row.createCell(7);
                cell6.setCellValue(declarado);
                cell6.setCellStyle(moneyStyle);

                double esperado = sesion.getMontoFinalCalculado() != null ? sesion.getMontoFinalCalculado().doubleValue() : 0.0;
                double diferencia = declarado - esperado;
                
                Cell cell7 = row.createCell(8);
                cell7.setCellValue(diferencia);
                cell7.setCellStyle(diferencia < 0 ? badMoneyStyle : (diferencia > 0 ? goodMoneyStyle : moneyStyle));
            }

            sheet.createFreezePane(0, 9);
            
            if (rowIdx > 9) {
                sheet.setAutoFilter(new CellRangeAddress(8, rowIdx - 1, 1, 8));
            }

            sheet.setColumnWidth(0, 1000);
            sheet.setColumnWidth(1, 4500);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 7000);
            sheet.setColumnWidth(4, 5500);
            sheet.setColumnWidth(5, 5500);
            sheet.setColumnWidth(6, 4500);
            sheet.setColumnWidth(7, 5000);
            sheet.setColumnWidth(8, 5000);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("Error interno al generar el Excel de Auditoría", e);
        }
    }
}