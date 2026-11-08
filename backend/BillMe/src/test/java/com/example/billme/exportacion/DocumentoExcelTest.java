package com.example.billme.exportacion;

import com.example.billme.controller.DocumentoController;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.service.DocumentoService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoExcelTest {

    private final DocumentoExcel generador = new DocumentoExcel();

    @Test
    @DisplayName("Genera Excel canónico con fórmulas evaluadas, totales y celdas vacías sin peso")
    void generaExcelCanonicoConFormulas() throws IOException {
        Cliente cliente = Cliente.builder().nombre("María López").build();
        LocalDate fecha = LocalDate.of(2026, 10, 30);

        DocumentoLinea linea1 = DocumentoLinea.builder()
                .orden(0)
                .descripcion("Placa de acero 1/2 pulgada")
                .cantidad(new BigDecimal("2"))
                .pesoUnitKg(new BigDecimal("30.00"))
                .precioUnit(new BigDecimal("1500.00"))
                .build();

        DocumentoLinea linea2 = DocumentoLinea.builder()
                .orden(1)
                .descripcion("Servicio de corte")
                .cantidad(new BigDecimal("1.5"))
                .pesoUnitKg(null)
                .precioUnit(new BigDecimal("100.00"))
                .build();

        List<DocumentoLinea> lineas = new ArrayList<>();
        lineas.add(linea1);
        lineas.add(linea2);

        Documento doc = Documento.builder()
                .folio("FAC-0012")
                .tipo(TipoDocumento.FACTURA)
                .fecha(fecha)
                .cliente(cliente)
                .mostrarEntrega(false)
                .lineas(lineas)
                .build();

        byte[] contenido = generador.generar(doc, "María López");
        assertThat(contenido).isNotEmpty();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            // Hoja nombrada con el folio
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getSheetName()).isEqualTo("FAC-0012");

            // Fila 1 (rowIdx 0): H1:I1 Fecha
            Row fila1 = hoja.getRow(0);
            Cell cellH1 = fila1.getCell(7);
            assertThat(cellH1).isNotNull();
            assertThat(cellH1.getLocalDateTimeCellValue().toLocalDate()).isEqualTo(fecha);

            // Fila 2 (rowIdx 1): B2 Encabezado
            Row fila2 = hoja.getRow(1);
            Cell cellB2 = fila2.getCell(1);
            assertThat(cellB2.getStringCellValue()).isEqualTo("María López");

            // Fila 4 (rowIdx 3): Encabezados de tabla
            Row filaHeaders = hoja.getRow(3);
            assertThat(filaHeaders.getCell(1).getStringCellValue()).isEqualTo("Cantidad");
            assertThat(filaHeaders.getCell(2).getStringCellValue()).isEqualTo("Descripción");
            assertThat(filaHeaders.getCell(5).getStringCellValue()).isEqualTo("Peso Unit (KG)");
            assertThat(filaHeaders.getCell(6).getStringCellValue()).isEqualTo("Peso Total (KG)");
            assertThat(filaHeaders.getCell(7).getStringCellValue()).isEqualTo("Precio Unitario C$");
            assertThat(filaHeaders.getCell(8).getStringCellValue()).isEqualTo("Precio Total C$");
            // Sin mostrarEntrega, no deben existir cols J (9) y K (10)
            assertThat(filaHeaders.getCell(9)).isNull();
            assertThat(filaHeaders.getCell(10)).isNull();

            // Fila 5 (rowIdx 4): Línea 1
            Row fila5 = hoja.getRow(4);
            assertThat(fila5.getCell(1).getNumericCellValue()).isEqualTo(2.0);
            assertThat(fila5.getCell(2).getStringCellValue()).isEqualTo("Placa de acero 1/2 pulgada");
            assertThat(fila5.getCell(5).getNumericCellValue()).isEqualTo(30.0);
            // G5 fórmula ROUND(B5*F5,2) y evaluado 60.0
            Cell g5 = fila5.getCell(6);
            assertThat(g5.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(g5.getCellFormula()).isEqualTo("ROUND(B5*F5,2)");
            assertThat(g5.getNumericCellValue()).isEqualTo(60.0);
            // H5 precio unitario 1500.0
            assertThat(fila5.getCell(7).getNumericCellValue()).isEqualTo(1500.0);
            // I5 fórmula ROUND(B5*H5,2) y evaluado 3000.0
            Cell i5 = fila5.getCell(8);
            assertThat(i5.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(i5.getCellFormula()).isEqualTo("ROUND(B5*H5,2)");
            assertThat(i5.getNumericCellValue()).isEqualTo(3000.0);

            // Fila 6 (rowIdx 5): Línea 2
            Row fila6 = hoja.getRow(5);
            assertThat(fila6.getCell(1).getNumericCellValue()).isEqualTo(1.5);
            assertThat(fila6.getCell(2).getStringCellValue()).isEqualTo("Servicio de corte");
            // F6 y G6 vacías por tener pesoUnitKg == null
            Cell f6 = fila6.getCell(5);
            assertThat(f6.getCellType()).isEqualTo(CellType.BLANK);
            Cell g6 = fila6.getCell(6);
            assertThat(g6.getCellType()).isEqualTo(CellType.BLANK);
            // H6 y I6
            assertThat(fila6.getCell(7).getNumericCellValue()).isEqualTo(100.0);
            Cell i6 = fila6.getCell(8);
            assertThat(i6.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(i6.getCellFormula()).isEqualTo("ROUND(B6*H6,2)");
            assertThat(i6.getNumericCellValue()).isEqualTo(150.0);

            // Fila 7 (rowIdx 6): FIN DE LÍNEA
            Row filaFin = hoja.getRow(6);
            Cell cellFin = filaFin.getCell(1);
            assertThat(cellFin.getStringCellValue()).isEqualTo("*****FIN DE LÍNEA*****");
            // Verificar rango combinado de FIN DE LÍNEA: B7 a I7 (cols 1 a 8)
            boolean regionFinEncontrada = false;
            for (CellRangeAddress r : hoja.getMergedRegions()) {
                if (r.getFirstRow() == 6 && r.getLastRow() == 6 && r.getFirstColumn() == 1 && r.getLastColumn() == 8) {
                    regionFinEncontrada = true;
                    break;
                }
            }
            assertThat(regionFinEncontrada).isTrue();

            // Fila 9 (rowIdx 8): Total C$
            Row filaTotalC = hoja.getRow(8);
            assertThat(filaTotalC.getCell(7).getStringCellValue()).isEqualTo("Total C$");
            Cell cellTotalC = filaTotalC.getCell(8);
            assertThat(cellTotalC.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(cellTotalC.getCellFormula()).isEqualTo("SUM(I5:I6)");
            assertThat(cellTotalC.getNumericCellValue()).isEqualTo(3150.0);

            // Fila 10 (rowIdx 9): Total KG
            Row filaTotalKg = hoja.getRow(9);
            assertThat(filaTotalKg.getCell(7).getStringCellValue()).isEqualTo("Total KG");
            Cell cellTotalKg = filaTotalKg.getCell(8);
            assertThat(cellTotalKg.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(cellTotalKg.getCellFormula()).isEqualTo("SUM(G5:G6)");
            assertThat(cellTotalKg.getNumericCellValue()).isEqualTo(60.0);
        }
    }

    @Test
    @DisplayName("Genera columnas de entrega J y K cuando mostrarEntrega es true")
    void generaColumnasEntregaCuandoMostrarEntregaEsTrue() throws IOException {
        DocumentoLinea linea = DocumentoLinea.builder()
                .orden(0)
                .descripcion("Tubo de 2 pulgadas")
                .cantidad(new BigDecimal("10"))
                .pesoUnitKg(new BigDecimal("5.50"))
                .precioUnit(new BigDecimal("250.00"))
                .fechaEntrega(LocalDate.of(2026, 11, 10))
                .lugarEntrega("Obra Central")
                .build();

        Documento doc = Documento.builder()
                .folio("COT-0005")
                .tipo(TipoDocumento.COTIZACION)
                .fecha(LocalDate.of(2026, 10, 30))
                .cliente(Cliente.builder().nombre("Juan Pérez").build())
                .mostrarEntrega(true)
                .lineas(List.of(linea))
                .build();

        byte[] contenido = generador.generar(doc, "Juan Pérez / Cotización");
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);

            // Headers de fila 4 contienen Fecha entrega y Lugar entrega
            Row filaHeaders = hoja.getRow(3);
            assertThat(filaHeaders.getCell(9).getStringCellValue()).isEqualTo("Fecha entrega");
            assertThat(filaHeaders.getCell(10).getStringCellValue()).isEqualTo("Lugar entrega");

            // Fila 5 contiene los datos de entrega
            Row fila5 = hoja.getRow(4);
            assertThat(fila5.getCell(9).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 11, 10));
            assertThat(fila5.getCell(10).getStringCellValue()).isEqualTo("Obra Central");

            // Fila FIN DE LÍNEA combinada hasta K (columna 10)
            boolean regionFinHastaK = false;
            for (CellRangeAddress r : hoja.getMergedRegions()) {
                if (r.getFirstRow() == 5 && r.getLastRow() == 5 && r.getFirstColumn() == 1 && r.getLastColumn() == 10) {
                    regionFinHastaK = true;
                    break;
                }
            }
            assertThat(regionFinHastaK).isTrue();
        }
    }

    @Test
    @DisplayName("Genera fila Total U$ cuando mostrarTotalUsd es true")
    void generaFilaTotalUsdCuandoMostrarTotalUsdEsTrue() throws IOException {
        Cliente cliente = Cliente.builder().nombre("María López").build();
        DocumentoLinea linea = DocumentoLinea.builder()
                .orden(0)
                .descripcion("Tubo de 2 pulgadas")
                .cantidad(new BigDecimal("10"))
                .pesoUnitKg(new BigDecimal("5.50"))
                .precioUnit(new BigDecimal("366.243"))
                .build();

        Documento doc = Documento.builder()
                .folio("FAC-0015")
                .tipo(TipoDocumento.FACTURA)
                .fecha(LocalDate.of(2026, 11, 10))
                .cliente(cliente)
                .mostrarEntrega(false)
                .mostrarTotalUsd(true)
                .tipoCambio(new BigDecimal("36.6243"))
                .lineas(List.of(linea))
                .build();

        byte[] contenido = generador.generar(doc, "María López");
        assertThat(contenido).isNotEmpty();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);

            // Row 7 (rowIdx 7): Total C$
            Row filaTotalC = hoja.getRow(7);
            assertThat(filaTotalC.getCell(7).getStringCellValue()).isEqualTo("Total C$");

            // Row 8 (rowIdx 8): Total U$
            Row filaTotalUsd = hoja.getRow(8);
            assertThat(filaTotalUsd.getCell(7).getStringCellValue()).isEqualTo("Total U$");
            Cell cellTotalUsd = filaTotalUsd.getCell(8);
            assertThat(cellTotalUsd.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(cellTotalUsd.getCellFormula()).isEqualTo("ROUND(I8/36.6243,2)");
            assertThat(cellTotalUsd.getNumericCellValue()).isEqualTo(100.0);

            // Row 9 (rowIdx 9): Total KG
            Row filaTotalKg = hoja.getRow(9);
            assertThat(filaTotalKg.getCell(7).getStringCellValue()).isEqualTo("Total KG");
        }
    }

    @Test
    @DisplayName("NombreArchivo normaliza caracteres diacríticos, espacios y extensiones")
    void normalizacionNombreArchivo() {
        String nombre1 = NombreArchivo.de("FAC-0012", "María López", LocalDate.of(2026, 10, 30), "pdf");
        assertThat(nombre1).isEqualTo("FAC-0012_Maria-Lopez_2026-10-30.pdf");

        String nombre2 = NombreArchivo.de("FAC-0012", "María López", LocalDate.of(2026, 10, 30), ".xlsx");
        assertThat(nombre2).isEqualTo("FAC-0012_Maria-Lopez_2026-10-30.xlsx");

        String nombre3 = NombreArchivo.de("COT-0001", "Cliente de prueba", LocalDate.of(2026, 11, 1), "xlsx");
        assertThat(nombre3).isEqualTo("COT-0001_Cliente-de-prueba_2026-11-01.xlsx");

        String nombre4 = NombreArchivo.de("FAC-0099", "--- Constructora & Acabados, S.A. ---", LocalDate.of(2026, 12, 5), "xlsx");
        assertThat(nombre4).isEqualTo("FAC-0099_Constructora-Acabados-S-A_2026-12-05.xlsx");
    }

    @Test
    @DisplayName("encabezadoPorDefecto arma la cadena esperada según tipo y proyecto")
    void encabezadoPorDefectoSegunTipoYProyecto() {
        DocumentoService service = new DocumentoService(null, null, null, null, null, null, null, null, null, null, generador);

        // Cotización con cliente y proyecto
        Documento cotConProyecto = Documento.builder()
                .tipo(TipoDocumento.COTIZACION)
                .cliente(Cliente.builder().nombre("Cliente de prueba").build())
                .proyecto(Proyecto.builder().nombre("Obra centro").build())
                .build();
        assertThat(service.encabezadoPorDefecto(cotConProyecto))
                .isEqualTo("Cliente de prueba / Obra centro / Cotización");

        // Cotización sin proyecto
        Documento cotSinProyecto = Documento.builder()
                .tipo(TipoDocumento.COTIZACION)
                .cliente(Cliente.builder().nombre("María López").build())
                .build();
        assertThat(service.encabezadoPorDefecto(cotSinProyecto))
                .isEqualTo("María López / Cotización");

        // Factura con proyecto
        Documento facConProyecto = Documento.builder()
                .tipo(TipoDocumento.FACTURA)
                .cliente(Cliente.builder().nombre("Cliente de prueba").build())
                .proyecto(Proyecto.builder().nombre("Obra centro").build())
                .build();
        assertThat(service.encabezadoPorDefecto(facConProyecto))
                .isEqualTo("Cliente de prueba / Obra centro");

        // Factura sin proyecto
        Documento facSinProyecto = Documento.builder()
                .tipo(TipoDocumento.FACTURA)
                .cliente(Cliente.builder().nombre("María López").build())
                .build();
        assertThat(service.encabezadoPorDefecto(facSinProyecto))
                .isEqualTo("María López");
    }

    @Test
    @DisplayName("DocumentoService exportarExcel genera ArchivoExcel con contenido y nombre correctos")
    void servicioExportarExcel() {
        DocumentoRepository docRepo = Mockito.mock(DocumentoRepository.class);
        Documento doc = Documento.builder()
                .folio("FAC-0012")
                .tipo(TipoDocumento.FACTURA)
                .fecha(LocalDate.of(2026, 10, 30))
                .cliente(Cliente.builder().nombre("María López").build())
                .lineas(List.of())
                .build();
        UUID id = UUID.randomUUID();
        Mockito.when(docRepo.findById(id)).thenReturn(Optional.of(doc));

        DocumentoService service = new DocumentoService(docRepo, null, null, null, null, null, null, null, null, null, generador);
        ArchivoExcel archivo = service.exportarExcel(id, null);

        assertThat(archivo.nombre()).isEqualTo("FAC-0012_Maria-Lopez_2026-10-30.xlsx");
        assertThat(archivo.contenido()).isNotEmpty();

        ArchivoExcel archivoPersonalizado = service.exportarExcel(id, "  Encabezado Especial  ");
        assertThat(archivoPersonalizado.nombre()).isEqualTo("FAC-0012_Maria-Lopez_2026-10-30.xlsx");
        assertThat(archivoPersonalizado.contenido()).isNotEmpty();
    }

    @Test
    @DisplayName("DocumentoController exportarExcel retorna ResponseEntity con headers y body correctos")
    void controllerExportarExcel() {
        DocumentoService service = Mockito.mock(DocumentoService.class);
        DocumentoController controller = new DocumentoController(service);
        UUID id = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3};
        Mockito.when(service.exportarExcel(id, "Mi Encabezado"))
                .thenReturn(new ArchivoExcel("FAC-0012_Maria-Lopez_2026-10-30.xlsx", bytes));

        ResponseEntity<byte[]> response = controller.exportarExcel(id, "Mi Encabezado");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"FAC-0012_Maria-Lopez_2026-10-30.xlsx\"");
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getBody()).isEqualTo(bytes);
    }
}
