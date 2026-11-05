package com.example.billme.controller;

import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.dto.DocumentoRequest;
import com.example.billme.dto.LineaRequest;
import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.service.ClienteService;
import com.example.billme.service.DocumentoService;
import com.example.billme.service.ProductoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ReporteControllerTest extends IntegrationTest {

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProductoService productoService;

    @Test
    void consultarGananciasPorHttp() throws Exception {
        ClienteResponse cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        ProductoResponse producto = productoService.crear(new ProductoRequest("009001271", "Tubo", null, null,
                BigDecimal.TEN, List.of()));

        documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 10, 15),
                null,
                false,
                List.of(new LineaRequest(null, producto.id(), null, BigDecimal.ONE, null,
                        new BigDecimal("100.00"), null, null, null, null))
        ));

        mockMvc.perform(get("/api/reportes/ganancias")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facturado").value(100.00))
                .andExpect(jsonPath("$.facturas", hasSize(1)))
                .andExpect(jsonPath("$.facturas[0].clienteNombre").value("Cliente de prueba"));
    }
}
