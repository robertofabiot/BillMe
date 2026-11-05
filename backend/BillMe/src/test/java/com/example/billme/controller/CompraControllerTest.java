package com.example.billme.controller;

import com.example.billme.dto.*;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.ProveedorRepository;
import com.example.billme.service.ClienteService;
import com.example.billme.service.DocumentoService;
import com.example.billme.service.ProductoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class CompraControllerTest extends IntegrationTest {

    @Autowired
    DocumentoService documentoService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProductoService productoService;

    @Autowired
    ProveedorRepository proveedorRepository;

    ClienteResponse cliente;
    ProductoResponse tubo;
    Proveedor proveedor;
    DocumentoResponse factura;

    @BeforeEach
    void setup() {
        cliente = clienteService.crear(new ClienteRequest("Cliente de prueba", null, null));
        tubo = productoService.crear(new ProductoRequest("009001271", "Tubo Estruct. Cua. 4 x 4", null, null,
                new BigDecimal("44.17"), List.of()));
        proveedor = proveedorRepository.findByNombre("Proveedor principal").orElseThrow();
        factura = documentoService.crear(new DocumentoRequest(
                TipoDocumento.FACTURA,
                cliente.id(),
                null,
                LocalDate.of(2026, 10, 15),
                null,
                false,
                List.of(new LineaRequest(null, tubo.id(), null, new BigDecimal("2"), null,
                        new BigDecimal("1300.00"), null, null, null, null))
        ));
    }

    @Test
    void registrarYListarComprasPorHttp() throws Exception {
        String lineaId = factura.lineas().get(0).id().toString();
        String json = """
                {
                    "proveedorId": "%s",
                    "fecha": "2026-10-15",
                    "numeroFactura": "FAC-PROV-1",
                    "nota": "Compra por mayor",
                    "lineas": [
                        {
                            "lineaId": "%s",
                            "precioSinIva": 950.00,
                            "descuentoPct": 10.0,
                            "pesoUnitKg": 45.0
                        }
                    ]
                }
                """.formatted(proveedor.getId(), lineaId);

        mockMvc.perform(post("/api/documentos/" + factura.id() + "/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.proveedorId").value(proveedor.getId().toString()))
                .andExpect(jsonPath("$.proveedorNombre").value("Proveedor principal"))
                .andExpect(jsonPath("$.numeroFactura").value("FAC-PROV-1"))
                .andExpect(jsonPath("$.nota").value("Compra por mayor"));

        mockMvc.perform(get("/api/documentos/" + factura.id() + "/compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroFactura").value("FAC-PROV-1"));
    }
}
