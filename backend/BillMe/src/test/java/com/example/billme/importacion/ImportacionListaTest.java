package com.example.billme.importacion;

import com.example.billme.dto.ProductoRequest;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.service.ProductoService;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
class ImportacionListaTest extends IntegrationTest {

    @Autowired
    ProductoRepository productoRepository;

    @Autowired
    PrecioListaRepository precioListaRepository;

    @Autowired
    ProductoService productoService;

    private static byte[] lista(double precioPerlin) {
        return ExcelDePrueba.listaDelProveedor()
                .fila("001001169", "Perlin Negro 2 x  8 x 4.50mm Segunda", "UND", 65.95, precioPerlin)
                .fila("024005084", "Lamina Lisa Prepintada 914 x 3050 x 0.55mm", "UND", 12.25, 850.00)
                .fila("009001999", "Tubo sin precio todavía", "Metro", null, null)
                .bytes();
    }

    @Test
    void laVistaPreviaNoGuardaNada() throws Exception {
        vistaPrevia(lista(4100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoja").value("lista_precios"))
                .andExpect(jsonPath("$.filasLeidas").value(3))
                .andExpect(jsonPath("$.productosNuevos").value(3))
                .andExpect(jsonPath("$.sinPrecio").value(1))
                .andExpect(jsonPath("$.aplicada").value(false))
                .andExpect(jsonPath("$.muestra.length()").value(3));

        assertThat(productoRepository.findByCodigoInterno("001001169")).isEmpty();
    }

    @Test
    void aplicarCreaProductosConSuPrecioDeLista() throws Exception {
        aplicar(lista(4100), "2026-10-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true))
                .andExpect(jsonPath("$.productosNuevos").value(3));

        var perlin = productoService.buscar("001001169").get(0);
        assertThat(perlin.nombrePrincipal()).isEqualTo("Perlin Negro 2 x 8 x 4.50mm Segunda");
        assertThat(perlin.familia()).isEqualTo("001");
        assertThat(perlin.peso()).isEqualByComparingTo("65.95");
        assertThat(perlin.precioListaVigente()).isEqualByComparingTo("4100");
        assertThat(perlin.fechaPrecioLista()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(productoService.buscar("009001999").get(0).unidad()).isEqualTo("METRO");
        assertThat(jdbc.queryForObject("select pr.nombre from productos p join proveedores pr on pr.id = p.proveedor_id "
                + "where p.codigo_interno = '001001169'", String.class)).isEqualTo("Proveedor principal");
        assertThat(precioListaRepository.count()).isEqualTo(2);
    }

    @Test
    void importarDosVecesElMismoArchivoNoDuplicaNada() throws Exception {
        aplicar(lista(4100), "2026-10-01");

        aplicar(lista(4100), "2026-10-15")
                .andExpect(jsonPath("$.productosNuevos").value(0))
                .andExpect(jsonPath("$.preciosQueCambian").value(0))
                .andExpect(jsonPath("$.sinCambios").value(2))
                .andExpect(jsonPath("$.muestra.length()").value(0));

        assertThat(precioListaRepository.count()).isEqualTo(2);
        assertThat(productoRepository.findByCodigoInternoIn(List.of("001001169", "024005084", "009001999")))
                .hasSize(3);
    }

    @Test
    void soloRegistraLosPreciosQueCambian() throws Exception {
        aplicar(lista(4100), "2026-10-01");

        aplicar(lista(4250), "2026-11-01")
                .andExpect(jsonPath("$.preciosQueCambian").value(1))
                .andExpect(jsonPath("$.sinCambios").value(1))
                .andExpect(jsonPath("$.muestra[0].codigo").value("001001169"))
                .andExpect(jsonPath("$.muestra[0].precioAnterior").value(4100))
                .andExpect(jsonPath("$.muestra[0].precioNuevo").value(4250));

        assertThat(precioListaRepository.count()).isEqualTo(3);
        assertThat(productoService.buscar("001001169").get(0).precioListaVigente()).isEqualByComparingTo("4250");
    }

    @Test
    void noCambiaElNombreDeUnProductoQueYaExiste() throws Exception {
        var creado = productoService.crear(new ProductoRequest(
                "001001169", "Perlin 2x8 negro (segunda)", null, null, null, List.of()));

        aplicar(lista(4100), "2026-10-01");

        var perlin = productoService.obtenerPorId(creado.id());
        assertThat(perlin.nombrePrincipal()).isEqualTo("Perlin 2x8 negro (segunda)");
        assertThat(perlin.peso()).isEqualByComparingTo("65.95");
        assertThat(perlin.precioListaVigente()).isEqualByComparingTo("4100");
    }

    @Test
    void rechazaUnArchivoQueNoEsLaLista() throws Exception {
        vistaPrevia("no es un excel".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    private ResultActions vistaPrevia(byte[] contenido) throws Exception {
        return mockMvc.perform(multipart("/api/importaciones/lista/vista-previa").file(archivo(contenido)));
    }

    private ResultActions aplicar(byte[] contenido, String fecha) throws Exception {
        return mockMvc.perform(multipart("/api/importaciones/lista/aplicar").file(archivo(contenido)).param("fecha", fecha));
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "lista.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido);
    }
}
