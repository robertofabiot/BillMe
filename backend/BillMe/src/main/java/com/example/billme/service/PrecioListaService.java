package com.example.billme.service;

import com.example.billme.dto.PrecioListaRequest;
import com.example.billme.dto.PrecioListaResponse;
import com.example.billme.dto.PreciosListaProductoResponse;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.model.PrecioLista;
import com.example.billme.model.Producto;
import com.example.billme.repository.PrecioListaRepository;
import com.example.billme.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Precios de lista del proveedor (sin IVA) y el costo que se deriva de ellos. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecioListaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PrecioListaRepository precioListaRepository;
    private final ProductoRepository productoRepository;
    private final ConfiguracionService configuracionService;

    public PreciosListaProductoResponse consultar(UUID productoId) {
        Producto producto = buscarProducto(productoId);
        List<PrecioListaResponse> historial = precioListaRepository
                .findByProductoIdOrderByFechaDescCreatedAtDesc(productoId).stream()
                .map(this::toResponse)
                .toList();
        return new PreciosListaProductoResponse(
                historial.isEmpty() ? null : historial.get(0),
                historial,
                descuentoProveedorDeFamilia(producto).orElse(null));
    }

    /**
     * Precio de lista sin IVA que corresponde a un precio conocido:
     * <ul>
     *   <li>Factura del proveedor (sin IVA): lista = precio / (1 − descuento)</li>
     *   <li>Precio a un cliente (con IVA): lista = precio / (1 + IVA) / (1 − descuento del cliente)</li>
     *   <li>Manual: el precio tal cual</li>
     * </ul>
     */
    public BigDecimal calcular(FuentePrecio fuente, BigDecimal precio, BigDecimal descuentoPct) {
        BigDecimal sinDescuento = BigDecimal.ONE.subtract(
                (descuentoPct != null ? descuentoPct : BigDecimal.ZERO).divide(CIEN, MathContext.DECIMAL64));
        BigDecimal lista = switch (fuente) {
            case FACTURA_PROVEEDOR -> precio.divide(sinDescuento, MathContext.DECIMAL64);
            case PRECIO_CLIENTE -> precio
                    .divide(configuracionService.factorIva(), MathContext.DECIMAL64)
                    .divide(sinDescuento, MathContext.DECIMAL64);
            case MANUAL -> precio;
            case LISTA_IMPORTADA -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los precios de la lista del proveedor se cargan con el importador");
        };
        return lista.setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional
    public PrecioListaResponse registrar(UUID productoId, PrecioListaRequest request) {
        Producto producto = buscarProducto(productoId);
        BigDecimal lista = calcular(request.fuente(), request.precio(), request.descuentoPct());
        boolean derivado = request.fuente() != FuentePrecio.MANUAL;
        PrecioLista guardado = precioListaRepository.save(PrecioLista.builder()
                .producto(producto)
                .precioSinIva(lista)
                .fecha(request.fecha() != null ? request.fecha() : LocalDate.now())
                .fuente(request.fuente())
                .descuentoPct(derivado ? request.descuentoPct() : null)
                .precioOrigen(derivado ? request.precio() : null)
                .nota(request.nota())
                .build());
        return toResponse(guardado);
    }

    public Optional<PrecioLista> vigente(UUID productoId) {
        return precioListaRepository.findFirstByProductoIdOrderByFechaDescCreatedAtDesc(productoId);
    }

    public Map<UUID, PrecioLista> vigentes(Collection<UUID> productoIds) {
        if (productoIds.isEmpty()) {
            return Map.of();
        }
        return precioListaRepository.vigentes(productoIds).stream()
                .collect(Collectors.toMap(p -> p.getProducto().getId(), Function.identity()));
    }

    private Producto buscarProducto(UUID productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private PrecioListaResponse toResponse(PrecioLista p) {
        return new PrecioListaResponse(p.getId(), p.getPrecioSinIva(), p.getFecha(), p.getFuente().name(),
                p.getDescuentoPct(), p.getPrecioOrigen(), p.getNota());
    }

    @Transactional
    public void eliminarDeProducto(UUID productoId) {
        precioListaRepository.deleteByProductoId(productoId);
    }

    /** Descuento del proveedor que se usó por última vez en la familia del producto, si hay. */
    public Optional<BigDecimal> descuentoProveedorDeFamilia(Producto producto) {
        if (producto.getFamilia() == null) {
            return Optional.empty();
        }
        return precioListaRepository
                .findFirstByProductoFamiliaAndFuenteAndDescuentoPctNotNullOrderByFechaDescCreatedAtDesc(
                        producto.getFamilia(), FuentePrecio.FACTURA_PROVEEDOR)
                .map(PrecioLista::getDescuentoPct);
    }

    /**
     * Costo estimado con IVA: lista × (1 − descuento del proveedor) × (1 + IVA).
     * Vacío si no hay lista vigente o no se conoce el descuento.
     */
    public Optional<BigDecimal> costoEstimadoConIva(Producto producto) {
        Optional<PrecioLista> vigente = vigente(producto.getId());
        if (vigente.isEmpty()) {
            return Optional.empty();
        }
        PrecioLista lista = vigente.get();
        Optional<BigDecimal> descuento = lista.getFuente() == FuentePrecio.FACTURA_PROVEEDOR
                ? Optional.ofNullable(lista.getDescuentoPct())
                : descuentoProveedorDeFamilia(producto);
        return descuento.map(d -> lista.getPrecioSinIva()
                .multiply(BigDecimal.ONE.subtract(d.divide(CIEN)))
                .multiply(configuracionService.factorIva())
                .setScale(2, RoundingMode.HALF_UP));
    }
}
