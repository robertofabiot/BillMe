package com.example.billme.config;

import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Arranca con el perfil prod: chequeo de salud y CORS tal como los usa el frontend desplegado. */
@ActiveProfiles("prod")
class ProduccionTest extends IntegrationTest {

    private static final String FRONTEND = "http://localhost:5173";

    @Test
    void laSaludRespondeSinIniciarSesion() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void laSaludPermiteConsultasDesdeElFrontend() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    void losDemasEndpointsDeActuatorNoSeExponen() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laApiAceptaElPreflightDelFrontend() throws Exception {
        mockMvc.perform(options("/api/clientes")
                        .header("Origin", FRONTEND)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    void laApiRechazaOrigenesDesconocidos() throws Exception {
        mockMvc.perform(options("/api/clientes")
                        .header("Origin", "https://sitio-ajeno.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
