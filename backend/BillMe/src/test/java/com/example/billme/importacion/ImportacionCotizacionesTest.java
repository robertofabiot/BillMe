package com.example.billme.importacion;

import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoLineaRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.service.DocumentoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import com.example.billme.dto.DecisionConciliacion;
import com.example.billme.model.AliasProducto;
import com.example.billme.model.Producto;
import com.example.billme.repository.AliasProductoRepository;
import com.example.billme.repository.ProductoRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ImportacionCotizacionesTest extends IntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoLineaRepository documentoLineaRepository;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AliasProductoRepository aliasProductoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "cotizaciones.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido);
    }

    private ResultActions vistaPrevia(byte[] contenido, UUID clienteId) throws Exception {
        return mockMvc.perform(multipart("/api/importaciones/cotizaciones/vista-previa")
                .file(archivo(contenido))
                .param("clienteId", clienteId.toString()));
    }

    private ResultActions aplicar(byte[] contenido, UUID clienteId) throws Exception {
        return aplicar(contenido, clienteId, null);
    }

    private ResultActions aplicar(byte[] contenido, UUID clienteId, String decisionesJson) throws Exception {
        var builder = multipart("/api/importaciones/cotizaciones/aplicar")
                .file(archivo(contenido))
                .param("clienteId", clienteId.toString());
        if (decisionesJson != null) {
            builder.param("decisiones", decisionesJson);
        }
        return mockMvc.perform(builder);
    }

    @Test
    void dosFilasIgualesEnFechaYEntregaFormanUnaFacturaOtraEntregaFormaOtra() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Tubo 1", 100.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 3, "Tubo 2", 150.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Tubo 3", 200.0, null, null, null, null, null, "Bodega Sur", null, null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(3))
                .andExpect(jsonPath("$.aplicada").value(true));

        List<Documento> docs = documentoRepository.findAll();
        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getTipo()).isEqualTo(TipoDocumento.HISTORICO);
        assertThat(docs.get(0).getEstado()).isEqualTo(EstadoDocumento.HISTORICA);
        assertThat(docs.get(0).getFolio()).isEqualTo("HIS-0001");
        assertThat(docs.get(0).getProyecto()).isNull();
        assertThat(docs.get(1).getFolio()).isEqualTo("HIS-0002");

        // La siguiente factura normal debe ser FAC-0001
        Producto prod = productoRepository.findByCodigoInterno("GEN-LIBRE").orElseThrow();
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.getId(),
                null,
                LocalDate.of(2026, 11, 2),
                null,
                false,
                List.of(new LineaRequest(null, prod.getId(), "Tubo libre", BigDecimal.ONE, null, new BigDecimal("100.00"), null, null, null, null))
        ));
        assertThat(fac.folio()).isEqualTo("FAC-0001");

        DocumentoResponse doc1 = documentoService.obtener(docs.get(0).getId());
        DocumentoResponse doc2 = documentoService.obtener(docs.get(1).getId());

        DocumentoResponse docNorte = doc1.lineas().stream().anyMatch(l -> "Bodega Norte".equals(l.lugarEntrega())) ? doc1 : doc2;
        DocumentoResponse docSur = doc1.lineas().stream().anyMatch(l -> "Bodega Sur".equals(l.lugarEntrega())) ? doc1 : doc2;

        assertThat(docNorte.lineas()).hasSize(2);
        assertThat(docNorte.total()).isEqualByComparingTo("650.00");

        assertThat(docSur.lineas()).hasSize(1);
        assertThat(docSur.total()).isEqualByComparingTo("200.00");
    }

    @Test
    void reimportarElMismoArchivoDa0NuevasYNoCreaNada() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Tubo 1", 100.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 3, "Tubo 2", 150.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(1))
                .andExpect(jsonPath("$.filasNuevas").value(2));

        assertThat(documentoRepository.count()).isEqualTo(1);
        assertThat(documentoLineaRepository.count()).isEqualTo(2);

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(0))
                .andExpect(jsonPath("$.filasYaImportadas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(0))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(0));

        assertThat(documentoRepository.count()).isEqualTo(1);
        assertThat(documentoLineaRepository.count()).isEqualTo(2);
    }

    @Test
    void agregarUnaFilaAUnGrupoLaSumaALaMismaFacturaYActualizaElTotal() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel1 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Item 1", 100.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .bytes();

        aplicar(excel1, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(1))
                .andExpect(jsonPath("$.filasNuevas").value(1));

        Documento docInicial = documentoRepository.findAll().get(0);
        assertThat(docInicial.getTotal()).isEqualByComparingTo("200.00");

        byte[] excel2 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Item 1", 100.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item 2", 50.0, null, null, null, null, null, "Bodega Norte", null, null, null)
                .bytes();

        aplicar(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(1))
                .andExpect(jsonPath("$.filasYaImportadas").value(1))
                .andExpect(jsonPath("$.registrosNuevos").value(0))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(1));

        assertThat(documentoRepository.count()).isEqualTo(1);
        DocumentoResponse docActualizado = documentoService.obtener(docInicial.getId());
        assertThat(docActualizado.lineas()).hasSize(2);
        assertThat(docActualizado.lineas().get(0).orden()).isEqualTo(0);
        assertThat(docActualizado.lineas().get(0).descripcion()).isEqualTo("Item 1");
        assertThat(docActualizado.lineas().get(1).orden()).isEqualTo(1);
        assertThat(docActualizado.lineas().get(1).descripcion()).isEqualTo("Item 2");
        assertThat(docActualizado.total()).isEqualByComparingTo("250.00");
    }

    @Test
    void dosFilasIdenticasSeConservanLasDos() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Plancha 1/2", 100.0, null, null, null, null, null, "Local", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 2, "Plancha 1/2", 100.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(1));

        DocumentoResponse doc = documentoService.obtener(documentoRepository.findAll().get(0).getId());
        assertThat(doc.lineas()).hasSize(2);
        assertThat(doc.lineas().get(0).descripcion()).isEqualTo("Plancha 1/2");
        assertThat(doc.lineas().get(1).descripcion()).isEqualTo("Plancha 1/2");
        assertThat(doc.total()).isEqualByComparingTo("400.00");

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(0))
                .andExpect(jsonPath("$.filasYaImportadas").value(2));

        assertThat(documentoRepository.count()).isEqualTo(1);
        assertThat(documentoLineaRepository.count()).isEqualTo(2);
    }

    @Test
    void laVistaPreviaNoGuardaNada() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Placa base", 100.0, null, null, null, null, null, "Local", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 3, "Tubo 2x2", 150.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        vistaPrevia(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(false))
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(1));

        assertThat(documentoRepository.count()).isEqualTo(0);
        assertThat(documentoLineaRepository.count()).isEqualTo(0);
    }

    @Test
    void columnaClienteConUnNombreNuevoCreaElCliente() throws Exception {
        Cliente clientePorDefecto = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Por Defecto")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        String nuevoClienteNombre = "Constructora Alfa S.A.";
        assertThat(clienteRepository.findFirstByNombreIgnoreCase(nuevoClienteNombre)).isEmpty();

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 5, "Angulo 2x1/8", 120.0, null, null, null, null, null, "Obra", null, null, nuevoClienteNombre)
                .bytes();

        vistaPrevia(excel, clientePorDefecto.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientesNuevos[0]").value(nuevoClienteNombre));

        assertThat(clienteRepository.findFirstByNombreIgnoreCase(nuevoClienteNombre)).isEmpty();

        aplicar(excel, clientePorDefecto.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientesNuevos[0]").value(nuevoClienteNombre));

        Optional<Cliente> creado = clienteRepository.findFirstByNombreIgnoreCase(nuevoClienteNombre);
        assertThat(creado).isPresent();
        assertThat(creado.get().getDescuentoPct()).isEqualByComparingTo(BigDecimal.ZERO);

        Documento doc = documentoRepository.findAll().get(0);
        assertThat(doc.getCliente().getId()).isEqualTo(creado.get().getId());
    }

    @Test
    void pu100_555QuedaEn100_56() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Tornillo especial", 100.555, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk());

        DocumentoLinea linea = documentoLineaRepository.findAll().get(0);
        assertThat(linea.getPrecioUnit()).isEqualByComparingTo("100.56");
        assertThat(linea.getTotal()).isEqualByComparingTo("100.56");
    }

    @Test
    void costo950SinIvaQuedaEn1092_50() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Principal")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Lamina prepintada", 1200.0, null, null, null, 950.00, null, "Local", null, null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk());

        DocumentoLinea linea = documentoLineaRepository.findAll().get(0);
        assertThat(linea.getCostoUnitConIva()).isEqualByComparingTo("1092.50");
    }

    @Test
    void claveImportacionGeneraSha256Valido() {
        String clave1 = ClaveImportacion.de("parte1", "parte2");
        assertThat(clave1).hasSize(64);
        assertThat(clave1).matches("^[0-9a-f]{64}$");

        String claveConNull = ClaveImportacion.de("parte1", null, "parte2");
        String claveConVacio = ClaveImportacion.de("parte1", "", "parte2");
        assertThat(claveConNull).isEqualTo(claveConVacio);
    }

    @Test
    void clienteInexistenteDa404() throws Exception {
        UUID noExiste = UUID.randomUUID();
        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Item", 10.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        vistaPrevia(excel, noExiste)
                .andExpect(status().isNotFound());

        aplicar(excel, noExiste)
                .andExpect(status().isNotFound());
    }

    @Test
    void elegirProductoCreaAliasYAlReimportarNoQuedaPendiente() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Conciliacion")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto productoReal = productoRepository.save(Producto.builder()
                .codigoInterno("PROD-TUB-1")
                .nombrePrincipal("Tubo Industrial 1 pulgada")
                .unidad("UND")
                .generico(false)
                .activo(true)
                .build());

        String descripcionEspecial = "Tubo Redondo Especial 1 pulgada";

        byte[] excel1 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, descripcionEspecial, 100.0, null, null, null, null, null, "Bodega", null, null, null)
                .bytes();

        vistaPrevia(excel1, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porConciliar").isArray())
                .andExpect(jsonPath("$.porConciliar[0].descripcion").value(descripcionEspecial))
                .andExpect(jsonPath("$.porConciliar[0].filas").value(1));

        String decisionesJson = objectMapper.writeValueAsString(List.of(
                new DecisionConciliacion(descripcionEspecial, productoReal.getId())
        ));

        aplicar(excel1, cliente.getId(), decisionesJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(1))
                .andExpect(jsonPath("$.filasNuevas").value(1));

        Optional<AliasProducto> aliasOpt = aliasProductoRepository.findFirstByAliasNorm(
                com.example.billme.util.NormalizadorTexto.normalizar(descripcionEspecial));
        assertThat(aliasOpt).isPresent();
        assertThat(aliasOpt.get().getProducto().getId()).isEqualTo(productoReal.getId());
        assertThat(aliasOpt.get().getNombreAlias()).isEqualTo(descripcionEspecial);

        DocumentoLinea linea1 = documentoLineaRepository.findAll().get(0);
        assertThat(linea1.getProducto().getId()).isEqualTo(productoReal.getId());
        assertThat(linea1.getDescripcion()).isEqualTo(descripcionEspecial);

        byte[] excel2 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 2), 5, descripcionEspecial, 120.0, null, null, null, null, null, "Bodega", null, null, null)
                .bytes();

        vistaPrevia(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(1))
                .andExpect(jsonPath("$.porConciliar").isEmpty());

        aplicar(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(1));

        List<DocumentoLinea> lineas = documentoLineaRepository.findAll();
        assertThat(lineas).hasSize(2);
        DocumentoLinea linea2 = lineas.stream().filter(l -> l.getCantidad().compareTo(new BigDecimal("5.000")) == 0).findFirst().orElseThrow();
        assertThat(linea2.getProducto().getId()).isEqualTo(productoReal.getId());
        assertThat(linea2.getDescripcion()).isEqualTo(descripcionEspecial);
    }

    @Test
    void sinDecisionEntraConPropuestaGenericaYConservaDescripcion() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Generico")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto genPlaca = productoRepository.findByCodigoInterno("GEN-PLACA").orElseThrow();
        Producto genPlatina = productoRepository.findByCodigoInterno("GEN-PLATINA").orElseThrow();
        Producto genLibre = productoRepository.findByCodigoInterno("GEN-LIBRE").orElseThrow();

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Placa base 200x200x12mm", 300.0, null, null, null, null, null, "Local", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 2, "Platina 2x1/4 x 6m", 150.0, null, null, null, null, null, "Local", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 3, "Servicio de oxicorte especial", 80.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        vistaPrevia(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porConciliar").isArray())
                .andExpect(jsonPath("$.porConciliar[0].descripcion").value("Placa base 200x200x12mm"))
                .andExpect(jsonPath("$.porConciliar[0].propuestaProductoId").value(genPlaca.getId().toString()))
                .andExpect(jsonPath("$.porConciliar[0].propuestaNombre").value(genPlaca.getNombrePrincipal()))
                .andExpect(jsonPath("$.porConciliar[1].descripcion").value("Platina 2x1/4 x 6m"))
                .andExpect(jsonPath("$.porConciliar[1].propuestaProductoId").value(genPlatina.getId().toString()))
                .andExpect(jsonPath("$.porConciliar[1].propuestaNombre").value(genPlatina.getNombrePrincipal()))
                .andExpect(jsonPath("$.porConciliar[2].descripcion").value("Servicio de oxicorte especial"))
                .andExpect(jsonPath("$.porConciliar[2].propuestaProductoId").value(genLibre.getId().toString()))
                .andExpect(jsonPath("$.porConciliar[2].propuestaNombre").value(genLibre.getNombrePrincipal()));

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(3));

        List<DocumentoLinea> lineas = documentoLineaRepository.findAll();
        assertThat(lineas).hasSize(3);

        DocumentoLinea lPlaca = lineas.stream().filter(l -> l.getDescripcion().equals("Placa base 200x200x12mm")).findFirst().orElseThrow();
        assertThat(lPlaca.getProducto().getId()).isEqualTo(genPlaca.getId());

        DocumentoLinea lPlatina = lineas.stream().filter(l -> l.getDescripcion().equals("Platina 2x1/4 x 6m")).findFirst().orElseThrow();
        assertThat(lPlatina.getProducto().getId()).isEqualTo(genPlatina.getId());

        DocumentoLinea lLibre = lineas.stream().filter(l -> l.getDescripcion().equals("Servicio de oxicorte especial")).findFirst().orElseThrow();
        assertThat(lLibre.getProducto().getId()).isEqualTo(genLibre.getId());

        assertThat(aliasProductoRepository.findFirstByAliasNorm(com.example.billme.util.NormalizadorTexto.normalizar("Placa base 200x200x12mm"))).isEmpty();
        assertThat(aliasProductoRepository.findFirstByAliasNorm(com.example.billme.util.NormalizadorTexto.normalizar("Platina 2x1/4 x 6m"))).isEmpty();
        assertThat(aliasProductoRepository.findFirstByAliasNorm(com.example.billme.util.NormalizadorTexto.normalizar("Servicio de oxicorte especial"))).isEmpty();
    }

    @Test
    void trasImportarConsultaPrecioSugeridoDevuelvePrecioHistorial() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Historial")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto productoReal = productoRepository.save(Producto.builder()
                .codigoInterno("PROD-VIGA-1")
                .nombrePrincipal("Viga IPE 100")
                .unidad("UND")
                .generico(false)
                .activo(true)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Viga IPE 100 especial", 275.50, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        String decisionesJson = objectMapper.writeValueAsString(List.of(
                new DecisionConciliacion("Viga IPE 100 especial", productoReal.getId())
        ));

        aplicar(excel, cliente.getId(), decisionesJson)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/precios/sugerido")
                        .param("clienteId", cliente.getId().toString())
                        .param("productoId", productoReal.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnit").value(275.50))
                .andExpect(jsonPath("$.origen").value("ESTE_CLIENTE"))
                .andExpect(jsonPath("$.clienteNombre").value("Cliente Historial"));
    }

    @Test
    void decisionConProductoInexistenteDa404() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Test 404")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Tubo Especial", 100.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        UUID productoFantasma = UUID.randomUUID();
        String decisionesJson = objectMapper.writeValueAsString(List.of(
                new DecisionConciliacion("Tubo Especial", productoFantasma)
        ));

        aplicar(excel, cliente.getId(), decisionesJson)
                .andExpect(status().isNotFound());
    }

    @Test
    void descripcionMayorA200NoCreaAliasPeroImportaCorrectamente() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Descripcion Larga")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto productoReal = productoRepository.save(Producto.builder()
                .codigoInterno("PROD-LARGO-1")
                .nombrePrincipal("Plancha Lisa Gruesa")
                .unidad("UND")
                .generico(false)
                .activo(true)
                .build());

        String descripcionLarga = "X".repeat(210);

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, descripcionLarga, 500.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        String decisionesJson = objectMapper.writeValueAsString(List.of(
                new DecisionConciliacion(descripcionLarga, productoReal.getId())
        ));

        aplicar(excel, cliente.getId(), decisionesJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(1));

        DocumentoLinea linea = documentoLineaRepository.findAll().get(0);
        assertThat(linea.getProducto().getId()).isEqualTo(productoReal.getId());
        assertThat(linea.getDescripcion()).isEqualTo(descripcionLarga);

        assertThat(aliasProductoRepository.findByProductoId(productoReal.getId())).isEmpty();
    }

    @Test
    void filaConCodProductoSeConciliaDirectamenteSinEstarPendiente() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Codigo")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto prod = productoRepository.save(Producto.builder()
                .codigoInterno("COD-12345")
                .nombrePrincipal("Tubo Con Codigo")
                .unidad("UND")
                .generico(false)
                .activo(true)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Descripcion que no importa", 50.0, "COD-12345", null, null, null, null, "Local", null, null, null)
                .bytes();

        vistaPrevia(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(1))
                .andExpect(jsonPath("$.porConciliar").isEmpty());

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk());

        DocumentoLinea linea = documentoLineaRepository.findAll().get(0);
        assertThat(linea.getProducto().getId()).isEqualTo(prod.getId());
        assertThat(linea.getDescripcion()).isEqualTo("Descripcion que no importa");
    }

    @Test
    void filaConNombreNormCoincidenteSeConciliaDirectamenteSinEstarPendiente() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente NombreNorm")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        Producto prod = productoRepository.save(Producto.builder()
                .codigoInterno("PROD-NORM-1")
                .nombrePrincipal("Angulo Estructural 2x2")
                .unidad("UND")
                .generico(false)
                .activo(true)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Angulo  Estructural  2 x 2", 70.0, null, null, null, null, null, "Local", null, null, null)
                .bytes();

        vistaPrevia(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasNuevas").value(1))
                .andExpect(jsonPath("$.porConciliar").isEmpty());

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk());

        DocumentoLinea linea = documentoLineaRepository.findAll().get(0);
        assertThat(linea.getProducto().getId()).isEqualTo(prod.getId());
    }

    @Test
    void importadoConProyectoEnExcelGuardaEnNotasYProyectoEsNull() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Con Proyecto")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Item Proyecto", 100.0, null, null, null, null, null, "Bodega", "Proyecto Torre", null, null)
                .bytes();

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(1));

        Documento doc = documentoRepository.findAll().get(0);
        assertThat(doc.getTipo()).isEqualTo(TipoDocumento.HISTORICO);
        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.HISTORICA);
        assertThat(doc.getProyecto()).isNull();
        assertThat(doc.getNotas()).contains("Proyecto en el Excel: Proyecto Torre.");
    }

    @Test
    void trasConvertirRegistroYAgregarFilaAlExcelLaFacturaNoCambiaYFilaCuentaComoYaImportada() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Convertir")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        byte[] excel1 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Item Base", 100.0, null, null, null, null, null, "Bodega", null, null, null)
                .bytes();

        aplicar(excel1, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(1));

        Documento doc = documentoRepository.findAll().get(0);
        assertThat(doc.getFolio()).isEqualTo("HIS-0001");

        // Convertir el histórico en factura activa
        DocumentoResponse convertida = documentoService.convertirHistorico(doc.getId());
        assertThat(convertida.tipo()).isEqualTo("FACTURA");
        assertThat(convertida.folio()).isEqualTo("FAC-0001");
        assertThat(convertida.lineas()).hasSize(1);
        assertThat(convertida.total()).isEqualByComparingTo("200.00");

        // Ahora preparamos excel2 con el mismo grupo + una fila nueva
        byte[] excel2 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 2, "Item Base", 100.0, null, null, null, null, null, "Bodega", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item Nuevo", 50.0, null, null, null, null, null, "Bodega", null, null, null)
                .bytes();

        // En vista previa, como el grupo ya está convertido, esa fila cuenta como ya importada (filasNuevas = 0, filasYaImportadas = 2)
        vistaPrevia(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(0))
                .andExpect(jsonPath("$.filasYaImportadas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(0))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(0));

        // Aplicar no modifica la factura convertida ni crea nuevos registros
        aplicar(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(0))
                .andExpect(jsonPath("$.filasYaImportadas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(0))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(0));

        DocumentoResponse docTras = documentoService.obtener(doc.getId());
        assertThat(docTras.lineas()).hasSize(1);
        assertThat(docTras.total()).isEqualByComparingTo("200.00");
    }

    @Test
    void archivoMixtoConGrupoConvertidoGrupoExistenteYGrupoNuevo() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cliente Mixto")
                .descuentoPct(BigDecimal.ZERO)
                .build());

        // Archivo 1: dos grupos
        byte[] excel1 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G1 L1", 100.0, null, null, null, null, null, "Bodega 1", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G2 L1", 150.0, null, null, null, null, null, "Bodega 2", null, null, null)
                .bytes();

        aplicar(excel1, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrosNuevos").value(2));

        List<Documento> docs1 = documentoRepository.findAll();
        assertThat(docs1).hasSize(2);
        DocumentoResponse r1 = documentoService.obtener(docs1.get(0).getId());
        DocumentoResponse r2 = documentoService.obtener(docs1.get(1).getId());
        DocumentoResponse docG1 = r1.lineas().stream().anyMatch(l -> "Bodega 1".equals(l.lugarEntrega())) ? r1 : r2;
        DocumentoResponse docG2 = r1.lineas().stream().anyMatch(l -> "Bodega 2".equals(l.lugarEntrega())) ? r1 : r2;

        // Convertimos el Grupo 1 a Factura activa
        DocumentoResponse facG1 = documentoService.convertirHistorico(docG1.id());
        assertThat(facG1.tipo()).isEqualTo("FACTURA");
        assertThat(facG1.folio()).isEqualTo("FAC-0001");

        // El Grupo 2 sigue siendo HISTORICO
        assertThat(docG2.tipo()).isEqualTo("HISTORICO");

        // Archivo 2:
        // - G1 L1 (ya importada)
        // - G1 L2 (nueva en grupo convertido -> debe ignorarse)
        // - G2 L1 (ya importada)
        // - G2 L2 (nueva en grupo histórico existente -> debe agregarse)
        // - G3 L1 (grupo nuevo)
        byte[] excel2 = ExcelDePrueba.cotizaciones()
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G1 L1", 100.0, null, null, null, null, null, "Bodega 1", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G1 L2", 80.0, null, null, null, null, null, "Bodega 1", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G2 L1", 150.0, null, null, null, null, null, "Bodega 2", null, null, null)
                .fila(LocalDate.of(2026, 11, 1), 1, "Item G2 L2", 90.0, null, null, null, null, null, "Bodega 2", null, null, null)
                .fila(LocalDate.of(2026, 11, 2), 1, "Item G3 L1", 200.0, null, null, null, null, null, "Bodega 3", null, null, null)
                .bytes();

        vistaPrevia(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(5))
                .andExpect(jsonPath("$.filasYaImportadas").value(3))
                .andExpect(jsonPath("$.filasNuevas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(1))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(1));

        aplicar(excel2, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(5))
                .andExpect(jsonPath("$.filasYaImportadas").value(3))
                .andExpect(jsonPath("$.filasNuevas").value(2))
                .andExpect(jsonPath("$.registrosNuevos").value(1))
                .andExpect(jsonPath("$.registrosConLineasNuevas").value(1));

        // Verificamos estado final:
        // G1: FACTURA FAC-0001 sigue intacta con 1 sola línea de 100.00
        DocumentoResponse docG1Actualizado = documentoService.obtener(docG1.id());
        assertThat(docG1Actualizado.lineas()).hasSize(1);
        assertThat(docG1Actualizado.total()).isEqualByComparingTo("100.00");

        // G2: HISTORICO recibió su nueva línea y su total subió a 240.00
        DocumentoResponse docG2Actualizado = documentoService.obtener(docG2.id());
        assertThat(docG2Actualizado.lineas()).hasSize(2);
        assertThat(docG2Actualizado.total()).isEqualByComparingTo("240.00");

        // G3: Se creó nuevo histórico HIS-0003 con 1 línea de 200.00
        List<Documento> todos = documentoRepository.findAll();
        assertThat(todos).hasSize(3);
        DocumentoResponse docG3 = todos.stream()
                .map(d -> documentoService.obtener(d.getId()))
                .filter(d -> d.lineas().stream().anyMatch(l -> "Bodega 3".equals(l.lugarEntrega())))
                .findFirst().orElseThrow();
        assertThat(docG3.folio()).isEqualTo("HIS-0003");
        assertThat(docG3.tipo()).isEqualTo("HISTORICO");
        assertThat(docG3.estado()).isEqualTo("HISTORICA");
        assertThat(docG3.total()).isEqualByComparingTo("200.00");
    }
}
