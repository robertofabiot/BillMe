package com.example.billme.service;

import com.example.billme.dto.ProductoRequest;
import com.example.billme.dto.ProductoResponse;
import com.example.billme.model.AliasProducto;
import com.example.billme.model.Producto;
import com.example.billme.repository.AliasProductoRepository;
import com.example.billme.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AliasProductoRepository aliasProductoRepository;

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductoResponse> buscar(String q) {
        return productoRepository.buscar(q).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductoResponse obtenerPorId(UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setCodigoInterno(request.codigoInterno());
        producto.setNombrePrincipal(request.nombrePrincipal());
        producto.setDescripcion(request.descripcion());
        producto.setPeso(request.peso());
        producto.setPrecioListaProveedor(request.precioListaProveedor());
        producto.setDescuentoProveedor(request.descuentoProveedor());
        producto.setCostoNeto(request.costoNeto());

        producto = productoRepository.save(producto);

        if (request.aliases() != null && !request.aliases().isEmpty()) {
            for (String nombreAlias : request.aliases()) {
                AliasProducto alias = new AliasProducto();
                alias.setNombreAlias(nombreAlias);
                alias.setProducto(producto);
                aliasProductoRepository.save(alias);
            }
        }

        return toResponse(productoRepository.findById(producto.getId()).orElseThrow());
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        producto.setCodigoInterno(request.codigoInterno());
        producto.setNombrePrincipal(request.nombrePrincipal());
        producto.setDescripcion(request.descripcion());
        producto.setPeso(request.peso());
        producto.setPrecioListaProveedor(request.precioListaProveedor());
        producto.setDescuentoProveedor(request.descuentoProveedor());
        producto.setCostoNeto(request.costoNeto());

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

        return toResponse(productoRepository.findById(id).orElseThrow());
    }

    public void eliminar(UUID id) {
        if (!productoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        productoRepository.deleteById(id);
    }

    private ProductoResponse toResponse(Producto entity) {
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
                entity.getPeso(),
                entity.getPrecioListaProveedor(),
                entity.getDescuentoProveedor(),
                entity.getCostoNeto(),
                aliases,
                entity.getCreatedAt()
        );
    }
}
