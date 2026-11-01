package com.example.billme.service;

import com.example.billme.enums.FuentePrecio;
import com.example.billme.model.PrecioLista;
import com.example.billme.model.Producto;
import com.example.billme.repository.PrecioListaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
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
    private final ConfiguracionService configuracionService;

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
