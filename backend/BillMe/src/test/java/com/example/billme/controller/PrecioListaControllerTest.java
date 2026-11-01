package com.example.billme.controller;

import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.service.ProductoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Casos con números redondos para poder comprobar las fórmulas a mano. */
@WithMockUser
class PrecioListaControllerTest extends IntegrationTest {

    @Autowired
    ProductoService productoService;

    ProductoResponse tubo;

    @BeforeEach
    void crearProducto() {
        tubo = productoService.crear(new ProductoRequest(
                "009001045", "Tubo Cañeria Galv. 1 x 2.00mm", null, null, null, List.of()));
    }

    @Test
    void desdeLaFacturaDelProveedor() throws Exception {
        // 840.00 sin IVA con 16 % de descuento → lista 1,000.00
        calcular("""
                {"fuente": "FACTURA_PROVEEDOR", "precio": 840.00, "descuentoPct": 16}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioSinIva").value(closeTo(1000.0, 0.0001)));
    }

    @Test
    void desdeElPrecioDadoAUnCliente() throws Exception {
        // 1,104.00 con IVA y 4 % de descuento del cliente → lista 1,000.00
        calcular("""
                {"fuente": "PRECIO_CLIENTE", "precio": 1104.00, "descuentoPct": 4}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioSinIva").value(closeTo(1000.0, 0.0001)));
    }

    @Test
    void registrarGuardaHistorialYPasaASerElVigente() throws Exception {
        registrar("""
                {"fuente": "MANUAL", "precio": 450, "fecha": "2025-03-01"}""")
                .andExpect(status().isCreated());
        registrar("""
                {"fuente": "FACTURA_PROVEEDOR", "precio": 840.00, "descuentoPct": 16, "fecha": "2025-06-01",
                 "nota": "Factura 1234"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioOrigen").value(840.0))
                .andExpect(jsonPath("$.descuentoPct").value(16));

        mockMvc.perform(get("/api/productos/{id}/precios-lista", tubo.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente.precioSinIva").value(closeTo(1000.0, 0.0001)))
                .andExpect(jsonPath("$.vigente.fuente").value("FACTURA_PROVEEDOR"))
                .andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.descuentoProveedorSugerido").value(16));
        mockMvc.perform(get("/api/productos/{id}", tubo.id()))
                .andExpect(jsonPath("$.precioListaVigente").value(closeTo(1000.0, 0.0001)));
    }

    @Test
    void elDescuentoSugeridoSaleDeOtroProductoDeLaMismaFamilia() throws Exception {
        registrar("""
                {"fuente": "FACTURA_PROVEEDOR", "precio": 900.00, "descuentoPct": 18.5}""")
                .andExpect(status().isCreated());
        ProductoResponse otroTubo = productoService.crear(new ProductoRequest(
                "009001271", "Tubo Estruct. Cua. 4 x 4", null, null, null, List.of()));

        mockMvc.perform(get("/api/productos/{id}/precios-lista", otroTubo.id()))
                .andExpect(jsonPath("$.vigente").doesNotExist())
                .andExpect(jsonPath("$.descuentoProveedorSugerido").value(18.5));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        calcular("""
                {"fuente": "FACTURA_PROVEEDOR", "precio": -5, "descuentoPct": 100}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
        calcular("""
                {"fuente": "LISTA_IMPORTADA", "precio": 100}""")
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/productos/{id}/precios-lista", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private ResultActions calcular(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/productos/{id}/precios-lista/calcular", tubo.id())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions registrar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/productos/{id}/precios-lista", tubo.id())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
