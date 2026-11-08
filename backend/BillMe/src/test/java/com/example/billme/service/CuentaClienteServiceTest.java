package com.example.billme.service;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.dto.CuentaClienteResponse;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.ProyectoResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Abono;
import com.example.billme.model.Cliente;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.ProyectoRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser
class CuentaClienteServiceTest extends IntegrationTest {

    @Autowired
    CuentaClienteService cuentaClienteService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ClienteRepository clienteRepository;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    ProyectoRepository proyectoRepository;

    @Autowired
    ProductoService productoService;

    @Autowired
    DocumentoService documentoService;

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    AbonoRepository abonoRepository;

    private ClienteResponse cliente;
    private ProyectoResponse obraCentro;
    private ProductoResponse producto;

    @BeforeEach
    void setup() {
        cliente = clienteService.crear(new ClienteRequest(
                "Cliente de prueba",
                "88888888",
                "Notas",
                BigDecimal.ZERO,
                null,
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 2, 1)
        ));

        obraCentro = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra centro", null));
        producto = productoService.crear(new ProductoRequest("001", "Material", null, "UND", null, List.of()));
    }

    @Test
    void cuentaConSaldoInicialFacturasAbonosYPorProyecto() {
        Cliente clienteEnt = clienteRepository.findById(cliente.id()).orElseThrow();
        Proyecto obraCentroEnt = proyectoRepository.findById(obraCentro.id()).orElseThrow();

        // 2026-02-10: Factura 5,000.00 (Obra centro)
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                obraCentro.id(),
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("5000.00"), null, null, null, null))
        ));

        // 2026-02-15: Factura 3,000.00 (Sin proyecto)
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 15),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("3000.00"), null, null, null, null))
        ));

        // 2026-02-20: Abono 2,000.00 (Obra centro)
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .proyecto(obraCentroEnt)
                .monto(new BigDecimal("2000.00"))
                .fechaPago(LocalDate.of(2026, 2, 20))
                .build());

        // 2026-02-25: Abono 1,000.00 (Sin proyecto)
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .monto(new BigDecimal("1000.00"))
                .fechaPago(LocalDate.of(2026, 2, 25))
                .build());

        CuentaClienteResponse cuenta = cuentaClienteService.cuenta(cliente.id());

        // Comprobaciones de saldos globales
        assertThat(cuenta.totalFacturado()).isEqualByComparingTo("8000.00");
        assertThat(cuenta.totalAbonado()).isEqualByComparingTo("3000.00");
        assertThat(cuenta.saldo()).isEqualByComparingTo("6000.00");

        // Comprobaciones por proyecto
        var saldoObra = cuenta.proyectos().stream()
                .filter(p -> obraCentro.id().equals(p.proyectoId()))
                .findFirst().orElseThrow();
        assertThat(saldoObra.facturado()).isEqualByComparingTo("5000.00");
        assertThat(saldoObra.abonado()).isEqualByComparingTo("2000.00");
        assertThat(saldoObra.saldo()).isEqualByComparingTo("3000.00");

        var saldoSinProyecto = cuenta.proyectos().stream()
                .filter(p -> p.proyectoId() == null)
                .findFirst().orElseThrow();
        assertThat(saldoSinProyecto.facturado()).isEqualByComparingTo("3000.00");
        assertThat(saldoSinProyecto.abonado()).isEqualByComparingTo("1000.00");
        assertThat(saldoSinProyecto.saldo()).isEqualByComparingTo("3000.00"); // 1000 saldo_inicial + 3000 - 1000 = 3000

        // Movimientos y saldo corrido
        List<CuentaClienteResponse.Movimiento> movs = cuenta.movimientos();
        assertThat(movs).hasSize(5);

        assertThat(movs.get(0).tipo()).isEqualTo("SALDO_INICIAL");
        assertThat(movs.get(0).saldo()).isEqualByComparingTo("1000.00");

        assertThat(movs.get(1).tipo()).isEqualTo("FACTURA");
        assertThat(movs.get(1).fecha()).isEqualTo(LocalDate.of(2026, 2, 10));
        assertThat(movs.get(1).cargo()).isEqualByComparingTo("5000.00");
        assertThat(movs.get(1).saldo()).isEqualByComparingTo("6000.00");

        assertThat(movs.get(2).tipo()).isEqualTo("FACTURA");
        assertThat(movs.get(2).fecha()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(movs.get(2).cargo()).isEqualByComparingTo("3000.00");
        assertThat(movs.get(2).saldo()).isEqualByComparingTo("9000.00");

        assertThat(movs.get(3).tipo()).isEqualTo("ABONO");
        assertThat(movs.get(3).fecha()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(movs.get(3).abono()).isEqualByComparingTo("2000.00");
        assertThat(movs.get(3).saldo()).isEqualByComparingTo("7000.00");

        assertThat(movs.get(4).tipo()).isEqualTo("ABONO");
        assertThat(movs.get(4).fecha()).isEqualTo(LocalDate.of(2026, 2, 25));
        assertThat(movs.get(4).abono()).isEqualByComparingTo("1000.00");
        assertThat(movs.get(4).saldo()).isEqualByComparingTo("6000.00");

        // saldoAl(2026-02-14) es 6,000.00
        assertThat(cuentaClienteService.saldoAl(cliente.id(), null, LocalDate.of(2026, 2, 14)))
                .isEqualByComparingTo("6000.00");

        // saldos() coincide con cuenta()
        Map<UUID, BigDecimal> saldos = cuentaClienteService.saldos();
        assertThat(saldos.get(cliente.id())).isEqualByComparingTo(cuenta.saldo());
    }

    @Test
    void abonoQueSuperaDeudaDejaSaldoAFavor() {
        Cliente clienteEnt = clienteRepository.findById(cliente.id()).orElseThrow();
        // Factura de 5,000
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("5000.00"), null, null, null, null))
        ));

        // Abono de 7,000.00 el 2026-03-01: saldo_inicial (1000) + facturas (5000) - abonos (7000) = -1000
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .monto(new BigDecimal("7000.00"))
                .fechaPago(LocalDate.of(2026, 3, 1))
                .build());

        CuentaClienteResponse cuenta = cuentaClienteService.cuenta(cliente.id());
        assertThat(cuenta.saldo()).isEqualByComparingTo("-1000.00");
    }

    @Test
    void noCuentanFacturaAnuladaCotizacionFacturaAnteriorNiFacturaImportada() {
        Cliente clienteEnt = clienteRepository.findById(cliente.id()).orElseThrow();

        // Cotización
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("500.00"), null, null, null, null))
        ));

        // Factura anulada
        DocumentoResponse facAnulada = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("600.00"), null, null, null, null))
        ));
        documentoService.anular(facAnulada.id());

        // Factura anterior a fecha_inicio_cuenta (2026-01-15 < 2026-02-01)
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 1, 15),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("700.00"), null, null, null, null))
        ));

        // Registro del histórico (no cuenta en saldo)
        DocumentoResponse facImportada = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material", BigDecimal.ONE, null, new BigDecimal("800.00"), null, null, null, null))
        ));
        var docEnt = documentoRepository.findById(facImportada.id()).orElseThrow();
        docEnt.setTipo(TipoDocumento.HISTORICO);
        docEnt.setEstado(EstadoDocumento.HISTORICA);
        documentoRepository.save(docEnt);

        // Abono anterior a fecha_inicio_cuenta (2026-01-20)
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .monto(new BigDecimal("150.00"))
                .fechaPago(LocalDate.of(2026, 1, 20))
                .build());

        CuentaClienteResponse cuenta = cuentaClienteService.cuenta(cliente.id());

        // Ninguno debe haber contado: solo queda el saldo inicial de 1,000.00
        assertThat(cuenta.totalFacturado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.totalAbonado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.saldo()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.movimientos()).hasSize(1);
        assertThat(cuenta.movimientos().get(0).tipo()).isEqualTo("SALDO_INICIAL");

        // Al convertir el histórico a factura activa, sí entra al saldo
        documentoService.convertirHistorico(docEnt.getId());
        CuentaClienteResponse cuentaTrasConvertir = cuentaClienteService.cuenta(cliente.id());
        assertThat(cuentaTrasConvertir.totalFacturado()).isEqualByComparingTo("800.00");
        assertThat(cuentaTrasConvertir.saldo()).isEqualByComparingTo("1800.00");
    }
}
