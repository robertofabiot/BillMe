package com.example.billme.service;

import com.example.billme.dto.*;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.FormaCalculo;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.enums.MetodoPrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.PrecioLista;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.support.IntegrationTest;
import com.example.billme.util.Fechas;
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
class PrecioSugeridoServiceTest extends IntegrationTest {

    @Autowired
    PrecioSugeridoService precioSugeridoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProductoService productoService;

    @Autowired
    DocumentoService documentoService;

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    PrecioListaRepository precioListaRepository;

    @Autowired
    ConfiguracionService configuracionService;

    ClienteResponse clienteA;
    ClienteResponse clienteB;
    ProductoResponse productoTubo;
    ProductoResponse productoPlaca;

    @BeforeEach
    void setup() {
        clienteA = clienteService.crear(new ClienteRequest("Cliente Alpha", "11111111", null, new BigDecimal("4.00"), null));
        clienteB = clienteService.crear(new ClienteRequest("Cliente Beta", "22222222", null, new BigDecimal("6.00"), new BigDecimal("2.1000")));

        productoTubo = productoService.crear(new ProductoRequest(
                "TUB-001", "Tubo Industrial 2x2", null, "UND", new BigDecimal("15.5"), List.of()
        ));

        // Use seeded generic plate or create custom product with PLACA shape
        UUID placaId = productoRepository.findByCodigoInterno("GEN-PLACA").orElseThrow().getId();
        productoPlaca = productoService.obtenerPorId(placaId);
    }

    @Test
    void ultimoPrecioDeEseClienteTienePrioridad() {
        // Documento con Cliente B hace 2 días con precio 200
        crearDoc(clienteB.id(), productoTubo.id(), new BigDecimal("200.00"), Fechas.hoy().minusDays(2), TipoDocumento.COTIZACION);

        // Documento con Cliente A hace 5 días con precio 180
        DocumentoResponse docA = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("180.00"), Fechas.hoy().minusDays(5), TipoDocumento.FACTURA);

        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);

        assertThat(res.precioUnit()).isEqualByComparingTo("180.00");
        assertThat(res.origen()).isEqualTo("ESTE_CLIENTE");
        assertThat(res.clienteNombre()).isEqualTo("Cliente Alpha");
        assertThat(res.folio()).isEqualTo(docA.folio());
        assertThat(res.fecha()).isEqualTo(Fechas.hoy().minusDays(5));
        assertThat(res.diasAntiguedad()).isEqualTo(5);
        assertThat(res.viejo()).isFalse();
    }

    @Test
    void ultimoPrecioDeOtroClienteSiNoHayHistorialConEste() {
        // Solo Cliente B tiene compras
        DocumentoResponse docB = crearDoc(clienteB.id(), productoTubo.id(), new BigDecimal("220.00"), Fechas.hoy().minusDays(10), TipoDocumento.COTIZACION);

        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);

        assertThat(res.precioUnit()).isEqualByComparingTo("220.00");
        assertThat(res.origen()).isEqualTo("OTRO_CLIENTE");
        assertThat(res.clienteNombre()).isEqualTo("Cliente Beta");
        assertThat(res.folio()).isEqualTo(docB.folio());
        assertThat(res.fecha()).isEqualTo(Fechas.hoy().minusDays(10));
        assertThat(res.diasAntiguedad()).isEqualTo(10);
    }

    @Test
    void rechazadaCuentaYAnuladaNoCuenta() {
        // Cotización rechazada con precio 150
        DocumentoResponse cot = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("150.00"), Fechas.hoy().minusDays(3), TipoDocumento.COTIZACION);
        documentoService.cambiarEstadoCotizacion(cot.id(), EstadoDocumento.RECHAZADA);

        // Factura anulada más reciente con precio 300
        DocumentoResponse fac = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("300.00"), Fechas.hoy().minusDays(1), TipoDocumento.FACTURA);
        documentoService.anular(fac.id());

        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);

        // Debe tomar la rechazada de 150 y descartar la anulada
        assertThat(res.precioUnit()).isEqualByComparingTo("150.00");
        assertThat(res.folio()).isEqualTo(cot.folio());
    }

    @Test
    void viejoCon366DiasYNoViejoCon365() {
        // Documento con 366 días de antigüedad
        DocumentoResponse docViejo = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("110.00"), Fechas.hoy().minusDays(366), TipoDocumento.COTIZACION);
        PrecioSugeridoResponse resViejo = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);

        assertThat(resViejo.diasAntiguedad()).isEqualTo(366);
        assertThat(resViejo.viejo()).isTrue();

        // Limpiar para probar 365 días
        documentoService.cambiarEstadoCotizacion(docViejo.id(), EstadoDocumento.RECHAZADA);
        DocumentoResponse docNoViejo = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("115.00"), Fechas.hoy().minusDays(365), TipoDocumento.COTIZACION);
        PrecioSugeridoResponse resNoViejo = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);

        assertThat(resNoViejo.diasAntiguedad()).isEqualTo(365);
        assertThat(resNoViejo.viejo()).isFalse();
    }

    @Test
    void genericoPorDescripcionNormalizada() {
        UUID libreId = productoRepository.findByCodigoInterno("GEN-LIBRE").orElseThrow().getId();
        ProductoResponse libre = productoService.obtenerPorId(libreId);

        // Crear cotización con descripción específica
        crearDocConDescripcion(clienteA.id(), libre.id(), "Placa 10\" x 8\" x 1/4\"", new BigDecimal("195.60"), Fechas.hoy().minusDays(1));

        // Búsqueda con misma descripción normalizada (diferentes mayúsculas / espacios)
        PrecioSugeridoResponse resExacto = precioSugeridoService.sugerir(clienteA.id(), libre.id(), "  placa   10\"  x  8\"  x  1/4\"  ");
        assertThat(resExacto.precioUnit()).isEqualByComparingTo("195.60");
        assertThat(resExacto.origen()).isEqualTo("ESTE_CLIENTE");

        // Búsqueda con descripción distinta no encuentra historial
        PrecioSugeridoResponse resDistinto = precioSugeridoService.sugerir(clienteA.id(), libre.id(), "Platina 4\" x 3/16\"");
        assertThat(resDistinto.precioUnit()).isNull();
        assertThat(resDistinto.origen()).isNull();

        // Búsqueda sin descripción no busca historial para genéricos
        PrecioSugeridoResponse resVacio = precioSugeridoService.sugerir(clienteA.id(), libre.id(), "");
        assertThat(resVacio.precioUnit()).isNull();
        assertThat(resVacio.origen()).isNull();
    }

    @Test
    void tresPrioridadesDeTarifa() {
        // Prioridad 1: última usada con ese cliente en una línea POR_PESO
        crearDocPorPeso(clienteB.id(), productoPlaca.id(), new BigDecimal("250.00"), "2.35", Fechas.hoy().minusDays(2));
        PrecioSugeridoResponse res1 = precioSugeridoService.sugerir(clienteB.id(), productoPlaca.id(), null);
        assertThat(res1.tarifaKgUsd()).isEqualByComparingTo("2.35");
        assertThat(res1.origenTarifa()).isEqualTo("ULTIMA");

        // Prioridad 2: sin línea POR_PESO previa, toma la tarifa propia del cliente (clienteB tiene 2.1000)
        PrecioSugeridoResponse res2 = precioSugeridoService.sugerir(clienteB.id(), productoPlaca.id(), null);
        // Cuando clienteA no tiene líneas POR_PESO, pero le asignamos tarifa al cliente
        clienteService.actualizar(clienteA.id(), new ClienteRequest("Cliente Alpha", "11111111", null, new BigDecimal("4.00"), new BigDecimal("1.9500")));
        PrecioSugeridoResponse resCliente = precioSugeridoService.sugerir(clienteA.id(), productoPlaca.id(), null);
        assertThat(resCliente.tarifaKgUsd()).isEqualByComparingTo("1.9500");
        assertThat(resCliente.origenTarifa()).isEqualTo("CLIENTE");

        // Prioridad 3: cliente sin tarifa propia y sin líneas POR_PESO -> configuración (1.80)
        clienteService.actualizar(clienteA.id(), new ClienteRequest("Cliente Alpha", "11111111", null, new BigDecimal("4.00"), null));
        PrecioSugeridoResponse resConfig = precioSugeridoService.sugerir(clienteA.id(), productoPlaca.id(), null);
        assertThat(resConfig.tarifaKgUsd()).isEqualByComparingTo("1.80");
        assertThat(resConfig.origenTarifa()).isEqualTo("CONFIGURACION");
    }

    @Test
    void formulaCalculaConDescuentoCliente() {
        // Registrar precio de lista vigente para tubo
        precioListaRepository.save(PrecioLista.builder()
                .producto(productoRepository.getReferenceById(productoTubo.id()))
                .precioSinIva(new BigDecimal("1000.00"))
                .fecha(Fechas.hoy())
                .fuente(FuentePrecio.MANUAL)
                .build());

        // Cliente Alpha tiene 4% de descuento
        // 1000 * 1.15 * (1 - 0.04) = 1104.00
        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);
        assertThat(res.precioLista()).isEqualByComparingTo("1000.00");
        assertThat(res.descuentoPct()).isEqualByComparingTo("4.00");
        assertThat(res.precioFormula()).isEqualByComparingTo("1104.00");

        // Endpoint de cálculo directo de fórmula
        PrecioFormulaResponse formula = precioSugeridoService.calcularFormula(productoTubo.id(), new BigDecimal("6.00"));
        assertThat(formula.precioUnit()).isEqualByComparingTo("1081.00");

        // Producto sin lista lanza 409
        assertThatThrownBy(() -> precioSugeridoService.calcularFormula(productoPlaca.id(), BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Este producto no tiene precio de lista");
    }

    @Test
    void historialConFiltroPorClienteYBusqueda() {
        crearDocConDescripcion(clienteA.id(), productoTubo.id(), "Tubo Industrial 2x2 C14", new BigDecimal("180.00"), Fechas.hoy().minusDays(1));
        crearDocConDescripcion(clienteB.id(), productoTubo.id(), "Tubo Redondo 1 pulgada", new BigDecimal("120.00"), Fechas.hoy().minusDays(2));

        // Filtro por cliente
        List<PrecioHistorialResponse> histClienteA = precioSugeridoService.historial(null, clienteA.id());
        assertThat(histClienteA).hasSize(1);
        assertThat(histClienteA.get(0).clienteNombre()).isEqualTo("Cliente Alpha");

        // Filtro por texto
        List<PrecioHistorialResponse> histRedondo = precioSugeridoService.historial("redondo", null);
        assertThat(histRedondo).hasSize(1);
        assertThat(histRedondo.get(0).descripcion()).contains("Redondo");
    }

    @Test
    void cotizacionDeFacturaAnuladaSigueAlimentandoPrecioSugerido() {
        DocumentoResponse cot = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("350.00"), Fechas.hoy().minusDays(1), TipoDocumento.COTIZACION);
        DocumentoResponse fac = documentoService.convertirAFactura(cot.id(), null);
        documentoService.anular(fac.id());

        // La cotización volvió a pendiente; su precio sigue sugiriéndose
        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);
        assertThat(res.precioUnit()).isEqualByComparingTo("350.00");
        assertThat(res.folio()).isEqualTo(cot.folio());
        assertThat(res.origen()).isEqualTo("ESTE_CLIENTE");
    }

    @Test
    void historicoAlimentaPrecioSugeridoYHistorial() {
        DocumentoResponse doc = crearDoc(clienteA.id(), productoTubo.id(), new BigDecimal("420.00"), Fechas.hoy().minusDays(3), TipoDocumento.COTIZACION);
        var docEnt = documentoRepository.findById(doc.id()).orElseThrow();
        docEnt.setTipo(TipoDocumento.HISTORICO);
        docEnt.setEstado(EstadoDocumento.HISTORICA);
        docEnt.setFolio("HIS-0001");
        documentoRepository.save(docEnt);

        PrecioSugeridoResponse res = precioSugeridoService.sugerir(clienteA.id(), productoTubo.id(), null);
        assertThat(res.precioUnit()).isEqualByComparingTo("420.00");
        assertThat(res.folio()).isEqualTo("HIS-0001");

        List<PrecioHistorialResponse> hist = precioSugeridoService.historial(null, clienteA.id());
        assertThat(hist).anyMatch(h -> "HIS-0001".equals(h.folio()) && "HISTORICO".equals(h.tipo())
                && h.precioUnit().compareTo(new BigDecimal("420.00")) == 0);
    }

    private DocumentoResponse crearDoc(UUID clienteId, UUID productoId, BigDecimal pu, LocalDate fecha, TipoDocumento tipo) {
        return documentoService.crear(new DocumentoRequest(
                tipo,
                clienteId,
                null,
                fecha,
                null,
                false,
                List.of(new LineaRequest(null, productoId, null, BigDecimal.ONE, null, pu, MetodoPrecio.MANUAL, null, null, null))
        ));
    }

    private DocumentoResponse crearDocConDescripcion(UUID clienteId, UUID productoId, String desc, BigDecimal pu, LocalDate fecha) {
        return documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                clienteId,
                null,
                fecha,
                null,
                false,
                List.of(new LineaRequest(null, productoId, desc, BigDecimal.ONE, null, pu, MetodoPrecio.MANUAL, null, null, null))
        ));
    }

    private DocumentoResponse crearDocPorPeso(UUID clienteId, UUID productoId, BigDecimal pu, String tarifaUsd, LocalDate fecha) {
        String params = String.format("{\"tarifaUsd\":%s}", tarifaUsd);
        return documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                clienteId,
                null,
                fecha,
                null,
                false,
                List.of(new LineaRequest(null, productoId, "Placa 10x8", BigDecimal.ONE, new BigDecimal("2.58"), pu, MetodoPrecio.POR_PESO, params, null, null))
        ));
    }
}
