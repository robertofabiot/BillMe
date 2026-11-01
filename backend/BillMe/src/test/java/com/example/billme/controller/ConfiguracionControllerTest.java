package com.example.billme.controller;

import com.example.billme.dto.ConfiguracionRequest;
import com.example.billme.service.ConfiguracionService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ConfiguracionControllerTest extends IntegrationTest {

    @Autowired
    ConfiguracionService configuracionService;

    @AfterEach
    void restaurarValoresIniciales() {
        configuracionService.actualizar(new ConfiguracionRequest(
                new BigDecimal("15"), new BigDecimal("36.6243"), new BigDecimal("1.80"),
                new BigDecimal("0.129"), 365));
    }

    @Test
    void devuelveLosValoresDelNegocio() throws Exception {
        mockMvc.perform(get("/api/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ivaPct").value(15))
                .andExpect(jsonPath("$.tipoCambio").value(36.6243))
                .andExpect(jsonPath("$.tarifaKgUsd").value(1.8))
                .andExpect(jsonPath("$.densidadKgIn3").value(0.129))
                .andExpect(jsonPath("$.diasPrecioViejo").value(365));
        assertThat(configuracionService.factorIva()).isEqualByComparingTo("1.15");
    }

    @Test
    void guardaLosCambios() throws Exception {
        mockMvc.perform(put("/api/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ivaPct": 15, "tipoCambio": 36.7, "tarifaKgUsd": 1.9,
                                 "densidadKgIn3": 0.129, "diasPrecioViejo": 180}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaKgUsd").value(1.9));

        assertThat(configuracionService.tipoCambio()).isEqualByComparingTo("36.7");
        assertThat(configuracionService.diasPrecioViejo()).isEqualTo(180);
    }

    @Test
    void rechazaValoresFueraDeRango() throws Exception {
        mockMvc.perform(put("/api/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ivaPct": 150, "tipoCambio": 36.6243, "tarifaKgUsd": 1.8,
                                 "densidadKgIn3": 0.129, "diasPrecioViejo": 365}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("ivaPct"));
    }
}
