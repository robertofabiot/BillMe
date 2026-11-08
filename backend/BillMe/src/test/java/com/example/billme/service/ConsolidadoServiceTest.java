package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Abono;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser
class ConsolidadoServiceTest extends IntegrationTest {

    @Autowired
    ConsolidadoService consolidadoService;

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

    @Autowired
    ConfiguracionService configuracionService;

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
    void consolidadoEnVivoRangoFebreroYMarzoConSubtotalesYTotales() {
        Cliente clienteEnt = clienteRepository.findById(cliente.id()).orElseThrow();
        Proyecto obraCentroEnt = proyectoRepository.findById(obraCentro.id()).orElseThrow();

        // Febrero: Saldo al 28 de febrero de 4,000.00
        // Factura 2026-02-10: 5,000.00
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 2, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material Feb", BigDecimal.ONE, null, new BigDecimal("5000.00"), null, null, null, null))
        ));

        // Abono 2026-02-20: 2,000.00
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .monto(new BigDecimal("2000.00"))
                .fechaPago(LocalDate.of(2026, 2, 20))
                .build());

        // Verificamos saldo anterior al 28 de febrero: 1000 + 5000 - 2000 = 4000.00
        BigDecimal saldoAl28Feb = cuentaClienteService.saldoAl(cliente.id(), null, LocalDate.of(2026, 2, 28));
        assertThat(saldoAl28Feb).isEqualByComparingTo("4000.00");

        // Marzo:
        // Factura del 2 de marzo:
        // línea 1: 1,000.00, sin fecha de entrega
        // línea 2: 500.00, entrega el 5 de marzo
        DocumentoResponse fac2Marzo = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 2),
                null,
                false,
                List.of(
                        new LineaRequest(null, producto.id(), "Línea 1", BigDecimal.ONE, null, new BigDecimal("1000.00"), null, null, null, null),
                        new LineaRequest(null, producto.id(), "Línea 2", BigDecimal.ONE, null, new BigDecimal("500.00"), null, null, LocalDate.of(2026, 3, 5), null)
                )
        ));

        // Factura del 5 de marzo: 2,000.00, sin fecha de entrega
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 5),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Línea 5 marzo", BigDecimal.ONE, null, new BigDecimal("2000.00"), null, null, null, null))
        ));

        // Abono del 20 de marzo: 1,500.00
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .monto(new BigDecimal("1500.00"))
                .fechaPago(LocalDate.of(2026, 3, 20))
                .nota("Transferencia BAC")
                .build());

        // 1. Vista previa del 2026-03-01 al 2026-03-31
        ConsolidadoResponse vista = consolidadoService.vistaPrevia(new ConsolidadoRangoRequest(
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        ));

        assertThat(vista.id()).isNull();
        assertThat(vista.folio()).isNull();
        assertThat(vista.grupos()).hasSize(2);

        // Subtotal 1 (2 de marzo) = 1,000.00
        assertThat(vista.grupos().get(0).fechaEntrega()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(vista.grupos().get(0).nombre()).isEqualTo("Subtotal 1 (2 de marzo)");
        assertThat(vista.grupos().get(0).subtotal()).isEqualByComparingTo("1000.00");
        assertThat(vista.grupos().get(0).lineas()).hasSize(1);

        // Subtotal 2 (5 de marzo) = 2,500.00
        assertThat(vista.grupos().get(1).fechaEntrega()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(vista.grupos().get(1).nombre()).isEqualTo("Subtotal 2 (5 de marzo)");
        assertThat(vista.grupos().get(1).subtotal()).isEqualByComparingTo("2500.00");
        assertThat(vista.grupos().get(1).lineas()).hasSize(2);

        // Total 3,500.00; saldo anterior 4,000.00; abonos 1,500.00; total actualizado 6,000.00, igual a saldoAl(31 de marzo)
        assertThat(vista.total()).isEqualByComparingTo("3500.00");
        assertThat(vista.saldoAnterior()).isEqualByComparingTo("4000.00");
        assertThat(vista.totalAbonos()).isEqualByComparingTo("1500.00");
        assertThat(vista.totalActualizado()).isEqualByComparingTo("6000.00");
        assertThat(vista.totalActualizado()).isEqualByComparingTo(cuentaClienteService.saldoAl(cliente.id(), null, LocalDate.of(2026, 3, 31)));

        // 2. Guardar consolidado -> Folio CONS-0001
        ConsolidadoResponse guardado1 = consolidadoService.crear(new ConsolidadoGuardarRequest(
                cliente.id(),
                null,
                "Consolidado Cliente de prueba",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(new ConsolidadoGuardarRequest.NombreGrupo(LocalDate.of(2026, 3, 5), "Entrega 5 de marzo obra"))
        ));
        assertThat(guardado1.folio()).isEqualTo("CONS-0001");
        assertThat(guardado1.grupos().get(1).nombre()).isEqualTo("Entrega 5 de marzo obra");

        // 3. Editar la factura cambia el consolidado sin volver a guardarlo
        // Editamos la factura de 2 de marzo cambiando línea 1 a 1,200.00
        documentoService.actualizar(fac2Marzo.id(), new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 2),
                null,
                false,
                List.of(
                        new LineaRequest(null, producto.id(), "Línea 1 editada", BigDecimal.ONE, null, new BigDecimal("1200.00"), null, null, null, null),
                        new LineaRequest(null, producto.id(), "Línea 2", BigDecimal.ONE, null, new BigDecimal("500.00"), null, null, LocalDate.of(2026, 3, 5), null)
                )
        ));

        ConsolidadoResponse recargado = consolidadoService.obtener(guardado1.id());
        assertThat(recargado.grupos().get(0).subtotal()).isEqualByComparingTo("1200.00");
        assertThat(recargado.total()).isEqualByComparingTo("3700.00");
        assertThat(recargado.totalActualizado()).isEqualByComparingTo("6200.00");

        // 4. Nombre editado se conserva
        assertThat(recargado.grupos().get(1).nombre()).isEqualTo("Entrega 5 de marzo obra");

        // 5. Factura anulada desaparece
        DocumentoResponse facAnular = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 10),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Anulada", BigDecimal.ONE, null, new BigDecimal("800.00"), null, null, null, null))
        ));
        documentoService.anular(facAnular.id());
        ConsolidadoResponse trasAnular = consolidadoService.obtener(guardado1.id());
        assertThat(trasAnular.grupos()).noneMatch(g -> g.fechaEntrega().equals(LocalDate.of(2026, 3, 10)));

        // 6. Registro del histórico no entra; tras convertir sí
        Documento docImport = documentoRepository.findById(facAnular.id()).orElseThrow();
        docImport.setTipo(TipoDocumento.HISTORICO);
        docImport.setEstado(EstadoDocumento.HISTORICA);
        documentoRepository.save(docImport);
        ConsolidadoResponse trasImport = consolidadoService.obtener(guardado1.id());
        assertThat(trasImport.grupos()).noneMatch(g -> g.fechaEntrega().equals(LocalDate.of(2026, 3, 10)));

        documentoService.convertirHistorico(docImport.getId());
        ConsolidadoResponse trasConvertir = consolidadoService.obtener(guardado1.id());
        assertThat(trasConvertir.grupos()).anyMatch(g -> g.fechaEntrega().equals(LocalDate.of(2026, 3, 10)));

        // 7. Filtrar por proyecto no incluye el saldo inicial
        // Creamos factura y abono para Obra centro en marzo
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                obraCentro.id(),
                LocalDate.of(2026, 3, 15),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material Obra Centro", BigDecimal.ONE, null, new BigDecimal("1000.00"), null, null, null, null))
        ));
        abonoRepository.save(Abono.builder()
                .cliente(clienteEnt)
                .proyecto(obraCentroEnt)
                .monto(new BigDecimal("400.00"))
                .fechaPago(LocalDate.of(2026, 3, 16))
                .build());

        ConsolidadoResponse consProyecto = consolidadoService.vistaPrevia(new ConsolidadoRangoRequest(
                cliente.id(),
                obraCentro.id(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        ));
        // saldoAnterior para Obra centro al 28 de feb no incluye saldo inicial (solo facturas - abonos de ese proyecto)
        assertThat(consProyecto.saldoAnterior()).isEqualByComparingTo("0.00"); // en feb la factura de 5000 fue sin proyecto
        assertThat(consProyecto.total()).isEqualByComparingTo("1000.00");
        assertThat(consProyecto.totalAbonos()).isEqualByComparingTo("400.00");
        assertThat(consProyecto.totalActualizado()).isEqualByComparingTo("600.00");

        // 8. Segundo consolidado -> Folio CONS-0002
        ConsolidadoResponse guardado2 = consolidadoService.crear(new ConsolidadoGuardarRequest(
                cliente.id(),
                obraCentro.id(),
                "Consolidado Obra Centro",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                null
        ));
        assertThat(guardado2.folio()).isEqualTo("CONS-0002");
    }

    @Test
    void validacionesRangoYProyecto() {
        ClienteResponse otro = clienteService.crear(new ClienteRequest("María López", null, null));

        // Desde > Hasta -> 400
        assertThatThrownBy(() -> consolidadoService.vistaPrevia(new ConsolidadoRangoRequest(
                cliente.id(),
                null,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 1)
        ))).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La fecha inicial no puede ser posterior a la final");

        // Proyecto de otro cliente -> 400
        assertThatThrownBy(() -> consolidadoService.vistaPrevia(new ConsolidadoRangoRequest(
                otro.id(),
                obraCentro.id(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10)
        ))).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El proyecto no pertenece a ese cliente");
    }

    @Test
    void actualizarConsolidadoRenombrandoGrupoConservaUltimoNombre() {
        LocalDate fechaFactura = LocalDate.of(2026, 3, 5);
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                fechaFactura,
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), "Material prueba grupo", BigDecimal.ONE, null, new BigDecimal("1500.00"), null, null, null, null))
        ));

        // 1. Crear consolidado con grupo renombrado
        ConsolidadoResponse creado = consolidadoService.crear(new ConsolidadoGuardarRequest(
                cliente.id(),
                null,
                "Consolidado Grupo Test",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(new ConsolidadoGuardarRequest.NombreGrupo(fechaFactura, "Nombre Inicial"))
        ));
        assertThat(creado.grupos()).hasSize(1);
        assertThat(creado.grupos().get(0).nombre()).isEqualTo("Nombre Inicial");

        // 2. Actualizar con el mismo nombre
        ConsolidadoResponse actualizado1 = consolidadoService.actualizar(creado.id(), new ConsolidadoGuardarRequest(
                cliente.id(),
                null,
                "Consolidado Grupo Test",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(new ConsolidadoGuardarRequest.NombreGrupo(fechaFactura, "Nombre Inicial"))
        ));
        assertThat(actualizado1.grupos().get(0).nombre()).isEqualTo("Nombre Inicial");

        // 3. Actualizar con otro nombre
        ConsolidadoResponse actualizado2 = consolidadoService.actualizar(creado.id(), new ConsolidadoGuardarRequest(
                cliente.id(),
                null,
                "Consolidado Grupo Test",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                List.of(new ConsolidadoGuardarRequest.NombreGrupo(fechaFactura, "Nombre Final Actualizado"))
        ));
        assertThat(actualizado2.grupos().get(0).nombre()).isEqualTo("Nombre Final Actualizado");

        // 4. Verificar con obtener que tiene el último nombre
        ConsolidadoResponse obtenido = consolidadoService.obtener(creado.id());
        assertThat(obtenido.grupos()).hasSize(1);
        assertThat(obtenido.grupos().get(0).nombre()).isEqualTo("Nombre Final Actualizado");
    }

    @Test
    void consolidadoGuardaTipoCambioYActualizarNoLoCambiaAunqueCambieConfiguracion() {
        ConfiguracionResponse original = configuracionService.obtener();
        try {
            documentoService.crear(new DocumentoRequest(
                    TipoDocumento.FACTURA,
                    cliente.id(),
                    null,
                    LocalDate.of(2026, 3, 5),
                    null,
                    false,
                    List.of(new LineaRequest(null, producto.id(), "Prod 1", BigDecimal.ONE, null, new BigDecimal("3662.43"), null, null, null, null))
            ));

            ConsolidadoResponse creado = consolidadoService.crear(new ConsolidadoGuardarRequest(
                    cliente.id(),
                    null,
                    "Consolidado USD Test",
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    List.of(),
                    true
            ));

            assertThat(creado.mostrarTotalUsd()).isTrue();
            assertThat(creado.tipoCambio()).isEqualByComparingTo("36.624300");
            assertThat(creado.totalUsd()).isEqualByComparingTo("100.00");

            // Cambiar la configuración global
            configuracionService.actualizar(new ConfiguracionRequest(
                    new BigDecimal("15.0"), new BigDecimal("40.0000"), new BigDecimal("1.80"), new BigDecimal("0.1283"), 90
            ));

            // Actualizar el consolidado
            ConsolidadoResponse actualizado = consolidadoService.actualizar(creado.id(), new ConsolidadoGuardarRequest(
                    cliente.id(),
                    null,
                    "Consolidado USD Test Editado",
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    List.of(),
                    true
            ));

            assertThat(actualizado.nombre()).isEqualTo("Consolidado USD Test Editado");
            assertThat(actualizado.tipoCambio()).isEqualByComparingTo("36.624300");
            assertThat(actualizado.totalUsd()).isEqualByComparingTo("100.00");
        } finally {
            configuracionService.actualizar(new ConfiguracionRequest(
                    original.ivaPct(), original.tipoCambio(), original.tarifaKgUsd(), original.densidadKgIn3(), original.diasPrecioViejo()
            ));
        }
    }
}
