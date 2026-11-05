package com.example.billme.importacion;

import com.example.billme.dto.DocumentoResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        return mockMvc.perform(multipart("/api/importaciones/cotizaciones/aplicar")
                .file(archivo(contenido))
                .param("clienteId", clienteId.toString()));
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
                .andExpect(jsonPath("$.facturasNuevas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(3))
                .andExpect(jsonPath("$.aplicada").value(true));

        List<Documento> docs = documentoRepository.findAll();
        assertThat(docs).hasSize(2);

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
                .andExpect(jsonPath("$.facturasNuevas").value(1))
                .andExpect(jsonPath("$.filasNuevas").value(2));

        assertThat(documentoRepository.count()).isEqualTo(1);
        assertThat(documentoLineaRepository.count()).isEqualTo(2);

        aplicar(excel, cliente.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(2))
                .andExpect(jsonPath("$.filasNuevas").value(0))
                .andExpect(jsonPath("$.filasYaImportadas").value(2))
                .andExpect(jsonPath("$.facturasNuevas").value(0))
                .andExpect(jsonPath("$.facturasConLineasNuevas").value(0));

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
                .andExpect(jsonPath("$.facturasNuevas").value(1))
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
                .andExpect(jsonPath("$.facturasNuevas").value(0))
                .andExpect(jsonPath("$.facturasConLineasNuevas").value(1));

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
                .andExpect(jsonPath("$.facturasNuevas").value(1));

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
                .andExpect(jsonPath("$.facturasNuevas").value(1));

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
}
