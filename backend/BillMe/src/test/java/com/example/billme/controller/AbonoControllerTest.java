package com.example.billme.controller;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.ClienteRequest;
import com.example.billme.dto.ClienteResponse;
import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.ProyectoResponse;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.service.AbonoService;
import com.example.billme.service.ClienteService;
import com.example.billme.service.ProyectoService;
import com.example.billme.support.IntegrationTest;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
class AbonoControllerTest extends IntegrationTest {

    @Autowired
    AbonoService abonoService;

    @Autowired
    AbonoRepository abonoRepository;

    @Autowired
    ClienteService clienteService;

    @Autowired
    ProyectoService proyectoService;

    @Autowired
    JsonMapper jsonMapper;

    private ClienteResponse cliente1;
    private ClienteResponse cliente2;
    private ProyectoResponse proyectoCliente1;
    private ProyectoResponse proyectoCliente2;

    @BeforeEach
    void setup() {
        cliente1 = clienteService.crear(new ClienteRequest("Cliente Uno", "11111111", null));
        cliente2 = clienteService.crear(new ClienteRequest("Cliente Dos", "22222222", null));
        proyectoCliente1 = proyectoService.crear(new ProyectoRequest(cliente1.id(), "Obra 1", null));
        proyectoCliente2 = proyectoService.crear(new ProyectoRequest(cliente2.id(), "Obra 2", null));
    }

    @Test
    void crearAbonoExitoso() throws Exception {
        AbonoRequest request = new AbonoRequest(
                cliente1.id(),
                proyectoCliente1.id(),
                new BigDecimal("1500.50"),
                LocalDate.of(2026, 11, 5),
                "Transferencia bancaria"
        );

        mockMvc.perform(post("/api/abonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.clienteId").value(cliente1.id().toString()))
                .andExpect(jsonPath("$.proyectoId").value(proyectoCliente1.id().toString()))
                .andExpect(jsonPath("$.proyectoNombre").value("Obra 1"))
                .andExpect(jsonPath("$.monto").value(1500.50))
                .andExpect(jsonPath("$.fechaPago").value("2026-11-05"))
                .andExpect(jsonPath("$.nota").value("Transferencia bancaria"));

        assertThat(abonoRepository.count()).isEqualTo(1);
    }

    @Test
    void crearAbonoConProyectoDeOtroClienteResponde400() throws Exception {
        AbonoRequest request = new AbonoRequest(
                cliente1.id(),
                proyectoCliente2.id(),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 11, 5),
                "Nota"
        );

        mockMvc.perform(post("/api/abonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El proyecto no pertenece a ese cliente"));
    }

    @Test
    void crearAbonoConMontoInvalidoOIncompletoResponde400() throws Exception {
        mockMvc.perform(post("/api/abonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": "%s",
                                  "monto": -50,
                                  "fechaPago": "2026-11-05"
                                }
                                """.formatted(cliente1.id())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/abonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "monto": 100
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarAbonoExitoso() throws Exception {
        var abono = abonoService.registrarAbono(new AbonoRequest(
                cliente1.id(),
                null,
                new BigDecimal("500.00"),
                LocalDate.of(2026, 11, 1),
                "Inicial"
        ));

        AbonoRequest editado = new AbonoRequest(
                cliente1.id(),
                proyectoCliente1.id(),
                new BigDecimal("800.00"),
                LocalDate.of(2026, 11, 2),
                "Corregido"
        );

        mockMvc.perform(put("/api/abonos/{id}", abono.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(editado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(abono.id().toString()))
                .andExpect(jsonPath("$.monto").value(800.00))
                .andExpect(jsonPath("$.proyectoNombre").value("Obra 1"))
                .andExpect(jsonPath("$.nota").value("Corregido"));
    }

    @Test
    void editarAbonoInexistenteResponde404() throws Exception {
        AbonoRequest request = new AbonoRequest(
                cliente1.id(),
                null,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 11, 1),
                null
        );

        mockMvc.perform(put("/api/abonos/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Abono no encontrado"));
    }

    @Test
    void borrarAbonoExitoso() throws Exception {
        var abono = abonoService.registrarAbono(new AbonoRequest(
                cliente1.id(),
                null,
                new BigDecimal("500.00"),
                LocalDate.of(2026, 11, 1),
                null
        ));

        mockMvc.perform(delete("/api/abonos/{id}", abono.id()))
                .andExpect(status().isNoContent());

        assertThat(abonoRepository.existsById(abono.id())).isFalse();
    }

    @Test
    void borrarAbonoInexistenteResponde404() throws Exception {
        mockMvc.perform(delete("/api/abonos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
