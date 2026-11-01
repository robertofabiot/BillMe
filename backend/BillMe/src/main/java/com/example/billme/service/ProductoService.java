package com.example.billme.service;

import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.model.AliasProducto;
import com.example.billme.model.PrecioLista;
import com.example.billme.model.Producto;
import com.example.billme.repository.AliasProductoRepository;
import com.example.billme.repository.DetalleFacturaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.util.NormalizadorTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AliasProductoRepository aliasProductoRepository;
    private final DetalleFacturaRepository detalleFacturaRepository;
    private final PrecioListaService precioListaService;

    /** Los 100 productos usados o editados más recientemente; el resto se encuentra con el buscador. */
    public List<ProductoResponse> listarRecientes() {
        return toResponses(productoRepository.findTop100ByOrderByUpdatedAtDesc());
    }

    /** Encuentra un producto aunque esté escrito distinto ("tubo 2x1.5" encuentra "Tubo Estruct. Cua. 2\" x 1.50mm"). */
    public List<ProductoResponse> buscar(String q) {
        String normalizado = NormalizadorTexto.normalizar(q);
        if (normalizado.isEmpty()) {
            return listarRecientes();
        }
        String codigo = q.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
        return toResponses(productoRepository.buscar(normalizado, "%" + normalizado + "%", codigo + "%"));
    }

    public ProductoResponse obtenerPorId(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        return toResponses(List.of(producto)).get(0);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        aplicar(producto, request);
        producto = productoRepository.save(producto);

        if (request.aliases() != null && !request.aliases().isEmpty()) {
            for (String nombreAlias : request.aliases()) {
                AliasProducto alias = new AliasProducto();
                alias.setNombreAlias(nombreAlias);
                alias.setProducto(producto);
                aliasProductoRepository.save(alias);
            }
        }

        return obtenerPorId(producto.getId());
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        aplicar(producto, request);
        productoRepository.save(producto);

        aliasProductoRepository.deleteByProductoId(id);

        if (request.aliases() != null && !request.aliases().isEmpty()) {
            for (String nombreAlias : request.aliases()) {
                AliasProducto alias = new AliasProducto();
                alias.setNombreAlias(nombreAlias);
                alias.setProducto(producto);
                aliasProductoRepository.save(alias);
            }
        }

        return obtenerPorId(id);
    }

    @Transactional
    public void eliminar(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (producto.isGenerico()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Los productos genéricos no se pueden eliminar");
        }
        if (detalleFacturaRepository.existsByProductoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un producto que aparece en facturas");
        }
        precioListaService.eliminarDeProducto(id);
        productoRepository.deleteById(id);
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setCodigoInterno(request.codigoInterno().trim());
        producto.setNombrePrincipal(request.nombrePrincipal().trim());
        producto.setDescripcion(request.descripcion());
        producto.setUnidad(request.unidad() == null || request.unidad().isBlank()
                ? "UND" : request.unidad().trim().toUpperCase());
        producto.setPeso(request.peso());
    }

    private List<ProductoResponse> toResponses(List<Producto> productos) {
        Map<UUID, PrecioLista> vigentes = precioListaService.vigentes(
                productos.stream().map(Producto::getId).toList());
        return productos.stream().map(p -> toResponse(p, vigentes.get(p.getId()))).toList();
    }

    private ProductoResponse toResponse(Producto entity, PrecioLista vigente) {
        List<String> aliases = List.of();
        if (entity.getAliases() != null) {
            aliases = entity.getAliases().stream()
                    .map(AliasProducto::getNombreAlias)
                    .toList();
        }

        return new ProductoResponse(
                entity.getId(),
                entity.getCodigoInterno(),
                entity.getNombrePrincipal(),
                entity.getDescripcion(),
                entity.getUnidad(),
                entity.getFamilia(),
                entity.getPeso(),
                entity.isGenerico(),
                entity.getFormaCalculo().name(),
                vigente != null ? vigente.getPrecioSinIva() : null,
                vigente != null ? vigente.getFecha() : null,
                aliases,
                entity.getCreatedAt()
        );
    }
}
