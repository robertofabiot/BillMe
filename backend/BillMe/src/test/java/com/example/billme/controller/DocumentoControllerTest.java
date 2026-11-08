package com.example.billme.controller;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.DocumentoResponse;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.ProyectoResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Cliente;
import com.example.billme.model.Documento;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.service.ClienteService;
import com.example.billme.service.DocumentoService;
import com.example.billme.service.ProductoService;
import com.example.billme.service.ProyectoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class DocumentoControllerTest extends IntegrationTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProyectoService proyectoService;

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
        cliente = clienteService.crear(new ClienteRequest("Cliente Base", null, null));
        producto = productoService.crear(new ProductoRequest("001", "Producto Base", null, null, null, List.of()));
    }

    @Test
    void tamanoDosConCincoDocumentosDaTresPaginasYCincoElementos() throws Exception {
        for (int i = 1; i <= 5; i++) {
            documentoService.crear(new DocumentoRequest(
                    TipoDocumento.FACTURA,
                    cliente.id(),
                    null,
                    LocalDate.of(2026, 10, i),
                    null,
                    false,
                    List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
            ));
        }

        // Página 0: 2 elementos de 5
        mockMvc.perform(get("/api/documentos")
                        .param("tamano", "2")
                        .param("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina", is(0)))
                .andExpect(jsonPath("$.totalPaginas", is(3)))
                .andExpect(jsonPath("$.totalElementos", is(5)))
                .andExpect(jsonPath("$.contenido", hasSize(2)));

        // Página 1: 2 elementos de 5
        mockMvc.perform(get("/api/documentos")
                        .param("tamano", "2")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina", is(1)))
                .andExpect(jsonPath("$.totalPaginas", is(3)))
                .andExpect(jsonPath("$.totalElementos", is(5)))
                .andExpect(jsonPath("$.contenido", hasSize(2)));

        // Página 2: 1 elemento de 5
        mockMvc.perform(get("/api/documentos")
                        .param("tamano", "2")
                        .param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina", is(2)))
                .andExpect(jsonPath("$.totalPaginas", is(3)))
                .andExpect(jsonPath("$.totalElementos", is(5)))
                .andExpect(jsonPath("$.contenido", hasSize(1)));
    }

    @Test
    void tamanoQuinientosSeLimitaADoscientos() throws Exception {
        Cliente clienteEntity = clienteRepository.findById(cliente.id()).orElseThrow();
        List<Documento> documentos = new ArrayList<>();
        for (int i = 1; i <= 205; i++) {
            documentos.add(Documento.builder()
                    .tipo(TipoDocumento.FACTURA)
                    .estado(EstadoDocumento.ACTIVA)
                    .folio(String.format("FAC-%04d", i))
                    .fecha(LocalDate.of(2026, 1, 1).plusDays(i % 200))
                    .cliente(clienteEntity)
                    .tipoCambio(new BigDecimal("36.6243"))
                    .total(new BigDecimal("10.00"))
                    .totalKg(BigDecimal.ZERO)
                    .build());
        }
        documentoRepository.saveAll(documentos);

        mockMvc.perform(get("/api/documentos")
                        .param("tamano", "500")
                        .param("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina", is(0)))
                .andExpect(jsonPath("$.totalElementos", is(205)))
                .andExpect(jsonPath("$.totalPaginas", is(2)))
                .andExpect(jsonPath("$.contenido", hasSize(200)));
    }

    @Test
    void qEncuentraPorFolioClienteYProyectoYFuncionaConDocumentosSinProyecto() throws Exception {
        ClienteResponse clienteSol = clienteService.crear(new ClienteRequest("Constructora del Sol", null, null));
        ClienteResponse clienteMar = clienteService.crear(new ClienteRequest("Inversiones Marítimas", null, null));

        ProyectoResponse proyectoHoriz = proyectoService.crear(new ProyectoRequest(clienteSol.id(), "Edificio Horizonte", null));

        // Doc 1: Cliente Sol, Proyecto Horizonte, Tipo COTIZACION
        DocumentoResponse doc1 = documentoService.crear(new DocumentoRequest(
                TipoDocumento.COTIZACION,
                clienteSol.id(),
                proyectoHoriz.id(),
                LocalDate.of(2026, 10, 1),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));

        // Doc 2: Cliente Marítimas, SIN PROYECTO, Tipo FACTURA
        DocumentoResponse doc2 = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                clienteMar.id(),
                null,
                LocalDate.of(2026, 10, 2),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("200"), null, null, null, null))
        ));

        // Doc 3: Cliente Sol, SIN PROYECTO, Tipo FACTURA
        DocumentoResponse doc3 = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                clienteSol.id(),
                null,
                LocalDate.of(2026, 10, 3),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("300"), null, null, null, null))
        ));

        // 1. Búsqueda por folio
        mockMvc.perform(get("/api/documentos").param("q", doc1.folio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].folio", is(doc1.folio())))
                .andExpect(jsonPath("$.contenido[0].proyectoNombre", is("Edificio Horizonte")));

        // 2. Búsqueda por cliente (funciona para documento sin proyecto)
        mockMvc.perform(get("/api/documentos").param("q", "marítimas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].folio", is(doc2.folio())))
                .andExpect(jsonPath("$.contenido[0].clienteNombre", is("Inversiones Marítimas")))
                .andExpect(jsonPath("$.contenido[0].proyectoId").doesNotExist());

        // 3. Búsqueda por proyecto
        mockMvc.perform(get("/api/documentos").param("q", "horizonte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.contenido[0].folio", is(doc1.folio())))
                .andExpect(jsonPath("$.contenido[0].proyectoNombre", is("Edificio Horizonte")));

        // 4. Búsqueda vacía incluye todos los documentos (con y sin proyecto)
        mockMvc.perform(get("/api/documentos").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(3)))
                .andExpect(jsonPath("$.contenido", hasSize(3)));
    }

    @Test
    void seMantieneElOrdenPorFechaDescendente() throws Exception {
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 20), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 12), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));

        mockMvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(3)))
                .andExpect(jsonPath("$.contenido[0].fecha", is("2026-10-20")))
                .andExpect(jsonPath("$.contenido[1].fecha", is("2026-10-12")))
                .andExpect(jsonPath("$.contenido[2].fecha", is("2026-10-05")));
    }

    @Test
    void convertirHistoricoEndpointRetorna200() throws Exception {
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));
        Documento his = documentoRepository.findById(fac.id()).orElseThrow();
        his.setTipo(TipoDocumento.HISTORICO);
        his.setEstado(EstadoDocumento.HISTORICA);
        his.setFolio("HIS-0001");
        documentoRepository.save(his);

        mockMvc.perform(post("/api/documentos/{id}/convertir-historico", his.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("FACTURA")))
                .andExpect(jsonPath("$.estado", is("ACTIVA")))
                .andExpect(jsonPath("$.folio", is("FAC-0002")));
    }

    @Test
    void reactivarEndpointRetorna200() throws Exception {
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));
        documentoService.anular(fac.id());

        mockMvc.perform(post("/api/documentos/{id}/reactivar", fac.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("ACTIVA")));
    }

    @Test
    void convertirHistoricoDocInexistenteDa404() throws Exception {
        mockMvc.perform(post("/api/documentos/{id}/convertir-historico", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void convertirHistoricoSobreFacturaDa409() throws Exception {
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));

        mockMvc.perform(post("/api/documentos/{id}/convertir-historico", fac.id()))
                .andExpect(status().isConflict());
    }

    @Test
    void reactivarDocInexistenteDa404() throws Exception {
        mockMvc.perform(post("/api/documentos/{id}/reactivar", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivarFacturaActivaDa409() throws Exception {
        DocumentoResponse fac = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));

        mockMvc.perform(post("/api/documentos/{id}/reactivar", fac.id()))
                .andExpect(status().isConflict());
    }

    @Test
    void crearDocumentoConTipoHistoricoDa400() throws Exception {
        String body = """
                {
                  "tipo": "HISTORICO",
                  "clienteId": "%s",
                  "fecha": "2026-10-05",
                  "lineas": [
                    { "productoId": "%s", "cantidad": 1, "precioUnit": 100 }
                  ]
                }
                """.formatted(cliente.id(), producto.id());

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtrarSinAcentosEncuentraDocumentosPorCliente() throws Exception {
        ClienteResponse clientePerez = clienteService.crear(new ClienteRequest("Cliente Pérez", null, null));
        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA, clientePerez.id(), null, LocalDate.of(2026, 10, 5), null, false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null, new BigDecimal("100"), null, null, null, null))
        ));

        mockMvc.perform(get("/api/documentos").param("q", "perez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].clienteNombre").value("Cliente Pérez"));

        mockMvc.perform(get("/api/documentos").param("q", "PÉREZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].clienteNombre").value("Cliente Pérez"));
    }

    @Test
    void cantidadMayorAlLimiteResponde400() throws Exception {
        String body = """
                {
                  "tipo": "FACTURA",
                  "clienteId": "%s",
                  "fecha": "2026-10-05",
                  "lineas": [
                    { "productoId": "%s", "cantidad": 12345678, "precioUnit": 100 }
                  ]
                }
                """.formatted(cliente.id(), producto.id());

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("lineas[0].cantidad"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Cantidad: tiene un valor muy alto"));
    }

    @Test
    void totalDeLineaDemasiadoGrandeResponde400() throws Exception {
        String body = """
                {
                  "tipo": "FACTURA",
                  "clienteId": "%s",
                  "fecha": "2026-10-05",
                  "lineas": [
                    { "productoId": "%s", "cantidad": 9999999, "precioUnit": 999999999 }
                  ]
                }
                """.formatted(cliente.id(), producto.id());

        mockMvc.perform(post("/api/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La línea 1 tiene un total demasiado grande"));
    }
}
