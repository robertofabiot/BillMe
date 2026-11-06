package com.example.billme.exportacion;

import com.example.billme.dto.ConsolidadoResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolidadoExcelTest {

    private final ConsolidadoExcel generador = new ConsolidadoExcel();

    @Test
    @DisplayName("Genera Excel de consolidado con fórmulas de subtotales, totales y total actualizado")
    void generaExcelConSubtotalesYTotalActualizado() throws IOException {
        UUID clienteId = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);

        ConsolidadoResponse.Linea l1 = new ConsolidadoResponse.Linea(
                UUID.randomUUID(), "FAC-0001", new BigDecimal("1"), "Línea 1",
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), null
        );
        ConsolidadoResponse.Linea l2 = new ConsolidadoResponse.Linea(
                UUID.randomUUID(), "FAC-0001", new BigDecimal("1"), "Línea 2",
                new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("500.00"), new BigDecimal("500.00"), null
        );
        ConsolidadoResponse.Linea l3 = new ConsolidadoResponse.Linea(
                UUID.randomUUID(), "FAC-0002", new BigDecimal("2"), "Línea 3",
                new BigDecimal("20.00"), new BigDecimal("40.00"), new BigDecimal("1000.00"), new BigDecimal("2000.00"), null
        );

        ConsolidadoResponse.Grupo g1 = new ConsolidadoResponse.Grupo(
                LocalDate.of(2026, 3, 2), "Subtotal 1 (2 de marzo)", List.of(l1),
                new BigDecimal("1000.00"), new BigDecimal("10.00")
        );
        ConsolidadoResponse.Grupo g2 = new ConsolidadoResponse.Grupo(
                LocalDate.of(2026, 3, 5), "Subtotal 2 (5 de marzo)", List.of(l2, l3),
                new BigDecimal("2500.00"), new BigDecimal("45.00")
        );

        ConsolidadoResponse.AbonoDelPeriodo abono = new ConsolidadoResponse.AbonoDelPeriodo(
                LocalDate.of(2026, 3, 20), new BigDecimal("1500.00"), "Transferencia BAC"
        );

        ConsolidadoResponse response = new ConsolidadoResponse(
                UUID.randomUUID(),
                "CONS-0001",
                "Consolidado Cliente de prueba",
                clienteId,
                "Cliente de prueba",
                null,
                null,
                desde,
                hasta,
                Instant.now(),
                List.of(g1, g2),
                new BigDecimal("3500.00"),
                new BigDecimal("55.00"),
                new BigDecimal("4000.00"), // saldo anterior
                List.of(abono),
                new BigDecimal("1500.00"), // total abonos
                new BigDecimal("6000.00")  // total actualizado = 4000 + 3500 - 1500
        );

        byte[] bytes = generador.generar(response, null);
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getSheetName()).isEqualTo("CONS-0001");

            // Fila 2 (rowIdx 1): Encabezado por defecto clienteNombre
            assertThat(hoja.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Cliente de prueba");

            // Fila 4 (rowIdx 3): Headers
            assertThat(hoja.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Cantidad");

            // Fila 5 (rowIdx 4): Línea 1 de Grupo 1
            Row rL1 = hoja.getRow(4);
            assertThat(rL1.getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(rL1.getCell(6).getCellFormula()).isEqualTo("ROUND(B5*F5,2)");
            assertThat(rL1.getCell(8).getCellFormula()).isEqualTo("ROUND(B5*H5,2)");

            // Fila 6 (rowIdx 5): Subtotal 1
            Row rSub1 = hoja.getRow(5);
            assertThat(rSub1.getCell(2).getStringCellValue()).isEqualTo("Subtotal 1 (2 de marzo)");
            assertThat(rSub1.getCell(6).getCellFormula()).isEqualTo("SUM(G5:G5)");
            assertThat(rSub1.getCell(6).getNumericCellValue()).isEqualTo(10.0);
            assertThat(rSub1.getCell(8).getCellFormula()).isEqualTo("SUM(I5:I5)");
            assertThat(rSub1.getCell(8).getNumericCellValue()).isEqualTo(1000.0);

            // Fila 7 (rowIdx 6): Línea 2 de Grupo 2
            Row rL2 = hoja.getRow(6);
            assertThat(rL2.getCell(1).getNumericCellValue()).isEqualTo(1.0);

            // Fila 8 (rowIdx 7): Línea 3 de Grupo 2
            Row rL3 = hoja.getRow(7);
            assertThat(rL3.getCell(1).getNumericCellValue()).isEqualTo(2.0);

            // Fila 9 (rowIdx 8): Subtotal 2
            Row rSub2 = hoja.getRow(8);
            assertThat(rSub2.getCell(2).getStringCellValue()).isEqualTo("Subtotal 2 (5 de marzo)");
            assertThat(rSub2.getCell(6).getCellFormula()).isEqualTo("SUM(G7:G8)");
            assertThat(rSub2.getCell(6).getNumericCellValue()).isEqualTo(45.0);
            assertThat(rSub2.getCell(8).getCellFormula()).isEqualTo("SUM(I7:I8)");
            assertThat(rSub2.getCell(8).getNumericCellValue()).isEqualTo(2500.0);

            // Fila 10 (rowIdx 9): FIN DE LÍNEA
            assertThat(hoja.getRow(9).getCell(1).getStringCellValue()).isEqualTo("*****FIN DE LÍNEA*****");

            // Fila 12 (rowIdx 11): Total C$
            Row rTotC = hoja.getRow(11);
            assertThat(rTotC.getCell(7).getStringCellValue()).isEqualTo("Total C$");
            assertThat(rTotC.getCell(8).getCellFormula()).isEqualTo("SUM(I6,I9)");
            assertThat(rTotC.getCell(8).getNumericCellValue()).isEqualTo(3500.0);

            // Fila 13 (rowIdx 12): Total KG
            Row rTotKg = hoja.getRow(12);
            assertThat(rTotKg.getCell(7).getStringCellValue()).isEqualTo("Total KG");
            assertThat(rTotKg.getCell(8).getCellFormula()).isEqualTo("SUM(G6,G9)");
            assertThat(rTotKg.getCell(8).getNumericCellValue()).isEqualTo(55.0);

            // Fila 15 (rowIdx 14): Saldo anterior
            Row rSaldo = hoja.getRow(14);
            assertThat(rSaldo.getCell(7).getStringCellValue()).isEqualTo("Saldo anterior");
            assertThat(rSaldo.getCell(8).getNumericCellValue()).isEqualTo(4000.0);

            // Fila 16 (rowIdx 15): Menos abonos
            Row rAbonos = hoja.getRow(15);
            assertThat(rAbonos.getCell(7).getStringCellValue()).isEqualTo("Menos abonos");
            assertThat(rAbonos.getCell(8).getNumericCellValue()).isEqualTo(1500.0);

            // Fila 18 (rowIdx 17): Total actualizado C$
            Row rAct = hoja.getRow(17);
            assertThat(rAct.getCell(7).getStringCellValue()).isEqualTo("Total actualizado C$");
            assertThat(rAct.getCell(8).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(rAct.getCell(8).getCellFormula()).isEqualTo("I12+I15-I16");
            assertThat(rAct.getCell(8).getNumericCellValue()).isEqualTo(6000.0);
        }
    }

    @Test
    @DisplayName("Sin saldo ni abonos, no existen las filas de saldo anterior, abonos ni total actualizado")
    void sinSaldoNiAbonosNoExistenFilasExtra() throws IOException {
        ConsolidadoResponse.Linea l1 = new ConsolidadoResponse.Linea(
                UUID.randomUUID(), "FAC-0001", new BigDecimal("1"), "Línea 1",
                null, null, new BigDecimal("1000.00"), new BigDecimal("1000.00"), null
        );
        ConsolidadoResponse.Grupo g1 = new ConsolidadoResponse.Grupo(
                LocalDate.of(2026, 3, 2), "Subtotal 1 (2 de marzo)", List.of(l1),
                new BigDecimal("1000.00"), BigDecimal.ZERO
        );

        ConsolidadoResponse response = new ConsolidadoResponse(
                UUID.randomUUID(),
                "CONS-0001",
                "Consolidado Simple",
                UUID.randomUUID(),
                "Cliente Simple",
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                Instant.now(),
                List.of(g1),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO, // saldoAnterior = 0
                List.of(),
                BigDecimal.ZERO, // totalAbonos = 0
                new BigDecimal("1000.00")
        );

        byte[] bytes = generador.generar(response, "Encabezado Custom");
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet hoja = libro.getSheetAt(0);

            // Fila 2: Encabezado personalizado
            assertThat(hoja.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Encabezado Custom");

            // Recorremos todas las filas y ninguna debe tener "Saldo anterior", "Menos abonos" ni "Total actualizado C$"
            for (Row row : hoja) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String txt = cell.getStringCellValue();
                        assertThat(txt).doesNotContain("Saldo anterior");
                        assertThat(txt).doesNotContain("Menos abonos");
                        assertThat(txt).doesNotContain("Total actualizado C$");
                        assertThat(txt).doesNotContain("Total U$");
                        assertThat(txt).doesNotContain("Total actualizado U$");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Con mostrarTotalUsd=true genera fila Total U$ y Total actualizado U$")
    void generaFilasTotalUsdCuandoMostrarTotalUsdEsTrue() throws IOException {
        UUID clienteId = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 3, 1);
        LocalDate hasta = LocalDate.of(2026, 3, 31);

        ConsolidadoResponse.Linea l1 = new ConsolidadoResponse.Linea(
                UUID.randomUUID(), "FAC-0001", new BigDecimal("1"), "Línea 1",
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("3662.43"), new BigDecimal("3662.43"), null
        );

        ConsolidadoResponse.Grupo g1 = new ConsolidadoResponse.Grupo(
                LocalDate.of(2026, 3, 2), "Subtotal 1 (2 de marzo)", List.of(l1),
                new BigDecimal("3662.43"), new BigDecimal("10.00")
        );

        ConsolidadoResponse.AbonoDelPeriodo abono = new ConsolidadoResponse.AbonoDelPeriodo(
                LocalDate.of(2026, 3, 20), new BigDecimal("1500.00"), "Transferencia"
        );

        ConsolidadoResponse response = new ConsolidadoResponse(
                UUID.randomUUID(),
                "CONS-0001",
                "Consolidado USD",
                clienteId,
                "Cliente USD",
                null,
                null,
                desde,
                hasta,
                Instant.now(),
                List.of(g1),
                new BigDecimal("3662.43"),
                new BigDecimal("10.00"),
                new BigDecimal("4000.00"),
                List.of(abono),
                new BigDecimal("1500.00"),
                new BigDecimal("6162.43"),
                true,
                new BigDecimal("36.624300"),
                new BigDecimal("100.00"),
                new BigDecimal("168.26")
        );

        byte[] bytes = generador.generar(response, null);
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet hoja = libro.getSheetAt(0);

            // Fila 9 (rowIdx 8): Total C$
            Row rTotC = hoja.getRow(8);
            assertThat(rTotC.getCell(7).getStringCellValue()).isEqualTo("Total C$");
            assertThat(rTotC.getCell(8).getNumericCellValue()).isEqualTo(3662.43);

            // Fila 10 (rowIdx 9): Total U$
            Row rTotUsd = hoja.getRow(9);
            assertThat(rTotUsd.getCell(7).getStringCellValue()).isEqualTo("Total U$");
            assertThat(rTotUsd.getCell(8).getCellFormula()).isEqualTo("ROUND(I9/36.624300,2)");
            assertThat(rTotUsd.getCell(8).getNumericCellValue()).isEqualTo(100.0);

            // Fila 11 (rowIdx 10): Total KG
            Row rTotKg = hoja.getRow(10);
            assertThat(rTotKg.getCell(7).getStringCellValue()).isEqualTo("Total KG");
            assertThat(rTotKg.getCell(8).getNumericCellValue()).isEqualTo(10.0);

            // Fila 13 (rowIdx 12): Saldo anterior
            Row rSaldo = hoja.getRow(12);
            assertThat(rSaldo.getCell(7).getStringCellValue()).isEqualTo("Saldo anterior");
            assertThat(rSaldo.getCell(8).getNumericCellValue()).isEqualTo(4000.0);

            // Fila 14 (rowIdx 13): Menos abonos
            Row rAbonos = hoja.getRow(13);
            assertThat(rAbonos.getCell(7).getStringCellValue()).isEqualTo("Menos abonos");
            assertThat(rAbonos.getCell(8).getNumericCellValue()).isEqualTo(1500.0);

            // Fila 16 (rowIdx 15): Total actualizado C$
            Row rActC = hoja.getRow(15);
            assertThat(rActC.getCell(7).getStringCellValue()).isEqualTo("Total actualizado C$");
            assertThat(rActC.getCell(8).getCellFormula()).isEqualTo("I9+I13-I14");
            assertThat(rActC.getCell(8).getNumericCellValue()).isEqualTo(6162.43);

            // Fila 17 (rowIdx 16): Total actualizado U$
            Row rActUsd = hoja.getRow(16);
            assertThat(rActUsd.getCell(7).getStringCellValue()).isEqualTo("Total actualizado U$");
            assertThat(rActUsd.getCell(8).getCellFormula()).isEqualTo("ROUND(I16/36.624300,2)");
            assertThat(rActUsd.getCell(8).getNumericCellValue()).isEqualTo(168.26);
        }
    }
}
