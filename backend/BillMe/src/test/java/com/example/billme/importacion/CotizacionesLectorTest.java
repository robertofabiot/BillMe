package com.example.billme.importacion;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CotizacionesLectorTest {

    private final CotizacionesLector lector = new CotizacionesLector();

    @Test
    void eligeLaHojaCotizacionesEntreVarias() {
        byte[] archivo = new ExcelDePrueba()
                .hoja("Resumen")
                .fila("fecha", "cantidad", "nombre_cotizacion", "precio_unitario_cotizacion")
                .fila(LocalDate.of(2023, 10, 1), 1, "Item en otra hoja", 50.0)
                .hoja("Cotizaciones")
                .fila("fecha", "cantidad", "nombre_cotizacion", "precio_unitario_cotizacion")
                .fila(LocalDate.of(2023, 10, 15), 5, "Item cotizaciones", 100.0)
                .hoja("Notas")
                .fila("Comentarios varios")
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.hoja()).isEqualTo("Cotizaciones");
        assertThat(lectura.filas()).hasSize(1);
        assertThat(lectura.filas().get(0).descripcion()).isEqualTo("Item cotizaciones");
    }

    @Test
    void leeFechaDeSerieYDeTexto() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2023, 10, 15), 2, "Tubo industrial", 200.0)
                .fila("25/12/2023", 3, "Lamina lisa", 350.0)
                .fila("5/3/2023", 1, "Perlin estructural", 500.0)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(3);
        assertThat(lectura.filas().get(0).fecha()).isEqualTo(LocalDate.of(2023, 10, 15));
        assertThat(lectura.filas().get(1).fecha()).isEqualTo(LocalDate.of(2023, 12, 25));
        assertThat(lectura.filas().get(2).fecha()).isEqualTo(LocalDate.of(2023, 3, 5));
    }

    @Test
    void leeCodigoDeTextoConCerosYRespetaCeros() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2023, 10, 15), 1, "Perlin Negro", 4100.0, "001001169")
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(1);
        assertThat(lectura.filas().get(0).codigo()).isEqualTo("001001169");
    }

    @Test
    void completaCodigoNumericoA9Digitos() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2023, 10, 15), 1, "Perlin Negro", 4100.0, 1001169)
                .fila(LocalDate.of(2023, 10, 16), 2, "Platina especial", 850.0, 45)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(2);
        assertThat(lectura.filas().get(0).codigo()).isEqualTo("001001169");
        assertThat(lectura.filas().get(1).codigo()).isEqualTo("000000045");
    }

    @Test
    void codigoCeroOTextoCeroQuedaComoNull() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2023, 10, 15), 1, "Item sin codigo texto", 100.0, "0")
                .fila(LocalDate.of(2023, 10, 16), 1, "Item sin codigo numero", 100.0, 0)
                .fila(LocalDate.of(2023, 10, 17), 1, "Item codigo vacio", 100.0, "")
                .fila(LocalDate.of(2023, 10, 18), 1, "Item codigo null", 100.0, null)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(4);
        assertThat(lectura.filas()).allMatch(f -> f.codigo() == null);
    }

    @Test
    void pesoUnitarioConTextoUsaPesoTotalEntreCantidad() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                // peso_unitario con texto -> debe calcular peso_total / cantidad (100 / 4 = 25)
                .fila(LocalDate.of(2023, 10, 15), 4, "Angulo 2x2", 400.0, null, "kg", 100.0)
                // peso_unitario numérico positivo directo -> debe usar el peso unitario
                .fila(LocalDate.of(2023, 10, 16), 2, "Platina 1/4", 300.0, null, 12.5, 50.0)
                // sin peso ni total -> peso null
                .fila(LocalDate.of(2023, 10, 17), 1, "Servicio corte", 150.0, null, null, null)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(3);
        assertThat(lectura.filas().get(0).pesoUnitKg()).isEqualByComparingTo("25");
        assertThat(lectura.filas().get(1).pesoUnitKg()).isEqualByComparingTo("12.5");
        assertThat(lectura.filas().get(2).pesoUnitKg()).isNull();
    }

    @Test
    void ignoraFilasVaciasEnSilencio() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2023, 10, 15), 1, "Fila valida 1", 100.0)
                .fila() // fila completamente vacia
                .fila(null, null, null, null) // celdas nulas
                .fila(null, "   ", "   ", null) // espacios
                .fila(LocalDate.of(2023, 10, 16), 2, "Fila valida 2", 200.0)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).extracting(FilaCotizacion::descripcion)
                .containsExactly("Fila valida 1", "Fila valida 2");
        assertThat(lectura.incompletas()).isEmpty();
    }

    @Test
    void registraFilaIncompletaConMotivo() {
        byte[] archivo = ExcelDePrueba.cotizaciones()
                // Falta fecha
                .fila(null, 2, "Fila sin fecha", 100.0)
                // Cantidad <= 0
                .fila(LocalDate.of(2023, 10, 15), 0, "Fila cantidad cero", 100.0)
                .fila(LocalDate.of(2023, 10, 15), -3, "Fila cantidad negativa", 100.0)
                // Descripción vacía
                .fila(LocalDate.of(2023, 10, 15), 1, "   ", 100.0)
                // Precio <= 0
                .fila(LocalDate.of(2023, 10, 15), 1, "Fila precio cero", 0)
                .fila(LocalDate.of(2023, 10, 15), 1, "Fila precio negativo", -50.0)
                // Fila válida
                .fila(LocalDate.of(2023, 10, 15), 1, "Fila valida", 100.0)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(1);
        assertThat(lectura.filas().get(0).descripcion()).isEqualTo("Fila valida");

        assertThat(lectura.incompletas()).hasSize(6);
        for (var incompleta : lectura.incompletas()) {
            assertThat(incompleta.fila()).isGreaterThan(1);
            assertThat(incompleta.motivo()).isNotBlank();
        }
        assertThat(lectura.incompletas().get(0).motivo()).containsIgnoringCase("fecha");
        assertThat(lectura.incompletas().get(1).motivo()).containsIgnoringCase("cantidad");
        assertThat(lectura.incompletas().get(2).motivo()).containsIgnoringCase("cantidad");
        assertThat(lectura.incompletas().get(3).motivo()).containsIgnoringCase("descripción");
        assertThat(lectura.incompletas().get(4).motivo()).containsIgnoringCase("precio");
        assertThat(lectura.incompletas().get(5).motivo()).containsIgnoringCase("precio");
    }

    @Test
    void sinHojaValidaDa400() {
        byte[] sinColumnas = new ExcelDePrueba()
                .hoja("Hoja1")
                .fila("col1", "col2", "col3")
                .fila("val1", "val2", "val3")
                .bytes();

        assertThatThrownBy(() -> leer(sinColumnas))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se encontró una hoja con fecha, cantidad, nombre_cotizacion y precio_unitario_cotizacion.");

        assertThatThrownBy(() -> leer("contenido no zip".getBytes()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se pudo leer el archivo. Debe ser un Excel (.xlsx o .xlsm).");
    }

    @Test
    void leeHojaSinCotizacionesPeroConColumnasObligatoriasEnSegundaPasada() {
        byte[] archivo = new ExcelDePrueba()
                .hoja("DatosImportacion")
                .fila("fecha", "cantidad", "nombre_cotizacion", "precio_unitario_cotizacion")
                .fila(LocalDate.of(2023, 11, 1), 10, "Placa base 1/2", 1500.0)
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.hoja()).isEqualTo("DatosImportacion");
        assertThat(lectura.filas()).hasSize(1);
        assertThat(lectura.filas().get(0).descripcion()).isEqualTo("Placa base 1/2");
    }

    @Test
    void mapeaTodosLosCamposOpcionalesYRecortaEntregadoYRedondeaPrecio() {
        String entregadoLargo = "A".repeat(250);
        byte[] archivo = ExcelDePrueba.cotizaciones()
                .fila(
                        LocalDate.of(2023, 10, 15), // fecha
                        2.5,                        // cantidad
                        "Lamina Lisa",              // nombre_cotizacion
                        123.456,                    // precio_unitario_cotizacion -> redondeado r2: 123.46
                        "001001169",                // cod_producto
                        10.0,                       // peso_unitario
                        25.0,                       // peso_total
                        95.50,                      // precio_unitario_facturado (costoSinIva)
                        "Entregar por la tarde",    // observaciones
                        entregadoLargo,             // Entregado (> 200 chars)
                        "Edificio Central",         // proyecto
                        "FAC-001",                  // num_factura_dia
                        "Cliente de prueba"         // cliente
                )
                .bytes();

        LecturaCotizaciones lectura = leer(archivo);

        assertThat(lectura.filas()).hasSize(1);
        FilaCotizacion fila = lectura.filas().get(0);

        assertThat(fila.fecha()).isEqualTo(LocalDate.of(2023, 10, 15));
        assertThat(fila.cantidad()).isEqualByComparingTo("2.5");
        assertThat(fila.descripcion()).isEqualTo("Lamina Lisa");
        assertThat(fila.precioUnit()).isEqualByComparingTo("123.46");
        assertThat(fila.codigo()).isEqualTo("001001169");
        assertThat(fila.pesoUnitKg()).isEqualByComparingTo("10.0");
        assertThat(fila.costoSinIva()).isEqualByComparingTo("95.50");
        assertThat(fila.observaciones()).isEqualTo("Entregar por la tarde");
        assertThat(fila.entregado()).hasSize(200).isEqualTo("A".repeat(200));
        assertThat(fila.proyecto()).isEqualTo("Edificio Central");
        assertThat(fila.numFacturaDia()).isEqualTo("FAC-001");
        assertThat(fila.cliente()).isEqualTo("Cliente de prueba");
    }

    private LecturaCotizaciones leer(byte[] archivo) {
        return lector.leer(new ByteArrayInputStream(archivo));
    }
}
