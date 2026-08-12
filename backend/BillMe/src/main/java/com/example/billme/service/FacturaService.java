package com.example.billme.service;

import com.example.billme.dto.AbonoResponse;
import com.example.billme.dto.DetalleFacturaRequest;
import com.example.billme.dto.DetalleResponse;
import com.example.billme.dto.FacturaRequest;
import com.example.billme.dto.FacturaResponse;
import com.example.billme.model.Abono;
import com.example.billme.model.Cliente;
import com.example.billme.model.DetalleFactura;
import com.example.billme.enums.EstadoFactura;
import com.example.billme.model.Factura;
import com.example.billme.model.Producto;
import com.example.billme.model.Proyecto;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.ClienteRepository;
import com.example.billme.repository.FacturaRepository;
import com.example.billme.repository.ProductoRepository;
import com.example.billme.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProyectoRepository proyectoRepository;
    private final ProductoRepository productoRepository;
    private final AbonoRepository abonoRepository;

    public List<FacturaResponse> listarTodas() {
        return facturaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FacturaResponse> filtrar(UUID clienteId, UUID proyectoId, Instant desde, Instant hasta, String estado) {
        EstadoFactura estadoFactura = null;
        if (estado != null && !estado.trim().isEmpty()) {
            try {
                estadoFactura = EstadoFactura.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de factura no válido");
            }
        }
        return facturaRepository.filtrar(clienteId, proyectoId, desde, hasta, estadoFactura)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FacturaResponse obtenerPorId(UUID id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        return toResponse(factura);
    }

    public FacturaResponse crear(FacturaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        Proyecto proyecto = null;
        if (request.proyectoId() != null) {
            proyecto = proyectoRepository.findById(request.proyectoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        }

        String nuevoFolio = generarFolioInterno();

        EstadoFactura estado = EstadoFactura.COTIZACION;
        if (request.estado() != null && !request.estado().trim().isEmpty()) {
            try {
                estado = EstadoFactura.valueOf(request.estado().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de factura no válido");
            }
        }

        Factura factura = new Factura();
        factura.setCliente(cliente);
        factura.setProyecto(proyecto);
        factura.setFolioInterno(nuevoFolio);
        factura.setEstado(estado);
        
        List<DetalleFactura> detalles = new ArrayList<>();
        BigDecimal totalVenta = BigDecimal.ZERO;
        BigDecimal costoRealEmpresa = BigDecimal.ZERO;

        if (request.detalles() != null) {
            for (DetalleFacturaRequest dr : request.detalles()) {
                Producto producto = productoRepository.findById(dr.productoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
                
                DetalleFactura detalle = new DetalleFactura();
                detalle.setFactura(factura);
                detalle.setProducto(producto);
                detalle.setCantidad(dr.cantidad());
                detalle.setPrecioUnitarioVenta(dr.precioUnitarioVenta());
                detalles.add(detalle);

                BigDecimal subtotalVenta = dr.cantidad().multiply(dr.precioUnitarioVenta());
                totalVenta = totalVenta.add(subtotalVenta);

                BigDecimal costoProd = producto.getCostoNeto() != null ? producto.getCostoNeto() : BigDecimal.ZERO;
                BigDecimal subtotalCosto = dr.cantidad().multiply(costoProd);
                costoRealEmpresa = costoRealEmpresa.add(subtotalCosto);
            }
        }
        
        factura.setDetalles(detalles);
        factura.setTotalVenta(totalVenta);
        factura.setCostoRealEmpresa(costoRealEmpresa);

        Factura savedFactura = facturaRepository.save(factura);
        return toResponse(savedFactura);
    }

    public FacturaResponse confirmarVenta(UUID id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));

        if (factura.getEstado() != EstadoFactura.COTIZACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden confirmar ventas en estado COTIZACION");
        }

        factura.setEstado(EstadoFactura.PENDIENTE);
        Factura savedFactura = facturaRepository.save(factura);
        return toResponse(savedFactura);
    }

    public FacturaResponse actualizar(UUID id, FacturaRequest request) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));

        if (request.clienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
            factura.setCliente(cliente);
        }

        if (request.proyectoId() != null) {
            Proyecto proyecto = proyectoRepository.findById(request.proyectoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
            factura.setProyecto(proyecto);
        }

        if (request.estado() != null && !request.estado().trim().isEmpty()) {
            try {
                factura.setEstado(EstadoFactura.valueOf(request.estado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de factura no válido");
            }
        }

        if (request.detalles() != null && !request.detalles().isEmpty()) {
            factura.getDetalles().clear();
            
            BigDecimal totalVenta = BigDecimal.ZERO;
            BigDecimal costoRealEmpresa = BigDecimal.ZERO;

            for (DetalleFacturaRequest dr : request.detalles()) {
                Producto producto = productoRepository.findById(dr.productoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
                
                DetalleFactura detalle = new DetalleFactura();
                detalle.setFactura(factura);
                detalle.setProducto(producto);
                detalle.setCantidad(dr.cantidad());
                detalle.setPrecioUnitarioVenta(dr.precioUnitarioVenta());
                factura.getDetalles().add(detalle);

                BigDecimal subtotalVenta = dr.cantidad().multiply(dr.precioUnitarioVenta());
                totalVenta = totalVenta.add(subtotalVenta);

                BigDecimal costoProd = producto.getCostoNeto() != null ? producto.getCostoNeto() : BigDecimal.ZERO;
                BigDecimal subtotalCosto = dr.cantidad().multiply(costoProd);
                costoRealEmpresa = costoRealEmpresa.add(subtotalCosto);
            }
            factura.setTotalVenta(totalVenta);
            factura.setCostoRealEmpresa(costoRealEmpresa);
        }

        Factura savedFactura = facturaRepository.save(factura);
        return toResponse(savedFactura);
    }

    private String generarFolioInterno() {
        List<Factura> facturas = facturaRepository.findAll();
        if (facturas.isEmpty()) {
            return "FAC-0001";
        }
        
        int max = 0;
        for (Factura f : facturas) {
            if (f.getFolioInterno() != null && f.getFolioInterno().startsWith("FAC-")) {
                try {
                    int num = Integer.parseInt(f.getFolioInterno().substring(4));
                    if (num > max) {
                        max = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return String.format("FAC-%04d", max + 1);
    }

    public FacturaResponse toResponse(Factura f) {
        String clienteNombre = f.getCliente() != null ? f.getCliente().getNombre() : null;
        String proyectoNombre = f.getProyecto() != null ? f.getProyecto().getNombre() : null;
        
        List<DetalleResponse> detallesResponse = f.getDetalles() != null 
                ? f.getDetalles().stream().map(this::toDetalleResponse).collect(Collectors.toList()) 
                : new ArrayList<>();
                
        List<AbonoResponse> abonosResponse = f.getAbonos() != null
                ? f.getAbonos().stream().map(this::toAbonoResponse).collect(Collectors.toList())
                : new ArrayList<>();

        BigDecimal totalAbonado = abonoRepository.sumMontoByFacturaId(f.getId());
        if (totalAbonado == null) {
            totalAbonado = BigDecimal.ZERO;
        }

        BigDecimal saldoPendiente = BigDecimal.ZERO;
        if (f.getTotalVenta() != null) {
            saldoPendiente = f.getTotalVenta().subtract(totalAbonado);
        }

        return new FacturaResponse(
                f.getId(),
                f.getFolioInterno(),
                f.getEstado() != null ? f.getEstado().name() : null,
                f.getTotalVenta(),
                f.getCostoRealEmpresa(),
                f.getCliente() != null ? f.getCliente().getId() : null,
                clienteNombre,
                f.getProyecto() != null ? f.getProyecto().getId() : null,
                proyectoNombre,
                f.getCreatedAt(),
                detallesResponse,
                abonosResponse,
                totalAbonado,
                saldoPendiente
        );
    }

    private DetalleResponse toDetalleResponse(DetalleFactura d) {
        return new DetalleResponse(
                d.getId(),
                d.getProducto() != null ? d.getProducto().getId() : null,
                d.getProducto() != null ? d.getProducto().getNombrePrincipal() : null,
                d.getProducto() != null ? d.getProducto().getCodigoInterno() : null,
                d.getCantidad(),
                d.getPrecioUnitarioVenta(),
                d.getProducto() != null ? d.getProducto().getCostoNeto() : null
        );
    }

    private AbonoResponse toAbonoResponse(Abono a) {
        return new AbonoResponse(
                a.getId(),
                a.getMonto(),
                a.getFechaPago()
        );
    }
}
