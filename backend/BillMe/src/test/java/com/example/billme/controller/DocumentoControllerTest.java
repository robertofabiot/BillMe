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
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
