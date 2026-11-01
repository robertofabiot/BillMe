package com.example.billme.service;

import com.example.billme.dto.ImportacionListaResponse;
import com.example.billme.dto.ImportacionListaResponse.Cambio;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.importacion.FilaListaPrecios;
import com.example.billme.importacion.LecturaListaPrecios;
import com.example.billme.importacion.ListaPreciosLector;
import com.example.billme.model.PrecioLista;
import com.example.billme.model.Producto;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Importa la lista de precios del proveedor principal. Se puede repetir cuantas veces
 * se quiera: solo crea los productos que faltan y solo registra un precio nuevo cuando
 * difiere del vigente. A los productos existentes no se les cambia el nombre.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportacionListaService {

    private static final String PROVEEDOR = "Proveedor principal";
    private static final int TAMANO_MUESTRA = 50;
    private static final int TAMANO_LOTE = 1000;

    private final ListaPreciosLector lector;
    private final ProductoRepository productoRepository;
    private final PrecioListaRepository precioListaRepository;
    private final ProveedorRepository proveedorRepository;
    private final PrecioListaService precioListaService;

    /** Qué pasaría al importar, sin guardar nada. */
    public ImportacionListaResponse vistaPrevia(InputStream archivo) {
        return procesar(lector.leer(archivo), null);
    }

    @Transactional
    public ImportacionListaResponse aplicar(InputStream archivo, LocalDate fecha) {
        return procesar(lector.leer(archivo), fecha != null ? fecha : LocalDate.now());
    }

    /** Con {@code fecha} null solo analiza; con fecha también guarda. */
    private ImportacionListaResponse procesar(LecturaListaPrecios lectura, LocalDate fecha) {
        boolean guardar = fecha != null;
        Map<String, Producto> existentes = productosPorCodigo(lectura.filas());
        Map<UUID, PrecioLista> vigentes = vigentes(existentes.values());
        Proveedor proveedor = guardar ? proveedorRepository.findByNombre(PROVEEDOR).orElseThrow() : null;

        List<Producto> productosAGuardar = new ArrayList<>();
        List<PrecioLista> preciosAGuardar = new ArrayList<>();
        List<Cambio> muestra = new ArrayList<>();
        int nuevos = 0, cambian = 0, sinCambios = 0, sinPrecio = 0;

        for (FilaListaPrecios fila : lectura.filas()) {
            Producto producto = existentes.get(fila.codigo());
            PrecioLista vigente = producto != null ? vigentes.get(producto.getId()) : null;
            boolean esNuevo = producto == null;

            if (esNuevo) {
                nuevos++;
            }
            if (fila.precio() == null) {
                sinPrecio++;
            } else if (vigente != null && vigente.getPrecioSinIva().compareTo(fila.precio()) == 0) {
                sinCambios++;
            } else if (!esNuevo) {
                cambian++;
            }
            boolean registrarPrecio = fila.precio() != null
                    && (vigente == null || vigente.getPrecioSinIva().compareTo(fila.precio()) != 0);
            if ((esNuevo || registrarPrecio) && muestra.size() < TAMANO_MUESTRA) {
                muestra.add(new Cambio(fila.codigo(), fila.descripcion(),
                        vigente != null ? vigente.getPrecioSinIva() : null, fila.precio()));
            }

            if (guardar) {
                if (esNuevo) {
                    producto = Producto.builder()
                            .codigoInterno(fila.codigo())
                            .nombrePrincipal(recortar(fila.descripcion(), 200))
                            .build();
                }
                producto.setUnidad(recortar(fila.unidad(), 20));
                if (fila.peso() != null) {
                    producto.setPeso(fila.peso());
                }
                if (producto.getProveedor() == null) {
                    producto.setProveedor(proveedor);
                }
                productosAGuardar.add(producto);
                if (registrarPrecio) {
                    preciosAGuardar.add(PrecioLista.builder()
                            .producto(producto)
                            .precioSinIva(fila.precio())
                            .fecha(fecha)
                            .fuente(FuentePrecio.LISTA_IMPORTADA)
                            .nota("Importado de la hoja " + lectura.hoja())
                            .build());
                }
            }
        }

        if (guardar) {
            productoRepository.saveAll(productosAGuardar);
            precioListaRepository.saveAll(preciosAGuardar);
        }
        return new ImportacionListaResponse(lectura.hoja(), lectura.filas().size(), lectura.filasIgnoradas(),
                nuevos, cambian, sinCambios, sinPrecio, muestra, guardar);
    }

    private Map<String, Producto> productosPorCodigo(List<FilaListaPrecios> filas) {
        List<String> codigos = filas.stream().map(FilaListaPrecios::codigo).toList();
        Map<String, Producto> productos = new HashMap<>();
        for (int i = 0; i < codigos.size(); i += TAMANO_LOTE) {
            productoRepository.findByCodigoInternoIn(codigos.subList(i, Math.min(i + TAMANO_LOTE, codigos.size())))
                    .forEach(p -> productos.put(p.getCodigoInterno(), p));
        }
        return productos;
    }

    private Map<UUID, PrecioLista> vigentes(Collection<Producto> productos) {
        List<UUID> ids = productos.stream().map(Producto::getId).toList();
        Map<UUID, PrecioLista> vigentes = new HashMap<>();
        for (int i = 0; i < ids.size(); i += TAMANO_LOTE) {
            vigentes.putAll(precioListaService.vigentes(ids.subList(i, Math.min(i + TAMANO_LOTE, ids.size()))));
        }
        return vigentes;
    }

    private static String recortar(String texto, int largo) {
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
