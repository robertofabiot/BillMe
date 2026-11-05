package com.example.billme.controller;

import com.example.billme.model.Proveedor;
import com.example.billme.repository.ProveedorRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ProveedorControllerTest extends IntegrationTest {

    @Autowired
    ProveedorRepository proveedorRepository;

    @Test
    void listarProveedoresActivosOrdenadosPorNombre() throws Exception {
        proveedorRepository.save(Proveedor.builder().nombre("Aceros del Norte").activo(true).build());
        proveedorRepository.save(Proveedor.builder().nombre("Z-Materiales").activo(true).build());
        proveedorRepository.save(Proveedor.builder().nombre("Inactivo").activo(false).build());

        // Debe listar en orden alfabético y excluir inactivos.
        // "Proveedor principal" viene de la migración.
        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombre").value("Aceros del Norte"))
                .andExpect(jsonPath("$[1].nombre").value("Proveedor principal"))
                .andExpect(jsonPath("$[2].nombre").value("Z-Materiales"));
    }

    @Test
    void crearProveedorResponde201() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ferretería Central\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Ferretería Central"));
    }

    @Test
    void crearProveedorConNombreRepetidoResponde409() throws Exception {
        // "Proveedor principal" ya existe
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"proveedor principal\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe ese proveedor"));

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"  PROVEEDOR PRINCIPAL  \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe ese proveedor"));
    }

    @Test
    void crearProveedorConNombreVacioResponde400() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }
}
