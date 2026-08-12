package com.example.billme.service;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.model.Abono;
import com.example.billme.enums.EstadoFactura;
import com.example.billme.model.Factura;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbonoService {

    private final AbonoRepository abonoRepository;
    private final FacturaRepository facturaRepository;

    public List<AbonoResponse> listarPorFactura(UUID facturaId) {
        return abonoRepository.findByFacturaId(facturaId).stream()
                .map(this::toAbonoResponse)
                .collect(Collectors.toList());
    }

    public AbonoResponse registrarAbono(UUID facturaId, AbonoRequest request) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));

        if (factura.getEstado() == EstadoFactura.COTIZACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pueden registrar abonos en cotizaciones");
        }
        
        if (factura.getEstado() == EstadoFactura.PAGADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura ya está pagada");
        }

        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del abono debe ser mayor a cero");
        }

        BigDecimal totalAbonadoActual = abonoRepository.sumMontoByFacturaId(facturaId);
        if (totalAbonadoActual == null) totalAbonadoActual = BigDecimal.ZERO;
        BigDecimal totalVenta = factura.getTotalVenta() != null ? factura.getTotalVenta() : BigDecimal.ZERO;
        BigDecimal saldoPendiente = totalVenta.subtract(totalAbonadoActual);

        if (request.monto().compareTo(saldoPendiente) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del abono excede el saldo pendiente de " + saldoPendiente);
        }

        Abono abono = new Abono();
        abono.setFactura(factura);
        abono.setMonto(request.monto());
        abono.setFechaPago(request.fechaPago());
        
        Abono savedAbono = abonoRepository.save(abono);

        BigDecimal totalAbonado = abonoRepository.sumMontoByFacturaId(facturaId);
        if (totalAbonado == null) {
            totalAbonado = BigDecimal.ZERO;
        }

        if (totalAbonado.compareTo(totalVenta) >= 0) {
            factura.setEstado(EstadoFactura.PAGADO);
        } else if (totalAbonado.compareTo(BigDecimal.ZERO) > 0) {
            factura.setEstado(EstadoFactura.PAGO_PARCIAL);
        }

        facturaRepository.save(factura);

        return toAbonoResponse(savedAbono);
    }

    private AbonoResponse toAbonoResponse(Abono a) {
        return new AbonoResponse(
                a.getId(),
                a.getMonto(),
                a.getFechaPago()
        );
    }
}
