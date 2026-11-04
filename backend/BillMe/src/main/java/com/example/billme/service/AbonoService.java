package com.example.billme.service;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.enums.TipoDocumento;
import com.example.billme.model.Abono;
import com.example.billme.model.Documento;
import com.example.billme.repository.AbonoRepository;
import com.example.billme.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbonoService {

    private final AbonoRepository abonoRepository;
    private final DocumentoRepository documentoRepository;

    public List<AbonoResponse> listarPorDocumento(UUID documentoId) {
        return abonoRepository.findByDocumentoId(documentoId).stream()
                .map(this::toAbonoResponse)
                .toList();
    }

    @Transactional
    public AbonoResponse registrarAbono(UUID documentoId, AbonoRequest request) {
        Documento factura = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada"));
        if (factura.getTipo() != TipoDocumento.FACTURA || factura.getEstado() != EstadoDocumento.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se registran abonos en facturas activas");
        }
        BigDecimal saldo = factura.getTotal().subtract(abonoRepository.sumMontoByDocumentoId(documentoId));
        if (request.monto().compareTo(saldo) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El abono es mayor que el saldo pendiente de " + saldo);
        }
        Abono abono = new Abono();
        abono.setDocumento(factura);
        abono.setMonto(request.monto());
        abono.setFechaPago(request.fechaPago());
        return toAbonoResponse(abonoRepository.save(abono));
    }

    private AbonoResponse toAbonoResponse(Abono a) {
        return new AbonoResponse(a.getId(), a.getMonto(), a.getFechaPago());
    }
}
