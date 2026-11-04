package com.example.billme.controller;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProyectoRequest;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.service.ClienteService;
import com.example.billme.service.DocumentoService;
import com.example.billme.service.ProductoService;
import com.example.billme.service.ProyectoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ClienteControllerTest extends IntegrationTest {

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    ProductoService productoService;

    @Autowired
    DocumentoService documentoService;

    @Test
    void crearSinNombreResponde400ConElCampoInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\": \"88888888\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }

    @Test
    void cuerpoMalformadoResponde400ConMensaje() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void clienteInexistenteResponde404ConMensaje() throws Exception {
        mockMvc.perform(get("/api/clientes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cliente no encontrado"));
    }

    @Test
    void borrarClienteConFacturasResponde409YNoBorraNada() throws Exception {
        var cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        var producto = productoService.crear(new ProductoRequest(
                "009001271", "Tubo Estruct. Cua. 4 x 4", null, "UND", null, List.of()));
        documentoService.crear(new DocumentoRequest(TipoDocumento.FACTURA, cliente.id(), null, LocalDate.of(2026, 10, 1),
                null, false, List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null,
                new BigDecimal("2000.00"), null, null, null, null))));

        mockMvc.perform(delete("/api/clientes/{id}", cliente.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("facturas")));

        assertThat(documentoService.filtrar(null, null, cliente.id(), null, null, null, null)).hasSize(1);
        mockMvc.perform(get("/api/clientes/{id}", cliente.id())).andExpect(status().isOk());
    }

    @Test
    void borrarClienteSinFacturasBorraTambienSusProyectos() throws Exception {
        var cliente = clienteService.crear(new ClienteRequest("Cliente nuevo", null, null));
        proyectoService.crear(new ProyectoRequest(cliente.id(), "Obra", null));

        mockMvc.perform(delete("/api/clientes/{id}", cliente.id()))
                .andExpect(status().isNoContent());

        assertThat(proyectoService.listarTodos()).isEmpty();
    }
}
