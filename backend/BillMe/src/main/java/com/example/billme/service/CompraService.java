package com.example.billme.service;

import com.example.billme.calculo.CalculoDocumento;
import com.example.billme.dto.CompraRequest;
import com.example.billme.dto.CompraResponse;
import com.example.billme.dto.PrecioListaRequest;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.FuentePrecio;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Compra;
import com.example.billme.model.Documento;
import com.example.billme.model.DocumentoLinea;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.CompraRepository;
import com.example.billme.repository.DocumentoRepository;
import com.example.billme.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraService {

    private final CompraRepository compraRepository;
    private final DocumentoRepository documentoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ConfiguracionService configuracionService;
    private final PrecioListaService precioListaService;
    private final DocumentoService documentoService;

    @Transactional
    public CompraResponse registrar(UUID documentoId, CompraRequest request) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));

        if (documento.getTipo() != TipoDocumento.FACTURA || documento.getEstado() != EstadoDocumento.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se registran compras de facturas activas");
        }

        String copiaAnterior = documentoService.copia(documento);

        Map<UUID, DocumentoLinea> lineasMap = documento.getLineas().stream()
                .collect(Collectors.toMap(DocumentoLinea::getId, Function.identity()));

        for (CompraRequest.Linea reqLine : request.lineas()) {
            if (!lineasMap.containsKey(reqLine.lineaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La línea no pertenece a esta factura");
            }
        }

        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));

        Compra compra = compraRepository.save(Compra.builder()
                .proveedor(proveedor)
                .documento(documento)
                .fecha(request.fecha())
                .numeroFactura(request.numeroFactura() == null || request.numeroFactura().isBlank() ? null : request.numeroFactura().trim())
                .nota(request.nota() == null || request.nota().isBlank() ? null : request.nota().trim())
                .build());

        for (CompraRequest.Linea reqLine : request.lineas()) {
            DocumentoLinea linea = lineasMap.get(reqLine.lineaId());
            linea.setCompra(compra);
            linea.setCostoUnitConIva(CalculoDocumento.r2(reqLine.precioSinIva().multiply(configuracionService.factorIva())));

            if (reqLine.pesoUnitKg() != null) {
                CalculoDocumento.LineaCalculada calc = CalculoDocumento.linea(linea.getCantidad(), linea.getPrecioUnit(), reqLine.pesoUnitKg());
                linea.setPesoUnitKg(calc.pesoUnitKg());
                linea.setPesoTotalKg(calc.pesoTotalKg());
            }

            if (!linea.getProducto().isGenerico() && reqLine.descuentoPct() != null) {
                String nota = (request.numeroFactura() != null && !request.numeroFactura().isBlank())
                        ? "Compra " + request.numeroFactura().trim()
                        : "Compra";
                precioListaService.registrar(linea.getProducto().getId(),
                        new PrecioListaRequest(FuentePrecio.FACTURA_PROVEEDOR, reqLine.precioSinIva(), reqLine.descuentoPct(), request.fecha(), nota));
            }
        }

        BigDecimal totalKg = documento.getLineas().stream()
                .map(DocumentoLinea::getPesoTotalKg)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        documento.setTotalKg(CalculoDocumento.r2(totalKg));

        documentoService.registrarRevision(documento, copiaAnterior, "Compra registrada");

        return new CompraResponse(
                compra.getId(),
                proveedor.getId(),
                proveedor.getNombre(),
                compra.getFecha(),
                compra.getNumeroFactura(),
                compra.getNota());
    }

    public List<CompraResponse> listar(UUID documentoId) {
        if (!documentoRepository.existsById(documentoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
        return compraRepository.findByDocumentoIdOrderByFechaDesc(documentoId).stream()
                .map(c -> new CompraResponse(
                        c.getId(),
                        c.getProveedor().getId(),
                        c.getProveedor().getNombre(),
                        c.getFecha(),
                        c.getNumeroFactura(),
                        c.getNota()))
                .toList();
    }
}
