package com.example.billme.exportacion;

import com.example.billme.dto.ConsolidadoResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConsolidadoExcel {

    public byte[] generar(ConsolidadoResponse consolidado, String encabezado) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String rawFolio = consolidado.folio() != null && !consolidado.folio().isBlank()
                    ? consolidado.folio()
                    : "Consolidado";
            String sheetName = WorkbookUtil.createSafeSheetName(rawFolio);
            Sheet sheet = workbook.createSheet(sheetName);

            boolean mostrarLugar = consolidado.grupos() != null && consolidado.grupos().stream()
                    .flatMap(g -> g.lineas().stream())
                    .anyMatch(l -> l.lugarEntrega() != null && !l.lugarEntrega().isBlank());

            int lastCol = mostrarLugar ? 9 : 8;

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
            if (mostrarLugar) {
                sheet.setColumnWidth(9, 24 * 256);
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

            CellStyle styleLugarEntrega = workbook.createCellStyle();
            aplicarBordes(styleLugarEntrega);
            styleLugarEntrega.setAlignment(HorizontalAlignment.LEFT);

            CellStyle styleVacio = workbook.createCellStyle();
            aplicarBordes(styleVacio);

            CellStyle styleSubtotalNombre = workbook.createCellStyle();
            styleSubtotalNombre.setFont(fontBold);
            aplicarBordes(styleSubtotalNombre);
            styleSubtotalNombre.setAlignment(HorizontalAlignment.LEFT);

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

            CellStyle styleTotalPeso = workbook.createCellStyle();
            styleTotalPeso.setFont(fontBold);
            aplicarBordes(styleTotalPeso);
            styleTotalPeso.setDataFormat(fmtPeso);
            styleTotalPeso.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle styleTotalUsd = workbook.createCellStyle();
            styleTotalUsd.setFont(fontBold);
            aplicarBordes(styleTotalUsd);
            styleTotalUsd.setDataFormat(fmtUsd);
            styleTotalUsd.setAlignment(HorizontalAlignment.RIGHT);

            // Fila 1 (rowIdx 0): H1:I1 Fecha
            Row r0 = sheet.createRow(0);
            Cell cellH1 = r0.createCell(7);
            if (consolidado.hasta() != null) {
                cellH1.setCellValue(consolidado.hasta());
            }
            cellH1.setCellStyle(styleFecha);
            r0.createCell(8).setCellStyle(styleFecha);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 7, 8));

            // Fila 2 (rowIdx 1): B2:{lastCol}2 Encabezado
            String encabezadoFinal = (encabezado != null && !encabezado.isBlank())
                    ? encabezado.trim()
                    : (consolidado.proyectoNombre() != null && !consolidado.proyectoNombre().isBlank()
                    ? consolidado.clienteNombre() + " / " + consolidado.proyectoNombre()
                    : consolidado.clienteNombre());

            Row r1 = sheet.createRow(1);
            Cell cellB2 = r1.createCell(1);
            cellB2.setCellValue(encabezadoFinal);
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

            if (mostrarLugar) {
                Cell cJ = r3.createCell(9);
                cJ.setCellValue("Lugar entrega");
                cJ.setCellStyle(styleHeader);
            }

            // Datos por grupo
            List<ConsolidadoResponse.Grupo> grupos = consolidado.grupos() != null ? consolidado.grupos() : List.of();
            List<Integer> subtotalRowIndices = new ArrayList<>();
            int currentRowIdx = 4;

            for (ConsolidadoResponse.Grupo g : grupos) {
                List<ConsolidadoResponse.Linea> lineas = g.lineas() != null ? g.lineas() : List.of();
                int groupStartRow = currentRowIdx + 1; // 1-based

                for (ConsolidadoResponse.Linea linea : lineas) {
                    Row row = sheet.createRow(currentRowIdx);
                    int r = currentRowIdx + 1;

                    // Col B: Cantidad
                    Cell cellB = row.createCell(1);
                    if (linea.cantidad() != null) {
                        cellB.setCellValue(linea.cantidad().doubleValue());
                    }
                    cellB.setCellStyle(styleCantidad);

                    // Col C:E: Descripción
                    for (int c = 2; c <= 4; c++) {
                        row.createCell(c).setCellStyle(styleDescripcion);
                    }
                    row.getCell(2).setCellValue(linea.descripcion() != null ? linea.descripcion() : "");
                    CellRangeAddress regionDesc = new CellRangeAddress(currentRowIdx, currentRowIdx, 2, 4);
                    sheet.addMergedRegion(regionDesc);
                    aplicarBordesRegion(regionDesc, sheet);

                    // Col F: Peso Unit (KG)
                    Cell cellF = row.createCell(5);
                    if (linea.pesoUnitKg() != null) {
                        cellF.setCellValue(linea.pesoUnitKg().doubleValue());
                        cellF.setCellStyle(stylePeso);
                    } else {
                        cellF.setCellStyle(styleVacio);
                    }

                    // Col G: Peso Total (KG)
                    Cell cellG = row.createCell(6);
                    if (linea.pesoUnitKg() != null) {
                        cellG.setCellFormula("ROUND(B" + r + "*F" + r + ",2)");
                        cellG.setCellStyle(stylePeso);
                    } else {
                        cellG.setCellStyle(styleVacio);
                    }

                    // Col H: Precio Unitario C$
                    Cell cellH = row.createCell(7);
                    if (linea.precioUnit() != null) {
                        cellH.setCellValue(linea.precioUnit().doubleValue());
                    }
                    cellH.setCellStyle(stylePrecio);

                    // Col I: Precio Total C$
                    Cell cellI = row.createCell(8);
                    cellI.setCellFormula("ROUND(B" + r + "*H" + r + ",2)");
                    cellI.setCellStyle(stylePrecio);

                    if (mostrarLugar) {
                        Cell cellJ = row.createCell(9);
                        if (linea.lugarEntrega() != null && !linea.lugarEntrega().isBlank()) {
                            cellJ.setCellValue(linea.lugarEntrega());
                            cellJ.setCellStyle(styleLugarEntrega);
                        } else {
                            cellJ.setCellStyle(styleVacio);
                        }
                    }

                    currentRowIdx++;
                }

                int groupEndRow = currentRowIdx; // 1-based

                // Fila Subtotal de grupo
                Row rowSub = sheet.createRow(currentRowIdx);
                int rSub = currentRowIdx + 1;
                subtotalRowIndices.add(rSub);

                // Col B
                rowSub.createCell(1).setCellStyle(styleVacio);

                // Col C:E Nombre del subtotal
                for (int c = 2; c <= 4; c++) {
                    rowSub.createCell(c).setCellStyle(styleSubtotalNombre);
                }
                rowSub.getCell(2).setCellValue(g.nombre() != null ? g.nombre() : "");
                CellRangeAddress regionSubNombre = new CellRangeAddress(currentRowIdx, currentRowIdx, 2, 4);
                sheet.addMergedRegion(regionSubNombre);
                aplicarBordesRegion(regionSubNombre, sheet);

                // Col F
                rowSub.createCell(5).setCellStyle(styleVacio);

                // Col G: SUM del peso
                Cell cellSubG = rowSub.createCell(6);
                if (groupStartRow <= groupEndRow) {
                    cellSubG.setCellFormula("SUM(G" + groupStartRow + ":G" + groupEndRow + ")");
                } else {
                    cellSubG.setCellFormula("0");
                }
                cellSubG.setCellStyle(styleTotalPeso);

                // Col H
                rowSub.createCell(7).setCellStyle(styleVacio);

                // Col I: SUM del precio
                Cell cellSubI = rowSub.createCell(8);
                if (groupStartRow <= groupEndRow) {
                    cellSubI.setCellFormula("SUM(I" + groupStartRow + ":I" + groupEndRow + ")");
                } else {
                    cellSubI.setCellFormula("0");
                }
                cellSubI.setCellStyle(styleTotalPrecio);

                if (mostrarLugar) {
                    rowSub.createCell(9).setCellStyle(styleVacio);
                }

                currentRowIdx++;
            }

            // Fila: Separador FIN DE LÍNEA
            int finLineaRowIdx = currentRowIdx++;
            Row rFin = sheet.createRow(finLineaRowIdx);
            for (int c = 1; c <= lastCol; c++) {
                rFin.createCell(c).setCellStyle(styleFinLinea);
            }
            rFin.getCell(1).setCellValue("*****FIN DE LÍNEA*****");
            CellRangeAddress regionFin = new CellRangeAddress(finLineaRowIdx, finLineaRowIdx, 1, lastCol);
            sheet.addMergedRegion(regionFin);
            aplicarBordesRegion(regionFin, sheet);

            // Fila Vacía
            sheet.createRow(currentRowIdx++);

            // Fila Total C$
            int totalCRowIdx = currentRowIdx++;
            int rTotC = totalCRowIdx + 1; // 1-based
            Row rTotalC = sheet.createRow(totalCRowIdx);
            Cell cTotalCLabel = rTotalC.createCell(7);
            cTotalCLabel.setCellValue("Total C$");
            cTotalCLabel.setCellStyle(styleTotalEtiqueta);

            Cell cTotalCValue = rTotalC.createCell(8);
            String formulaTotalC = subtotalRowIndices.isEmpty()
                    ? "0"
                    : "SUM(" + String.join(",", subtotalRowIndices.stream().map(r -> "I" + r).toList()) + ")";
            cTotalCValue.setCellFormula(formulaTotalC);
            cTotalCValue.setCellStyle(styleTotalPrecio);

            if (Boolean.TRUE.equals(consolidado.mostrarTotalUsd())) {
                int totalUsdRowIdx = currentRowIdx++;
                Row rTotalUsd = sheet.createRow(totalUsdRowIdx);
                Cell cTotalUsdLabel = rTotalUsd.createCell(7);
                cTotalUsdLabel.setCellValue("Total U$");
                cTotalUsdLabel.setCellStyle(styleTotalEtiqueta);

                String tc = consolidado.tipoCambio() != null ? consolidado.tipoCambio().toPlainString() : "1";
                Cell cTotalUsdValue = rTotalUsd.createCell(8);
                cTotalUsdValue.setCellFormula("ROUND(I" + rTotC + "/" + tc + ",2)");
                cTotalUsdValue.setCellStyle(styleTotalUsd);
            }

            // Fila Total KG
            int totalKgRowIdx = currentRowIdx++;
            Row rTotalKg = sheet.createRow(totalKgRowIdx);
            Cell cTotalKgLabel = rTotalKg.createCell(7);
            cTotalKgLabel.setCellValue("Total KG");
            cTotalKgLabel.setCellStyle(styleTotalEtiqueta);

            Cell cTotalKgValue = rTotalKg.createCell(8);
            String formulaTotalKg = subtotalRowIndices.isEmpty()
                    ? "0"
                    : "SUM(" + String.join(",", subtotalRowIndices.stream().map(r -> "G" + r).toList()) + ")";
            cTotalKgValue.setCellFormula(formulaTotalKg);
            cTotalKgValue.setCellStyle(styleTotalPeso);

            // Bloque Saldo anterior / Abonos / Total actualizado
            boolean tieneSaldo = consolidado.saldoAnterior() != null
                    && consolidado.saldoAnterior().compareTo(BigDecimal.ZERO) != 0;
            boolean tieneAbonos = consolidado.totalAbonos() != null
                    && consolidado.totalAbonos().compareTo(BigDecimal.ZERO) != 0;

            if (tieneSaldo || tieneAbonos) {
                sheet.createRow(currentRowIdx++); // Fila en blanco

                Integer rSaldo = null;
                Integer rAbonos = null;

                if (tieneSaldo) {
                    int saldoRowIdx = currentRowIdx++;
                    rSaldo = saldoRowIdx + 1;
                    Row rS = sheet.createRow(saldoRowIdx);
                    Cell cSLabel = rS.createCell(7);
                    cSLabel.setCellValue("Saldo anterior");
                    cSLabel.setCellStyle(styleTotalEtiqueta);

                    Cell cSVal = rS.createCell(8);
                    cSVal.setCellValue(consolidado.saldoAnterior().doubleValue());
                    cSVal.setCellStyle(styleTotalPrecio);
                }

                if (tieneAbonos) {
                    int abonoRowIdx = currentRowIdx++;
                    rAbonos = abonoRowIdx + 1;
                    Row rA = sheet.createRow(abonoRowIdx);
                    Cell cALabel = rA.createCell(7);
                    cALabel.setCellValue("Menos abonos");
                    cALabel.setCellStyle(styleTotalEtiqueta);

                    Cell cAVal = rA.createCell(8);
                    cAVal.setCellValue(consolidado.totalAbonos().doubleValue());
                    cAVal.setCellStyle(styleTotalPrecio);
                }

                sheet.createRow(currentRowIdx++); // Fila en blanco

                int totalActRowIdx = currentRowIdx++;
                Row rAct = sheet.createRow(totalActRowIdx);
                Cell cActLabel = rAct.createCell(7);
                cActLabel.setCellValue("Total actualizado C$");
                cActLabel.setCellStyle(styleTotalEtiqueta);

                StringBuilder formulaAct = new StringBuilder("I").append(rTotC);
                if (rSaldo != null) {
                    formulaAct.append("+I").append(rSaldo);
                }
                if (rAbonos != null) {
                    formulaAct.append("-I").append(rAbonos);
                }

                Cell cActVal = rAct.createCell(8);
                cActVal.setCellFormula(formulaAct.toString());
                cActVal.setCellStyle(styleTotalPrecio);

                if (Boolean.TRUE.equals(consolidado.mostrarTotalUsd())) {
                    int rActC = totalActRowIdx + 1; // 1-based
                    int totalActUsdRowIdx = currentRowIdx++;
                    Row rActUsd = sheet.createRow(totalActUsdRowIdx);
                    Cell cActUsdLabel = rActUsd.createCell(7);
                    cActUsdLabel.setCellValue("Total actualizado U$");
                    cActUsdLabel.setCellStyle(styleTotalEtiqueta);

                    String tc = consolidado.tipoCambio() != null ? consolidado.tipoCambio().toPlainString() : "1";
                    Cell cActUsdVal = rActUsd.createCell(8);
                    cActUsdVal.setCellFormula("ROUND(I" + rActC + "/" + tc + ",2)");
                    cActUsdVal.setCellStyle(styleTotalUsd);
                }
            }

            // Pre-evaluación de fórmulas
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar archivo Excel de consolidado", e);
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
