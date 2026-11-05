package com.example.billme.controller;

import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class CalculoControllerTest extends IntegrationTest {

    @Test
    void calcularPesoPlacaResponde200ConPesoYDescripcion() throws Exception {
        mockMvc.perform(post("/api/calculos/peso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "forma": "PLACA",
                                  "ancho": { "texto": "10", "unidad": "PULGADA" },
                                  "largo": { "texto": "8", "unidad": "PULGADA" },
                                  "espesor": { "texto": "1/4", "unidad": "PULGADA" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoUnitKg").value(2.58))
                .andExpect(jsonPath("$.descripcion").value("Placa 10\" x 8\" x 1/4\""));
    }

    @Test
    void calcularPrecioPorPesoResponde200ConPrecioUnit() throws Exception {
        mockMvc.perform(post("/api/calculos/precio-por-peso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pesoUnitKg": 2.58,
                                  "tarifaUsd": 1.80
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnit").value(195.60));
    }

    @Test
    void calcularPrecioPorMetroResponde200ConPrecioUnit() throws Exception {
        mockMvc.perform(post("/api/calculos/precio-por-metro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precioReferencia": 2700.00,
                                  "largoReferencia": 6.75,
                                  "largoNuevo": 5.40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnit").value(2160.00));
    }

    @Test
    void calcularPesoAngularSinAla2Responde400() throws Exception {
        mockMvc.perform(post("/api/calculos/peso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "forma": "ANGULAR",
                                  "ancho": { "texto": "2", "unidad": "PULGADA" },
                                  "largo": { "texto": "6", "unidad": "METRO" },
                                  "espesor": { "texto": "3/16", "unidad": "PULGADA" }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
