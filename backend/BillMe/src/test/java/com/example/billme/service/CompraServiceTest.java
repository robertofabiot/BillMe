package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.ProveedorRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser
class CompraServiceTest extends IntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    CompraService compraService;

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProductoService productoService;

    @Autowired
    PrecioListaService precioListaService;

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    ProveedorRepository proveedorRepository;

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    PrecioListaRepository precioListaRepository;

    ClienteResponse cliente;
    ProductoResponse tubo;
    ProductoResponse otroProducto;
    Proveedor proveedor;

    @BeforeEach
    void setup() {
        cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        tubo = productoService.crear(new ProductoRequest("009001271", "Tubo Estruct. Cua. 4 x 4", null, null,
                new BigDecimal("44.17"), List.of()));
        otroProducto = productoService.crear(new ProductoRequest("009001272", "Tubo Estruct. Cua. 2 x 2", null, null,
                new BigDecimal("20.00"), List.of()));
        proveedor = proveedorRepository.findByNombre("Proveedor principal").orElseThrow();
    }

    private DocumentoResponse crearFactura(BigDecimal cantidad, BigDecimal pu) {
        return documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, tubo.id(), null, cantidad, null, pu, null, null, null, null))
        ));
    }

    @Test
    void caso1_precioSinIva950CalculaCostoConIva1092_50() {
        DocumentoResponse factura = crearFactura(new BigDecimal("2"), new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        CompraResponse response = compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-100",
                "Nota compra",
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("950.00"), null, null))
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.proveedorId()).isEqualTo(proveedor.getId());
        assertThat(response.proveedorNombre()).isEqualTo("Proveedor principal");
        assertThat(response.numeroFactura()).isEqualTo("F-100");
        assertThat(response.nota()).isEqualTo("Nota compra");

        DocumentoResponse doc = documentoService.obtener(factura.id());
        LineaResponse linea = doc.lineas().get(0);
        assertThat(linea.costoUnitConIva()).isEqualByComparingTo("1092.50");
        assertThat(linea.compraId()).isNotNull();
        assertThat(linea.compraId()).isEqualTo(response.id());
    }

    @Test
    void caso2_precio840ConDescuento16Costo966YPrecioLista1000() {
        DocumentoResponse factura = crearFactura(new BigDecimal("1"), new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-200",
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("840.00"), new BigDecimal("16.00"), null))
        ));

        DocumentoResponse doc = documentoService.obtener(factura.id());
        LineaResponse linea = doc.lineas().get(0);
        // 840.00 * 1.15 = 966.00
        assertThat(linea.costoUnitConIva()).isEqualByComparingTo("966.00");

        // 840.00 / (1 - 0.16) = 1000.0000 con FACTURA_PROVEEDOR
        var vigente = precioListaService.vigente(tubo.id()).orElseThrow();
        assertThat(vigente.getPrecioSinIva()).isEqualByComparingTo("1000.0000");
        assertThat(vigente.getFuente()).isEqualTo(FuentePrecio.FACTURA_PROVEEDOR);
        assertThat(vigente.getDescuentoPct()).isEqualByComparingTo("16.00");
        assertThat(vigente.getNota()).isEqualTo("Compra F-200");
    }

    @Test
    void caso3_sinDescuentoNoCreaPrecioDeLista() {
        DocumentoResponse factura = crearFactura(new BigDecimal("1"), new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("840.00"), null, null))
        ));

        assertThat(precioListaService.vigente(tubo.id())).isEmpty();
    }

    @Test
    void caso4_genericoConDescuentoNoCreaPrecioDeLista() {
        UUID genericoLibreId = productoRepository.findByCodigoInterno("GEN-LIBRE").orElseThrow().getId();
        DocumentoResponse factura = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, genericoLibreId, "Línea genérica", BigDecimal.ONE, null,
                        new BigDecimal("500.00"), null, null, null, null))
        ));
        UUID lineaId = factura.lineas().get(0).id();

        compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-GEN",
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("400.00"), new BigDecimal("10.00"), null))
        ));

        assertThat(precioListaService.vigente(genericoLibreId)).isEmpty();
    }

    @Test
    void caso5_pesoReal30Con2Unidades60KgYTotalSinCambios() {
        DocumentoResponse factura = crearFactura(new BigDecimal("2"), new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        BigDecimal totalOriginal = factura.total(); // 2600.00

        compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("950.00"), null, new BigDecimal("30.00")))
        ));

        DocumentoResponse doc = documentoService.obtener(factura.id());
        LineaResponse linea = doc.lineas().get(0);
        assertThat(linea.pesoUnitKg()).isEqualByComparingTo("30.00");
        assertThat(linea.pesoTotalKg()).isEqualByComparingTo("60.00");
        assertThat(linea.total()).isEqualByComparingTo(totalOriginal);
        assertThat(doc.totalKg()).isEqualByComparingTo("60.00");
        assertThat(doc.total()).isEqualByComparingTo(totalOriginal);
    }

    @Test
    void caso6_lineaDeOtraFactura400FacturaAnuladaOCotizacion409() {
        DocumentoResponse factura1 = crearFactura(BigDecimal.ONE, new BigDecimal("100.00"));
        DocumentoResponse factura2 = crearFactura(BigDecimal.ONE, new BigDecimal("200.00"));
        UUID lineaFactura2 = factura2.lineas().get(0).id();

        // Línea no pertenece a factura 1 -> 400
        assertThatThrownBy(() -> compraService.registrar(factura1.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(lineaFactura2, new BigDecimal("50.00"), null, null))
        ))).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("La línea no pertenece a esta factura");
                });

        // Cotización -> 409
        DocumentoResponse cotizacion = documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, tubo.id(), null, BigDecimal.ONE, null, new BigDecimal("100.00"), null, null, null, null))
        ));
        UUID lineaCot = cotizacion.lineas().get(0).id();
        assertThatThrownBy(() -> compraService.registrar(cotizacion.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(lineaCot, new BigDecimal("50.00"), null, null))
        ))).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("Solo se registran compras de facturas activas");
                });

        // Factura anulada -> 409
        documentoService.anular(factura1.id());
        UUID lineaFactura1 = factura1.lineas().get(0).id();
        assertThatThrownBy(() -> compraService.registrar(factura1.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(lineaFactura1, new BigDecimal("50.00"), null, null))
        ))).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("Solo se registran compras de facturas activas");
                });
    }

    @Test
    void caso7_editarFacturaConservandoLineaMantieneCompraYCosto_cambiarProductoRecalcula() {
        DocumentoResponse factura = crearFactura(BigDecimal.ONE, new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        CompraResponse compra = compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-700",
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("950.00"), null, null))
        ));

        // 1. Editar la factura conservando la misma línea y producto
        DocumentoResponse actualizada = documentoService.actualizar(factura.id(), new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                "Nota editada",
                false,
                List.of(new LineaRequest(lineaId, tubo.id(), null, new BigDecimal("2"), null, new BigDecimal("1300.00"), null, null, null, null))
        ));

        LineaResponse lineaMant = actualizada.lineas().get(0);
        assertThat(lineaMant.costoUnitConIva()).isEqualByComparingTo("1092.50");
        assertThat(lineaMant.compraId()).isEqualTo(compra.id());

        // 2. Modificar la línea cambiando a otro producto
        DocumentoResponse cambioProducto = documentoService.actualizar(factura.id(), new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                "Nota editada 2",
                false,
                List.of(new LineaRequest(lineaId, otroProducto.id(), null, new BigDecimal("2"), null, new BigDecimal("1300.00"), null, null, null, null))
        ));

        LineaResponse lineaCambio = cambioProducto.lineas().get(0);
        assertThat(lineaCambio.compraId()).isNull();
        // Sin lista previa para otroProducto, costo estimado es null
        assertThat(lineaCambio.costoUnitConIva()).isNull();
    }

    @Test
    void caso8_registrarOtraVezSobreMismaLineaReemplazaCosto() {
        DocumentoResponse factura = crearFactura(BigDecimal.ONE, new BigDecimal("1300.00"));
        UUID lineaId = factura.lineas().get(0).id();

        CompraResponse c1 = compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-1",
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("950.00"), null, null))
        ));

        DocumentoResponse doc1 = documentoService.obtener(factura.id());
        assertThat(doc1.lineas().get(0).costoUnitConIva()).isEqualByComparingTo("1092.50");
        assertThat(doc1.lineas().get(0).compraId()).isEqualTo(c1.id());

        // Segunda compra sobre la misma línea
        CompraResponse c2 = compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA.plusDays(1),
                "F-2",
                null,
                List.of(new CompraRequest.Linea(lineaId, new BigDecimal("800.00"), null, null))
        ));

        DocumentoResponse doc2 = documentoService.obtener(factura.id());
        // 800.00 * 1.15 = 920.00
        assertThat(doc2.lineas().get(0).costoUnitConIva()).isEqualByComparingTo("920.00");
        assertThat(doc2.lineas().get(0).compraId()).isEqualTo(c2.id());

        List<CompraResponse> lista = compraService.listar(factura.id());
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).id()).isEqualTo(c2.id());
        assertThat(lista.get(1).id()).isEqualTo(c1.id());
    }
}
