package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.ProveedorRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser
class ReporteServiceTest extends IntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    ReporteService reporteService;

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    ProductoService productoService;

    @Autowired
    CompraService compraService;

    @Autowired
    ProveedorRepository proveedorRepository;

    @Autowired
    DocumentoRepository documentoRepository;

    ClienteResponse cliente;
    ProductoResponse productoConCosto;
    ProductoResponse productoSinCosto;
    Proveedor proveedor;

    @BeforeEach
    void setup() {
        cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        productoConCosto = productoService.crear(new ProductoRequest("009001271", "Tubo Estruct. Cua. 4 x 4", null, null,
                new BigDecimal("44.17"), List.of()));
        productoSinCosto = productoService.crear(new ProductoRequest("009001272", "Placa de anclaje", null, null,
                new BigDecimal("5.00"), List.of()));
        proveedor = proveedorRepository.findByNombre("Proveedor principal").orElseThrow();
    }

    @Test
    void reporteGananciasCalculoExacto() {
        // Línea 1: 2 unidades a PU 1,300.00
        // Línea 2: 1 unidad a PU 500.00
        DocumentoResponse factura = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(
                        new LineaRequest(null, productoConCosto.id(), null, new BigDecimal("2"), null,
                                new BigDecimal("1300.00"), null, null, null, null),
                        new LineaRequest(null, productoSinCosto.id(), null, BigDecimal.ONE, null,
                                new BigDecimal("500.00"), null, null, null, null)
                )
        ));

        UUID linea1Id = factura.lineas().get(0).id();

        // Registrar compra para línea 1: precio sin IVA 950.00 -> costo con IVA 1,092.50
        compraService.registrar(factura.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                "F-COMPRA-1",
                null,
                List.of(new CompraRequest.Linea(linea1Id, new BigDecimal("950.00"), null, null))
        ));

        // Ejecutar reporte
        GananciasResponse reporte = reporteService.ganancias(null, null, null, null);

        // Facturado 3,100.00; ventas con costo 2,600.00; costo 2,185.00; ganancia 415.00 (207.50 / u)
        assertThat(reporte.facturado()).isEqualByComparingTo("3100.00");
        assertThat(reporte.ventasConCosto()).isEqualByComparingTo("2600.00");
        assertThat(reporte.costo()).isEqualByComparingTo("2185.00");
        assertThat(reporte.ganancia()).isEqualByComparingTo("415.00");
        assertThat(reporte.lineasSinCosto()).isEqualTo(1);
        assertThat(reporte.ventasSinCosto()).isEqualByComparingTo("500.00");

        assertThat(reporte.facturas()).hasSize(1);
        GananciasResponse.FilaGanancia fila = reporte.facturas().get(0);
        assertThat(fila.id()).isEqualTo(factura.id());
        assertThat(fila.folio()).isEqualTo(factura.folio());
        assertThat(fila.fecha()).isEqualTo(FECHA);
        assertThat(fila.clienteNombre()).isEqualTo("Cliente de prueba");
        assertThat(fila.total()).isEqualByComparingTo("3100.00");
        assertThat(fila.costo()).isEqualByComparingTo("2185.00");
        assertThat(fila.ganancia()).isEqualByComparingTo("415.00");
        assertThat(fila.lineasSinCosto()).isEqualTo(1);
    }

    @Test
    void cotizacionesYFacturasAnuladasNoCuentan() {
        // Factura activa con venta y costo
        DocumentoResponse activa = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("1000.00"), null, null, null, null))
        ));
        compraService.registrar(activa.id(), new CompraRequest(
                proveedor.getId(),
                FECHA,
                null,
                null,
                List.of(new CompraRequest.Linea(activa.lineas().get(0).id(), new BigDecimal("800.00"), null, null))
        ));

        // Cotización
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("2000.00"), null, null, null, null))
        ));

        // Factura anulada
        DocumentoResponse paraAnular = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                FECHA,
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("3000.00"), null, null, null, null))
        ));
        documentoService.anular(paraAnular.id());

        GananciasResponse reporte = reporteService.ganancias(null, null, null, null);
        assertThat(reporte.facturas()).hasSize(1);
        assertThat(reporte.facturas().get(0).id()).isEqualTo(activa.id());
        assertThat(reporte.facturado()).isEqualByComparingTo("1000.00");
    }

    @Test
    void filtrosDeClienteProyectoYFechaFuncionan() {
        ClienteResponse cliente2 = clienteService.crear(new ClienteRequest("Cliente dos", null, null));
        ProyectoResponse proyecto1 = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra Norte", null));
        ProyectoResponse proyecto2 = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra Sur", null));

        // Factura 1: cliente 1, proyecto 1, fecha 2026-10-10
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                proyecto1.id(),
                LocalDate.of(2026, 10, 10),
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("100.00"), null, null, null, null))
        ));

        // Factura 2: cliente 1, proyecto 2, fecha 2026-10-20
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                proyecto2.id(),
                LocalDate.of(2026, 10, 20),
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("200.00"), null, null, null, null))
        ));

        // Factura 3: cliente 2, sin proyecto, fecha 2026-10-25
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente2.id(),
                null,
                LocalDate.of(2026, 10, 25),
                null,
                false,
                List.of(new LineaRequest(null, productoConCosto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("300.00"), null, null, null, null))
        ));

        // Filtro por cliente
        GananciasResponse repCliente2 = reporteService.ganancias(cliente2.id(), null, null, null);
        assertThat(repCliente2.facturas()).hasSize(1);
        assertThat(repCliente2.facturado()).isEqualByComparingTo("300.00");

        // Filtro por proyecto
        GananciasResponse repProyecto1 = reporteService.ganancias(null, proyecto1.id(), null, null);
        assertThat(repProyecto1.facturas()).hasSize(1);
        assertThat(repProyecto1.facturado()).isEqualByComparingTo("100.00");

        // Filtro por fecha desde / hasta
        GananciasResponse repFecha = reporteService.ganancias(null, null,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 22));
        assertThat(repFecha.facturas()).hasSize(1);
        assertThat(repFecha.facturado()).isEqualByComparingTo("200.00");
    }
}
