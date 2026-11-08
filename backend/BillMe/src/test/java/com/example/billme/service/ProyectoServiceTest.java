package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@WithMockUser
class ProyectoServiceTest extends IntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private DocumentoRepository documentoRepository;

    private ClienteResponse cliente;
    private ProductoResponse producto;

    @BeforeEach
    void setup() {
        cliente = clienteService.crear(new ClienteRequest("Cliente Inmobiliario", null, null));
        producto = productoService.crear(new ProductoRequest("001001", "Acero de refuerzo", null, null, null, List.of()));
    }

    @Test
    void totalFacturadoSumaLasFacturasActivasDelProyectoYNoCuentaCotizacionesNiAnuladas() {
        ProyectoResponse p1 = proyectoService.crear(new ProyectoRequest(cliente.id(), "Torre Norte", null));
        ProyectoResponse p2 = proyectoService.crear(new ProyectoRequest(cliente.id(), "Torre Sur", null));

        // Factura activa 1 para p1 (1000.00)
        documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), p1.id(), FECHA, null, false,
                List.of(linea(producto.id(), "10", "100.00"))));

        // Factura activa 2 para p1 (2500.00)
        documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), p1.id(), FECHA, null, false,
                List.of(linea(producto.id(), "25", "100.00"))));

        // Cotización pendiente para p1 (800.00) -> no debe sumar
        documentoService.crear(new DocumentoRequest(TipoDocumento.COTIZACION, cliente.id(), p1.id(), FECHA, null, false,
                List.of(linea(producto.id(), "8", "100.00"))));

        // Factura anulada para p1 (900.00) -> no debe sumar
        DocumentoResponse facAnulada = documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), p1.id(), FECHA, null, false,
                List.of(linea(producto.id(), "9", "100.00"))));
        documentoService.anular(facAnulada.id());

        // Factura activa para p2 (400.00) -> suma solo a p2
        documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), p2.id(), FECHA, null, false,
                List.of(linea(producto.id(), "4", "100.00"))));

        // Factura sin proyecto (300.00) -> no suma a ningún proyecto
        documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), null, FECHA, null, false,
                List.of(linea(producto.id(), "3", "100.00"))));

        // Verificar obtenerPorId
        ProyectoResponse respP1 = proyectoService.obtenerPorId(p1.id());
        assertThat(respP1.totalFacturado()).isEqualByComparingTo("3500.00");

        ProyectoResponse respP2 = proyectoService.obtenerPorId(p2.id());
        assertThat(respP2.totalFacturado()).isEqualByComparingTo("400.00");

        // Verificar listarTodos
        List<ProyectoResponse> todos = proyectoService.listarTodos();
        ProyectoResponse enTodosP1 = todos.stream().filter(p -> p.id().equals(p1.id())).findFirst().orElseThrow();
        assertThat(enTodosP1.totalFacturado()).isEqualByComparingTo("3500.00");

        // Verificar listarPorCliente
        List<ProyectoResponse> porCliente = proyectoService.listarPorCliente(cliente.id());
        ProyectoResponse enClienteP1 = porCliente.stream().filter(p -> p.id().equals(p1.id())).findFirst().orElseThrow();
        assertThat(enClienteP1.totalFacturado()).isEqualByComparingTo("3500.00");

        // Verificar listarActivosPorCliente
        List<ProyectoResponse> activos = proyectoService.listarActivosPorCliente(cliente.id());
        ProyectoResponse enActivosP1 = activos.stream().filter(p -> p.id().equals(p1.id())).findFirst().orElseThrow();
        assertThat(enActivosP1.totalFacturado()).isEqualByComparingTo("3500.00");

        // Verificar actualizar
        ProyectoResponse actualizado = proyectoService.actualizar(p1.id(), new ProyectoRequest(cliente.id(), "Torre Norte Renombrada", "ACTIVO"));
        assertThat(actualizado.totalFacturado()).isEqualByComparingTo("3500.00");
    }

    @Test
    void proyectoSinFacturasTieneTotalFacturadoCero() {
        ProyectoResponse nuevo = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra Nueva", null));
        assertThat(nuevo.totalFacturado()).isEqualByComparingTo("0.00");

        ProyectoResponse obtenido = proyectoService.obtenerPorId(nuevo.id());
        assertThat(obtenido.totalFacturado()).isEqualByComparingTo("0.00");
    }

    @Test
    void cotizacionConvertidaAFacturaSumaAlTotalFacturado() {
        ProyectoResponse p = proyectoService.crear(new ProyectoRequest(cliente.id(), "Proyecto Fase 1", null));

        DocumentoResponse cot = documentoService.crear(new DocumentoRequest(TipoDocumento.COTIZACION, cliente.id(), p.id(), FECHA, null, false,
                List.of(linea(producto.id(), "5", "200.00"))));

        assertThat(proyectoService.obtenerPorId(p.id()).totalFacturado()).isEqualByComparingTo("0.00");

        documentoService.convertirAFactura(cot.id(), FECHA);

        assertThat(proyectoService.obtenerPorId(p.id()).totalFacturado()).isEqualByComparingTo("1000.00");
    }

    @Test
    void actualizarProyectoConFacturaAOtroClienteLanza409YFacturaSigueEditable() {
        ClienteResponse otroCliente = clienteService.crear(new ClienteRequest("Otro Cliente", null, null));
        ProyectoResponse p = proyectoService.crear(new ProyectoRequest(cliente.id(), "Proyecto Con Factura", null));
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), p.id(), FECHA, null, false,
                List.of(linea(producto.id(), "5", "100.00"))));

        // Intento de mover el proyecto a otro cliente -> lanza 409 Conflict con mensaje exacto
        assertThatThrownBy(() -> proyectoService.actualizar(p.id(), new ProyectoRequest(otroCliente.id(), "Proyecto Con Factura", "ACTIVO")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("Este proyecto tiene movimientos: no se puede pasar a otro cliente");
                });

        // Comprobar que la factura sigue siendo editable
        DocumentoResponse facEditada = documentoService.actualizar(fac.id(), new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), p.id(), FECHA, "Factura editada correctamente", false,
                List.of(linea(producto.id(), "6", "100.00"))));
        assertThat(facEditada.notas()).isEqualTo("Factura editada correctamente");
        assertThat(facEditada.total()).isEqualByComparingTo("600.00");
    }

    @Test
    void historicoEnProyectoNoSumaAntesDeConvertirYDespuesSi() {
        ProyectoResponse p = proyectoService.crear(new ProyectoRequest(cliente.id(), "Proyecto Con Histórico", null));
        DocumentoResponse doc = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), p.id(), FECHA, null, false,
                List.of(linea(producto.id(), "10", "100.00"))));

        var docEnt = documentoRepository.findById(doc.id()).orElseThrow();
        docEnt.setTipo(TipoDocumento.HISTORICO);
        docEnt.setEstado(EstadoDocumento.HISTORICA);
        documentoRepository.save(docEnt);

        assertThat(proyectoService.obtenerPorId(p.id()).totalFacturado()).isEqualByComparingTo("0.00");

        documentoService.convertirHistorico(docEnt.getId());

        assertThat(proyectoService.obtenerPorId(p.id()).totalFacturado()).isEqualByComparingTo("1000.00");
    }

    private static LineaRequest linea(java.util.UUID productoId, String cantidad, String precio) {
        return new LineaRequest(null, productoId, null, new BigDecimal(cantidad), null, new BigDecimal(precio),
                null, null, null, null);
    }
}
