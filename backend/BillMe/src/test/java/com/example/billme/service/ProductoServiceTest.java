package com.example.billme.service;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.PrecioLista;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoServiceTest extends IntegrationTest {

    @Autowired
    ProductoService productoService;

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    PrecioListaRepository precioListaRepository;

    @Autowired
    ClienteService clienteService;

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ReporteService reporteService;

    @Test
    void derivaFamiliaYNombreNormalizado() {
        ProductoResponse perlin = crear("001001116", "Perlin Negro 2 x  6 x 2.38mm");

        var entidad = productoRepository.findById(perlin.id()).orElseThrow();
        assertThat(perlin.familia()).isEqualTo("001");
        assertThat(entidad.getNombreNorm()).isEqualTo("perlin negro 2 x 6 x 2.38mm");
        assertThat(perlin.unidad()).isEqualTo("UND");
    }

    @Test
    void codigosPropiosNoTienenFamilia() {
        assertThat(crear("PER-34", "Perno F1554 3/4").familia()).isNull();
    }

    @Test
    void muestraElPrecioDeListaMasReciente() {
        ProductoResponse perlin = crear("001001116", "Perlin Negro 2 x 6 x 2.38mm");
        registrarLista(perlin, "950.00", LocalDate.of(2025, 2, 1), FuentePrecio.LISTA_IMPORTADA, null);
        registrarLista(perlin, "1000.00", LocalDate.of(2025, 6, 1), FuentePrecio.LISTA_IMPORTADA, null);

        ProductoResponse leido = productoService.obtenerPorId(perlin.id());

        assertThat(leido.precioListaVigente()).isEqualByComparingTo("1000.00");
        assertThat(leido.fechaPrecioLista()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(productoService.listarRecientes())
                .filteredOn(p -> p.id().equals(perlin.id()))
                .singleElement()
                .satisfies(p -> assertThat(p.precioListaVigente()).isEqualByComparingTo("1000.00"));
    }

    @Test
    void losGenericosExistenYNoSePuedenBorrar() {
        var placa = productoRepository.findByCodigoInterno("GEN-PLACA").orElseThrow();
        assertThat(placa.isGenerico()).isTrue();
        assertThat(productoRepository.findByCodigoInterno("GEN-LIBRE")).isPresent();

        assertThatThrownBy(() -> productoService.eliminar(placa.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void laFacturaGuardaElCostoEstimadoConLaListaYElDescuentoDeLaFamilia() {
        ProductoResponse perlin = crear("001001116", "Perlin Negro 2 x 6 x 2.38mm");
        ProductoResponse otroPerlin = crear("001001074", "Perlin Negro 2 x 4 x 3.20mm");
        // El descuento de la familia 001 sale de una lista calculada desde la factura del proveedor
        registrarLista(otroPerlin, "1200.00", LocalDate.of(2025, 2, 1), FuentePrecio.FACTURA_PROVEEDOR, "20.00");
        registrarLista(perlin, "1000.00", LocalDate.of(2025, 2, 1), FuentePrecio.LISTA_IMPORTADA, null);
        var cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));

        var factura = documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), null,
                LocalDate.of(2026, 10, 1), null, false, List.of(new LineaRequest(null, perlin.id(), null,
                new BigDecimal("5"), null, new BigDecimal("1300.00"), null, null, null, null))));

        // 1000.00 × (1 − 0.20) × 1.15 = 920.00
        assertThat(factura.lineas().get(0).costoUnitConIva()).isEqualByComparingTo("920.00");
        assertThat(reporteService.generarReporte(cliente.id(), null, null, null).costoTotal()).isEqualByComparingTo("4600.00");
    }

    private ProductoResponse crear(String codigo, String nombre) {
        return productoService.crear(new ProductoRequest(codigo, nombre, null, null, null, List.of()));
    }

    private void registrarLista(ProductoResponse producto, String precio, LocalDate fecha,
                                FuentePrecio fuente, String descuento) {
        precioListaRepository.save(PrecioLista.builder()
                .producto(productoRepository.getReferenceById(producto.id()))
                .precioSinIva(new BigDecimal(precio))
                .fecha(fecha)
                .fuente(fuente)
                .descuentoPct(descuento != null ? new BigDecimal(descuento) : null)
                .build());
    }
}
