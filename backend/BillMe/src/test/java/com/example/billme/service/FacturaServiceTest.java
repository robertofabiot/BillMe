package com.example.billme.service;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.DetalleFacturaRequest;
import com.example.billme.dto.FacturaRequest;
import com.example.billme.dto.FacturaResponse;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.dto.ProyectoRequest;
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

/**
 * Flujos de venta con open-in-view desactivado: las respuestas deben armarse
 * completas dentro de las transacciones de los servicios.
 */
class FacturaServiceTest extends IntegrationTest {

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    ProductoService productoService;

    @Autowired
    FacturaService facturaService;

    @Autowired
    AbonoService abonoService;

    @Autowired
    ReporteService reporteService;

    @Test
    void listarIncluyeDetallesProyectoYAbonos() {
        FacturaResponse factura = facturaConUnAbono();

        List<FacturaResponse> facturas = facturaService.listarTodas();

        assertThat(facturas).hasSize(1);
        FacturaResponse listada = facturas.get(0);
        assertThat(listada.id()).isEqualTo(factura.id());
        assertThat(listada.proyectoNombre()).isEqualTo("Obra centro");
        assertThat(listada.detalles()).hasSize(1);
        assertThat(listada.totalVenta()).isEqualByComparingTo("3000");
        assertThat(listada.totalAbonado()).isEqualByComparingTo("1000");
        assertThat(listada.saldoPendiente()).isEqualByComparingTo("2000");
        assertThat(listada.estado()).isEqualTo("PAGO_PARCIAL");
    }

    @Test
    void filtraLasFacturasQueIncluyenUnProducto() {
        facturaConUnAbono();
        var otro = productoService.crear(new ProductoRequest("018001001", "Varilla Lisa 3/8", null, null, null, List.of()));

        assertThat(facturaService.filtrar(null, null, null, null, null, tubo().id())).hasSize(1);
        assertThat(facturaService.filtrar(null, null, null, null, null, otro.id())).isEmpty();
    }

    @Test
    void reporteSumaLoCobrado() {
        facturaConUnAbono();

        var reporte = reporteService.generarReporte(null, null, null, null);

        assertThat(reporte.ingresosTotales()).isEqualByComparingTo("3000");
        assertThat(reporte.totalCobrado()).isEqualByComparingTo("1000");
        assertThat(reporte.porCobrar()).isEqualByComparingTo("2000");
    }

    @Test
    void actualizarDetallesRecalculaElTotal() {
        FacturaResponse factura = facturaConUnAbono();
        ProductoResponse producto = tubo();

        facturaService.actualizar(factura.id(), new FacturaRequest(null, null, null,
                List.of(new DetalleFacturaRequest(producto.id(), new BigDecimal("3"), new BigDecimal("1500")))));

        assertThat(facturaService.obtenerPorId(factura.id()).totalVenta()).isEqualByComparingTo("4500");
    }

    @Test
    void noSePuedeBorrarUnProductoUsadoEnFacturas() {
        facturaConUnAbono();
        ProductoResponse producto = tubo();

        assertThatThrownBy(() -> productoService.eliminar(producto.id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(productoService.obtenerPorId(producto.id())).isNotNull();
    }

    @Test
    void crearFacturaSinClienteResponde400() {
        assertThatThrownBy(() -> facturaService.crear(new FacturaRequest(null, null, null, List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private ProductoResponse tubo() {
        return productoService.buscar("009001271").get(0);
    }

    private FacturaResponse facturaConUnAbono() {
        var cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        var proyecto = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra centro", null));
        var producto = productoService.crear(new ProductoRequest(
                "009001271", "Tubo Estruct. Cua. 4 x 4", null, "UND", new BigDecimal("44.17"),
                List.of("Caja de 4 x 4")));
        FacturaResponse factura = facturaService.crear(new FacturaRequest(cliente.id(), proyecto.id(), "PENDIENTE",
                List.of(new DetalleFacturaRequest(producto.id(), new BigDecimal("2"), new BigDecimal("1500")))));
        abonoService.registrarAbono(factura.id(), new AbonoRequest(new BigDecimal("1000"), LocalDate.now()));
        return factura;
    }
}
