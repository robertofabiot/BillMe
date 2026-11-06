package com.example.billme.exportacion;

import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class DocumentoExcel {

    public byte[] generar(Documento documento, String encabezado) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String rawFolio = documento.getFolio() != null && !documento.getFolio().isBlank()
                    ? documento.getFolio()
                    : "Documento";
            String sheetName = WorkbookUtil.createSafeSheetName(rawFolio);
            Sheet sheet = workbook.createSheet(sheetName);

            boolean mostrarEntrega = documento.isMostrarEntrega();
            int lastCol = mostrarEntrega ? 10 : 8;

            // Anchos de columna
            sheet.setColumnWidth(0, 2 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 15 * 256);
            sheet.setColumnWidth(5, 12 * 256);
            sheet.setColumnWidth(6, 12 * 256);
            sheet.setColumnWidth(7, 16 * 256);
            sheet.setColumnWidth(8, 16 * 256);
            if (mostrarEntrega) {
                sheet.setColumnWidth(9, 14 * 256);
                sheet.setColumnWidth(10, 24 * 256);
            }

            // Formatos y fuentes
            DataFormat df = workbook.createDataFormat();
            short fmtFecha = df.getFormat("dd/mm/yyyy");
            short fmtCantidad = df.getFormat("0.###");
            short fmtPeso = df.getFormat("#,##0.00");
            short fmtMoneda = df.getFormat("\"C$\"#,##0.00");
            short fmtUsd = df.getFormat("\"U$\"#,##0.00");

            Font fontBold = workbook.createFont();
            fontBold.setBold(true);

            Font fontTitulo = workbook.createFont();
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 14);

            Font fontFinLinea = workbook.createFont();
            fontFinLinea.setBold(true);
            fontFinLinea.setItalic(true);

            // Estilos
            CellStyle styleFecha = workbook.createCellStyle();
            styleFecha.setFont(fontBold);
            styleFecha.setDataFormat(fmtFecha);
            styleFecha.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleEncabezado = workbook.createCellStyle();
            styleEncabezado.setFont(fontTitulo);

            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFont(fontBold);
            aplicarBordes(styleHeader);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);

            CellStyle styleCantidad = workbook.createCellStyle();
            aplicarBordes(styleCantidad);
            styleCantidad.setDataFormat(fmtCantidad);
            styleCantidad.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleDescripcion = workbook.createCellStyle();
            aplicarBordes(styleDescripcion);
            styleDescripcion.setAlignment(HorizontalAlignment.LEFT);

            CellStyle stylePeso = workbook.createCellStyle();
            aplicarBordes(stylePeso);
            stylePeso.setDataFormat(fmtPeso);
            stylePeso.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle stylePrecio = workbook.createCellStyle();
            aplicarBordes(stylePrecio);
            stylePrecio.setDataFormat(fmtMoneda);
            stylePrecio.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleFechaEntrega = workbook.createCellStyle();
            aplicarBordes(styleFechaEntrega);
            styleFechaEntrega.setDataFormat(fmtFecha);
            styleFechaEntrega.setAlignment(HorizontalAlignment.CENTER);

            CellStyle styleLugarEntrega = workbook.createCellStyle();
            aplicarBordes(styleLugarEntrega);
            styleLugarEntrega.setAlignment(HorizontalAlignment.LEFT);

            CellStyle styleVacio = workbook.createCellStyle();
            aplicarBordes(styleVacio);

            CellStyle styleFinLinea = workbook.createCellStyle();
            styleFinLinea.setFont(fontFinLinea);
            aplicarBordes(styleFinLinea);
            styleFinLinea.setAlignment(HorizontalAlignment.CENTER);

            CellStyle styleTotalEtiqueta = workbook.createCellStyle();
            styleTotalEtiqueta.setFont(fontBold);
            aplicarBordes(styleTotalEtiqueta);
            styleTotalEtiqueta.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleTotalPrecio = workbook.createCellStyle();
            styleTotalPrecio.setFont(fontBold);
            aplicarBordes(styleTotalPrecio);
            styleTotalPrecio.setDataFormat(fmtMoneda);
            styleTotalPrecio.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleTotalUsd = workbook.createCellStyle();
            styleTotalUsd.setFont(fontBold);
            aplicarBordes(styleTotalUsd);
            styleTotalUsd.setDataFormat(fmtUsd);
            styleTotalUsd.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleTotalPeso = workbook.createCellStyle();
            styleTotalPeso.setFont(fontBold);
            aplicarBordes(styleTotalPeso);
            styleTotalPeso.setDataFormat(fmtPeso);
            styleTotalPeso.setAlignment(HorizontalAlignment.RIGHT);

            // Fila 1 (rowIdx 0): H1:I1 Fecha
            Row r0 = sheet.createRow(0);
            Cell cellH1 = r0.createCell(7);
            if (documento.getFecha() != null) {
                cellH1.setCellValue(documento.getFecha());
            }
            cellH1.setCellStyle(styleFecha);
            r0.createCell(8).setCellStyle(styleFecha);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 7, 8));

            // Fila 2 (rowIdx 1): B2:{lastCol}2 Encabezado
            Row r1 = sheet.createRow(1);
            Cell cellB2 = r1.createCell(1);
            cellB2.setCellValue(encabezado != null ? encabezado : "");
            cellB2.setCellStyle(styleEncabezado);
            for (int c = 2; c <= lastCol; c++) {
                r1.createCell(c).setCellStyle(styleEncabezado);
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, lastCol));

            // Fila 3 (rowIdx 2): Vacía
            sheet.createRow(2);

            // Fila 4 (rowIdx 3): Encabezados de tabla
            Row r3 = sheet.createRow(3);
            Cell cB = r3.createCell(1);
            cB.setCellValue("Cantidad");
            cB.setCellStyle(styleHeader);

            for (int c = 2; c <= 4; c++) {
                r3.createCell(c).setCellStyle(styleHeader);
            }
            r3.getCell(2).setCellValue("Descripción");
            CellRangeAddress regionDescHeader = new CellRangeAddress(3, 3, 2, 4);
            sheet.addMergedRegion(regionDescHeader);
            aplicarBordesRegion(regionDescHeader, sheet);

            Cell cF = r3.createCell(5);
            cF.setCellValue("Peso Unit (KG)");
            cF.setCellStyle(styleHeader);

            Cell cG = r3.createCell(6);
            cG.setCellValue("Peso Total (KG)");
            cG.setCellStyle(styleHeader);

            Cell cH = r3.createCell(7);
            cH.setCellValue("Precio Unitario C$");
            cH.setCellStyle(styleHeader);

            Cell cI = r3.createCell(8);
            cI.setCellValue("Precio Total C$");
            cI.setCellStyle(styleHeader);

            if (mostrarEntrega) {
                Cell cJ = r3.createCell(9);
                cJ.setCellValue("Fecha entrega");
                cJ.setCellStyle(styleHeader);

                Cell cK = r3.createCell(10);
                cK.setCellValue("Lugar entrega");
                cK.setCellStyle(styleHeader);
            }

            // Filas de datos (rowIdx 4 .. 4 + lineas.size() - 1)
            List<DocumentoLinea> lineas = documento.getLineas() != null ? documento.getLineas() : List.of();
            int numLineas = lineas.size();
            int startRowIdx = 4;

            for (int i = 0; i < numLineas; i++) {
                DocumentoLinea linea = lineas.get(i);
                int rowIdx = startRowIdx + i;
                int r = rowIdx + 1; // 1-based para fórmulas de Excel
                Row row = sheet.createRow(rowIdx);

                // Col B: Cantidad
                Cell cellB = row.createCell(1);
                if (linea.getCantidad() != null) {
                    cellB.setCellValue(linea.getCantidad().doubleValue());
                }
                cellB.setCellStyle(styleCantidad);

                // Col C:E: Descripción
                for (int c = 2; c <= 4; c++) {
                    row.createCell(c).setCellStyle(styleDescripcion);
                }
                row.getCell(2).setCellValue(linea.getDescripcion() != null ? linea.getDescripcion() : "");
                CellRangeAddress regionDesc = new CellRangeAddress(rowIdx, rowIdx, 2, 4);
                sheet.addMergedRegion(regionDesc);
                aplicarBordesRegion(regionDesc, sheet);

                // Col F: Peso Unit (KG)
                Cell cellF = row.createCell(5);
                if (linea.getPesoUnitKg() != null) {
                    cellF.setCellValue(linea.getPesoUnitKg().doubleValue());
                    cellF.setCellStyle(stylePeso);
                } else {
                    cellF.setCellStyle(styleVacio);
                }

                // Col G: Peso Total (KG)
                Cell cellG = row.createCell(6);
                if (linea.getPesoUnitKg() != null) {
                    cellG.setCellFormula("ROUND(B" + r + "*F" + r + ",2)");
                    cellG.setCellStyle(stylePeso);
                } else {
                    cellG.setCellStyle(styleVacio);
                }

                // Col H: Precio Unitario C$
                Cell cellH = row.createCell(7);
                if (linea.getPrecioUnit() != null) {
                    cellH.setCellValue(linea.getPrecioUnit().doubleValue());
                }
                cellH.setCellStyle(stylePrecio);

                // Col I: Precio Total C$
                Cell cellI = row.createCell(8);
                cellI.setCellFormula("ROUND(B" + r + "*H" + r + ",2)");
                cellI.setCellStyle(stylePrecio);

                // Columnas de entrega si aplica
                if (mostrarEntrega) {
                    Cell cellJ = row.createCell(9);
                    if (linea.getFechaEntrega() != null) {
                        cellJ.setCellValue(linea.getFechaEntrega());
                        cellJ.setCellStyle(styleFechaEntrega);
                    } else {
                        cellJ.setCellStyle(styleVacio);
                    }

                    Cell cellK = row.createCell(10);
                    if (linea.getLugarEntrega() != null && !linea.getLugarEntrega().isBlank()) {
                        cellK.setCellValue(linea.getLugarEntrega());
                        cellK.setCellStyle(styleLugarEntrega);
                    } else {
                        cellK.setCellStyle(styleVacio);
                    }
                }
            }

            int lastDataRow = 4 + numLineas; // 1-based (ej. si numLineas=2, lastDataRow=6)

            // Fila n+1: Separador FIN DE LÍNEA
            int finLineaRowIdx = 4 + numLineas;
            Row rFin = sheet.createRow(finLineaRowIdx);
            for (int c = 1; c <= lastCol; c++) {
                rFin.createCell(c).setCellStyle(styleFinLinea);
            }
            rFin.getCell(1).setCellValue("*****FIN DE LÍNEA*****");
            CellRangeAddress regionFin = new CellRangeAddress(finLineaRowIdx, finLineaRowIdx, 1, lastCol);
            sheet.addMergedRegion(regionFin);
            aplicarBordesRegion(regionFin, sheet);

            // Fila n+2: Vacía
            int blankRowIdx = finLineaRowIdx + 1;
            sheet.createRow(blankRowIdx);

            // Fila n+3: Total C$
            int totalCRowIdx = blankRowIdx + 1;
            Row rTotalC = sheet.createRow(totalCRowIdx);
            Cell cTotalCLabel = rTotalC.createCell(7);
            cTotalCLabel.setCellValue("Total C$");
            cTotalCLabel.setCellStyle(styleTotalEtiqueta);

            Cell cTotalCValue = rTotalC.createCell(8);
            String formulaTotalC = numLineas > 0 ? "SUM(I5:I" + lastDataRow + ")" : "0";
            cTotalCValue.setCellFormula(formulaTotalC);
            cTotalCValue.setCellStyle(styleTotalPrecio);

            int siguienteFilaIdx = totalCRowIdx + 1;

            if (documento.isMostrarTotalUsd()) {
                Row rTotalUsd = sheet.createRow(siguienteFilaIdx);
                Cell cTotalUsdLabel = rTotalUsd.createCell(7);
                cTotalUsdLabel.setCellValue("Total U$");
                cTotalUsdLabel.setCellStyle(styleTotalEtiqueta);

                Cell cTotalUsdValue = rTotalUsd.createCell(8);
                int filaTotalC = totalCRowIdx + 1;
                String tc = documento.getTipoCambio() != null ? documento.getTipoCambio().toPlainString() : "1";
                cTotalUsdValue.setCellFormula("ROUND(I" + filaTotalC + "/" + tc + ",2)");
                cTotalUsdValue.setCellStyle(styleTotalUsd);

                siguienteFilaIdx++;
            }

            // Total KG
            Row rTotalKg = sheet.createRow(siguienteFilaIdx);
            Cell cTotalKgLabel = rTotalKg.createCell(7);
            cTotalKgLabel.setCellValue("Total KG");
            cTotalKgLabel.setCellStyle(styleTotalEtiqueta);

            Cell cTotalKgValue = rTotalKg.createCell(8);
            String formulaTotalKg = numLineas > 0 ? "SUM(G5:G" + lastDataRow + ")" : "0";
            cTotalKgValue.setCellFormula(formulaTotalKg);
            cTotalKgValue.setCellStyle(styleTotalPeso);

            // Pre-evaluación de fórmulas
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar archivo Excel de documento", e);
        }
    }

    private void aplicarBordes(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void aplicarBordesRegion(CellRangeAddress region, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }
}
