package com.example.billme.service;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.LineaResponse;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.RevisionResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.MetodoPrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Usuario;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.UsuarioRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class DocumentoServiceTest extends IntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    ProductoService productoService;

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    AbonoService abonoService;

    @Autowired
    ReporteService reporteService;

    @Autowired
    UsuarioRepository usuarioRepository;

    ClienteResponse cliente;
    ProductoResponse tubo;

    @BeforeEach
    void datosBase() {
        cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        tubo = productoService.crear(new ProductoRequest("009001271", "Tubo Estruct. Cua. 4 x 4", null, null,
                new BigDecimal("44.17"), List.of()));
    }

    // ── Series y cálculo ─────────────────────────────────────────────────────

    @Test
    void cadaTipoTieneSuPropiaSerieDeFolios() {
        assertThat(crear(TipoDocumento.COTIZACION).folio()).isEqualTo("COT-0001");
        assertThat(crear(TipoDocumento.FACTURA).folio()).isEqualTo("FAC-0001");
        assertThat(crear(TipoDocumento.COTIZACION).folio()).isEqualTo("COT-0002");
        assertThat(crear(TipoDocumento.FACTURA).folio()).isEqualTo("FAC-0002");
    }

    @Test
    void elBackendRecalculaPreciosPesosYTotales() {
        DocumentoResponse doc = documentoService.crear(request(TipoDocumento.FACTURA, List.of(
                new LineaRequest(null, tubo.id(), null, new BigDecimal("10"), new BigDecimal("2.584"),
                        new BigDecimal("195.5957"), MetodoPrecio.POR_PESO, null, null, null),
                linea(tubo.id(), "2", "100"))));

        LineaResponse primera = doc.lineas().get(0);
        assertThat(primera.precioUnit()).isEqualByComparingTo("195.60");
        assertThat(primera.total()).isEqualByComparingTo("1956.00");
        assertThat(primera.pesoUnitKg()).isEqualByComparingTo("2.58");
        assertThat(primera.pesoTotalKg()).isEqualByComparingTo("25.80");
        assertThat(primera.metodoPrecio()).isEqualTo("POR_PESO");
        assertThat(doc.total()).isEqualByComparingTo("2156.00");
        // 25.80 + 2 × 44.17 del catálogo
        assertThat(doc.totalKg()).isEqualByComparingTo("114.14");
    }

    @Test
    void usaElNombreYElPesoDelCatalogoSiNoSeIndican() {
        LineaResponse linea = crear(TipoDocumento.COTIZACION).lineas().get(0);

        assertThat(linea.descripcion()).isEqualTo("Tubo Estruct. Cua. 4 x 4");
        assertThat(linea.pesoUnitKg()).isEqualByComparingTo("44.17");
    }

    @Test
    void unaLineaGenericaLlevaSuPropiaDescripcion() {
        UUID libre = productoRepository.findByCodigoInterno("GEN-LIBRE").orElseThrow().getId();

        DocumentoResponse doc = documentoService.crear(request(TipoDocumento.FACTURA, List.of(
                new LineaRequest(null, libre, "Disco de corte de 7 pulgadas", new BigDecimal("6"), null,
                        new BigDecimal("100"), null, null, null, null))));

        assertThat(doc.lineas().get(0).descripcion()).isEqualTo("Disco de corte de 7 pulgadas");
        assertThat(doc.lineas().get(0).productoGenerico()).isTrue();
        assertThat(doc.lineas().get(0).pesoTotalKg()).isNull();
        assertThat(doc.totalKg()).isEqualByComparingTo("0");

        assertEstado(HttpStatus.BAD_REQUEST, () -> documentoService.crear(request(TipoDocumento.FACTURA,
                List.of(linea(libre, "1", "100")))));
    }

    @Test
    void conservaFechaYLugarDeEntregaPorLinea() {
        DocumentoResponse doc = documentoService.crear(request(TipoDocumento.FACTURA, List.of(
                new LineaRequest(null, tubo.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null,
                        LocalDate.of(2026, 10, 20), "  Obra centro  "))));

        assertThat(doc.lineas().get(0).fechaEntrega()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(doc.lineas().get(0).lugarEntrega()).isEqualTo("Obra centro");
    }

    // ── Validaciones ─────────────────────────────────────────────────────────

    @Test
    void elProyectoDebeSerDelMismoCliente() {
        var otro = clienteService.crear(new ClienteRequest("Otro cliente", null, null));
        var ajeno = proyectoService.crear(new ProyectoRequest(otro.id(), "Obra ajena", null));

        assertEstado(HttpStatus.BAD_REQUEST, () -> documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA,
                cliente.id(), ajeno.id(), FECHA, null, false, List.of(linea(tubo.id(), "1", "100")))));
    }

    @Test
    void crearSinTipoResponde400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> documentoService.crear(request(null, List.of(linea(tubo.id(), "1", "100")))));
    }

    @Test
    void laApiExigeAlMenosUnaLinea() throws Exception {
        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "FACTURA", "clienteId": "%s", "fecha": "2026-10-15", "lineas": []}"""
                                .formatted(cliente.id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("lineas"));
    }

    // ── Conversión y estados ─────────────────────────────────────────────────

    @Test
    void convertirCreaLaFacturaYMarcaLaCotizacionComoAceptada() {
        DocumentoResponse cotizacion = documentoService.crear(new DocumentoRequest(TipoDocumento.COTIZACION,
                cliente.id(), null, FECHA, "Entrega en obra", true, List.of(
                new LineaRequest(null, tubo.id(), "Tubo cuadrado", new BigDecimal("3"), null, new BigDecimal("500"),
                        MetodoPrecio.ULTIMO_PRECIO, null, LocalDate.of(2026, 10, 20), "Bodega"))));

        DocumentoResponse factura = documentoService.convertirAFactura(cotizacion.id(), LocalDate.of(2026, 10, 18));

        assertThat(factura.tipo()).isEqualTo("FACTURA");
        assertThat(factura.folio()).isEqualTo("FAC-0001");
        assertThat(factura.estado()).isEqualTo("ACTIVA");
        assertThat(factura.fecha()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(factura.total()).isEqualByComparingTo("1500.00");
        assertThat(factura.notas()).isEqualTo("Entrega en obra");
        assertThat(factura.cotizacionOrigenFolio()).isEqualTo("COT-0001");
        LineaResponse linea = factura.lineas().get(0);
        assertThat(linea.descripcion()).isEqualTo("Tubo cuadrado");
        assertThat(linea.metodoPrecio()).isEqualTo("ULTIMO_PRECIO");
        assertThat(linea.lugarEntrega()).isEqualTo("Bodega");

        DocumentoResponse aceptada = documentoService.obtener(cotizacion.id());
        assertThat(aceptada.estado()).isEqualTo("ACEPTADA");
        assertThat(aceptada.facturaGeneradaFolio()).isEqualTo("FAC-0001");

        assertEstado(HttpStatus.CONFLICT, () -> documentoService.convertirAFactura(cotizacion.id(), null));
        assertEstado(HttpStatus.CONFLICT, () -> documentoService.actualizar(cotizacion.id(), requestDe(cotizacion)));
    }

    @Test
    void unaCotizacionSePuedeRechazarYReabrir() {
        DocumentoResponse cotizacion = crear(TipoDocumento.COTIZACION);

        assertThat(documentoService.cambiarEstadoCotizacion(cotizacion.id(), EstadoDocumento.RECHAZADA).estado())
                .isEqualTo("RECHAZADA");
        assertEstado(HttpStatus.CONFLICT, () -> documentoService.convertirAFactura(cotizacion.id(), null));

        assertThat(documentoService.cambiarEstadoCotizacion(cotizacion.id(), EstadoDocumento.PENDIENTE).estado())
                .isEqualTo("PENDIENTE");
        assertThat(documentoService.convertirAFactura(cotizacion.id(), FECHA).tipo()).isEqualTo("FACTURA");
    }

    @Test
    void laAceptacionSoloOcurreAlConvertir() {
        DocumentoResponse cotizacion = crear(TipoDocumento.COTIZACION);
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);

        assertEstado(HttpStatus.CONFLICT, () -> documentoService.cambiarEstadoCotizacion(cotizacion.id(), EstadoDocumento.ACEPTADA));
        assertEstado(HttpStatus.CONFLICT, () -> documentoService.cambiarEstadoCotizacion(factura.id(), EstadoDocumento.RECHAZADA));
        assertEstado(HttpStatus.CONFLICT, () -> documentoService.convertirAFactura(factura.id(), null));
    }

    @Test
    void unaFacturaAnuladaNoSeEditaNiSuma() {
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        DocumentoResponse otra = crear(TipoDocumento.FACTURA);

        assertThat(documentoService.anular(factura.id()).estado()).isEqualTo("ANULADA");

        assertEstado(HttpStatus.CONFLICT, () -> documentoService.anular(factura.id()));
        assertEstado(HttpStatus.CONFLICT, () -> documentoService.actualizar(factura.id(), requestDe(factura)));
        assertThat(reporteService.generarReporte(null, null, null, null).ingresosTotales())
                .isEqualByComparingTo(otra.total());
    }

    @Test
    void noSeAnulaUnaFacturaConAbonos() {
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        abonoService.registrarAbono(factura.id(), new AbonoRequest(new BigDecimal("100"), FECHA));

        assertEstado(HttpStatus.CONFLICT, () -> documentoService.anular(factura.id()));
    }

    // ── Edición ──────────────────────────────────────────────────────────────

    @Test
    void editarRecalculaYCambiaLasLineas() {
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        ProductoResponse varilla = productoService.crear(new ProductoRequest("018001001", "Varilla Lisa 3/8", null, null, null, List.of()));
        LineaResponse existente = factura.lineas().get(0);

        DocumentoResponse editada = documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(), null,
                LocalDate.of(2026, 11, 1), "Corregida", false, List.of(
                linea(varilla.id(), "4", "50"),
                new LineaRequest(existente.id(), tubo.id(), "Tubo corregido", new BigDecimal("3"), null,
                        new BigDecimal("700"), null, null, null, null))));

        assertThat(editada.fecha()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(editada.notas()).isEqualTo("Corregida");
        assertThat(editada.tipo()).isEqualTo("FACTURA");
        assertThat(editada.folio()).isEqualTo(factura.folio());
        assertThat(editada.total()).isEqualByComparingTo("2300.00");
        assertThat(editada.lineas()).extracting(LineaResponse::descripcion).containsExactly("Varilla Lisa 3/8", "Tubo corregido");
        assertThat(editada.lineas()).extracting(LineaResponse::orden).containsExactly(0, 1);
        assertThat(editada.lineas().get(1).id()).isEqualTo(existente.id());
        assertThat(documentoService.obtener(factura.id()).lineas()).hasSize(2);
    }

    @Test
    void unaLineaConservadaMantieneElCostoConElQueSeVendio() {
        jdbc.update("insert into precios_lista (id, created_at, updated_at, producto_id, precio_sin_iva, fecha, fuente, descuento_pct) "
                + "values (gen_random_uuid(), now(), now(), ?, 1000, '2026-01-01', 'FACTURA_PROVEEDOR', 20)", tubo.id());
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        LineaResponse linea = factura.lineas().get(0);
        assertThat(linea.costoUnitConIva()).isEqualByComparingTo("920.00");

        // La lista sube después de la venta
        jdbc.update("insert into precios_lista (id, created_at, updated_at, producto_id, precio_sin_iva, fecha, fuente, descuento_pct) "
                + "values (gen_random_uuid(), now(), now(), ?, 2000, '2026-02-01', 'FACTURA_PROVEEDOR', 20)", tubo.id());

        DocumentoResponse editada = documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(), null, FECHA,
                null, false, List.of(new LineaRequest(linea.id(), tubo.id(), null, new BigDecimal("9"), null,
                new BigDecimal("1300"), null, null, null, null), linea(tubo.id(), "1", "1300"))));

        assertThat(editada.lineas().get(0).costoUnitConIva()).isEqualByComparingTo("920.00");
        assertThat(editada.lineas().get(1).costoUnitConIva()).isEqualByComparingTo("1840.00");
    }

    @Test
    void noAceptaLineasDeOtroDocumento() {
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        DocumentoResponse otra = crear(TipoDocumento.FACTURA);

        assertEstado(HttpStatus.BAD_REQUEST, () -> documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(),
                null, FECHA, null, false, List.of(new LineaRequest(otra.lineas().get(0).id(), tubo.id(), null,
                BigDecimal.ONE, null, BigDecimal.TEN, null, null, null, null)))));
    }

    // ── Versiones anteriores ─────────────────────────────────────────────────

    @Test
    void editarGuardaCopiaDeComoEstabaYQuienLoCambio() {
        usuarioRepository.save(Usuario.builder().username("user").nombre("Juan Pérez").password("x").build());
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);

        documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(), null, FECHA, "Primera corrección",
                false, List.of(linea(tubo.id(), "2", "150"))));
        documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(), null, FECHA, "Segunda corrección",
                false, List.of(linea(tubo.id(), "3", "200"))));

        List<RevisionResponse> revisiones = documentoService.revisiones(factura.id());
        assertThat(revisiones).extracting(RevisionResponse::numero).containsExactly(2, 1);
        RevisionResponse primera = revisiones.get(1);
        assertThat(primera.editadoPor()).isEqualTo("Juan Pérez");
        assertThat(primera.editadoEn()).isNotNull();
        assertThat(primera.documento().total()).isEqualByComparingTo("100.00");
        assertThat(primera.documento().notas()).isNull();
        assertThat(primera.documento().fecha()).isEqualTo(FECHA);
        assertThat(primera.documento().lineas().get(0).descripcion()).isEqualTo("Tubo Estruct. Cua. 4 x 4");
        assertThat(revisiones.get(0).documento().notas()).isEqualTo("Primera corrección");
        assertThat(revisiones.get(0).documento().total()).isEqualByComparingTo("300.00");
        assertThat(documentoService.obtener(factura.id()).total()).isEqualByComparingTo("600.00");
    }

    @Test
    void guardarSinCambiosNoCreaUnaVersion() {
        DocumentoResponse factura = documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), null, FECHA,
                "Nota", true, List.of(linea(tubo.id(), "2", "100"))));
        LineaResponse linea = factura.lineas().get(0);

        documentoService.actualizar(factura.id(), new DocumentoRequest(null, cliente.id(), null, FECHA, "Nota", true,
                List.of(new LineaRequest(linea.id(), tubo.id(), linea.descripcion(), new BigDecimal("2.000"), linea.pesoUnitKg(),
                        new BigDecimal("100.00"), MetodoPrecio.MANUAL, null, null, null))));

        assertThat(documentoService.revisiones(factura.id())).isEmpty();
    }

    @Test
    void unDocumentoInexistenteNoTieneVersiones() {
        assertEstado(HttpStatus.NOT_FOUND, () -> documentoService.revisiones(UUID.randomUUID()));
    }

    // ── Consultas, abonos y reportes (con open-in-view desactivado) ──────────

    @Test
    void listarIncluyeLineasProyectoYAbonos() {
        var proyecto = proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra centro", null));
        DocumentoResponse factura = documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), proyecto.id(),
                FECHA, null, false, List.of(linea(tubo.id(), "2", "1500"))));
        abonoService.registrarAbono(factura.id(), new AbonoRequest(new BigDecimal("1000"), FECHA));

        DocumentoResponse listada = documentoService.filtrar(null, null, null, null, null, null, null).get(0);

        assertThat(listada.proyectoNombre()).isEqualTo("Obra centro");
        assertThat(listada.lineas()).hasSize(1);
        assertThat(listada.total()).isEqualByComparingTo("3000");
        assertThat(listada.totalAbonado()).isEqualByComparingTo("1000");
        assertThat(listada.saldoPendiente()).isEqualByComparingTo("2000");
    }

    @Test
    void filtraPorTipoProductoYFechaDelDocumento() {
        ProductoResponse varilla = productoService.crear(new ProductoRequest("018001001", "Varilla Lisa 3/8", null, null, null, List.of()));
        crear(TipoDocumento.COTIZACION);
        crear(TipoDocumento.FACTURA);

        assertThat(documentoService.filtrar(TipoDocumento.COTIZACION, null, null, null, null, null, null)).hasSize(1);
        assertThat(documentoService.filtrar(null, null, null, null, tubo.id(), null, null)).hasSize(2);
        assertThat(documentoService.filtrar(null, null, null, null, varilla.id(), null, null)).isEmpty();
        assertThat(documentoService.filtrar(null, null, null, null, null, FECHA, FECHA)).hasSize(2);
        assertThat(documentoService.filtrar(null, null, null, null, null, FECHA.plusDays(1), null)).isEmpty();
    }

    @Test
    void elReporteSoloCuentaFacturasActivas() {
        crear(TipoDocumento.COTIZACION);
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);
        abonoService.registrarAbono(factura.id(), new AbonoRequest(new BigDecimal("40"), FECHA));

        var reporte = reporteService.generarReporte(null, null, null, null);

        assertThat(reporte.totalFacturas()).isEqualTo(1);
        assertThat(reporte.ingresosTotales()).isEqualByComparingTo("100");
        assertThat(reporte.totalCobrado()).isEqualByComparingTo("40");
        assertThat(reporte.porCobrar()).isEqualByComparingTo("60");
    }

    @Test
    void losAbonosSoloVanAFacturasActivasYSinPasarseDelSaldo() {
        DocumentoResponse cotizacion = crear(TipoDocumento.COTIZACION);
        DocumentoResponse factura = crear(TipoDocumento.FACTURA);

        assertEstado(HttpStatus.BAD_REQUEST, () -> abonoService.registrarAbono(cotizacion.id(), new AbonoRequest(BigDecimal.TEN, FECHA)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> abonoService.registrarAbono(factura.id(), new AbonoRequest(new BigDecimal("101"), FECHA)));
    }

    @Test
    void noSePuedeBorrarUnProductoUsadoEnUnDocumento() {
        crear(TipoDocumento.COTIZACION);

        assertEstado(HttpStatus.CONFLICT, () -> productoService.eliminar(tubo.id()));
    }

    // ── Ayudas ───────────────────────────────────────────────────────────────

    private DocumentoResponse crear(TipoDocumento tipo) {
        return documentoService.crear(request(tipo, List.of(linea(tubo.id(), "1", "100"))));
    }

    private DocumentoRequest request(TipoDocumento tipo, List<LineaRequest> lineas) {
        return new DocumentoRequest(tipo, cliente.id(), null, FECHA, null, false, lineas);
    }

    private DocumentoRequest requestDe(DocumentoResponse doc) {
        return new DocumentoRequest(null, doc.clienteId(), doc.proyectoId(), doc.fecha(), doc.notas(), doc.mostrarEntrega(),
                List.of(linea(tubo.id(), "1", "100")));
    }

    private static LineaRequest linea(UUID productoId, String cantidad, String precio) {
        return new LineaRequest(null, productoId, null, new BigDecimal(cantidad), null, new BigDecimal(precio),
                null, null, null, null);
    }

    private static void assertEstado(HttpStatus esperado, org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(esperado));
    }
}
