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
            
            Sheet sheet = workbook.createSheet("Reporte de Ventas");
            
            // Inyección del nombre de la Sede
            Long sedeId = TenantContext.getCurrentSede();
            String nombreLocal = "Todas las Sedes (Consolidado)";
            
            if (sedeId != null) {
                Sede sede = sedeRepository.findById(sedeId)
                        .orElseThrow(() -> new ReglaNegocioException("Sede no encontrada para generación de reporte"));
                nombreLocal = "Sucursal: " + sede.getNombre();
            }

            // Fila 0: Título de la sede
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(nombreLocal);

            // Fila 1: Cabeceras
            Row headerRow = sheet.createRow(1);
            String[] columnas = {"Fecha", "Cantidad de Pedidos", "Total Ingresos (S/)"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
            }

            // Fila 2 en adelante: Datos
            int rowIdx = 2;
            for (VwDashboardVentas venta : datos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(venta.getFecha().toString());
                row.createCell(1).setCellValue(venta.getCantidadPedidos());
                row.createCell(2).setCellValue(venta.getTotalIngresos().doubleValue());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReglaNegocioException("Error interno del servidor al procesar la exportación del archivo Excel analítico", e);
        }
    }
}