package com.example.billme.service;

import com.example.billme.dto.PrecioHistorialResponse;
import com.example.billme.enums.EstadoDocumento;
import com.example.billme.repository.DocumentoLineaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecioHistorialService {

    private final DocumentoLineaRepository documentoLineaRepository;

    public List<PrecioHistorialResponse> obtenerHistorial(UUID clienteId, UUID productoId) {
        return documentoLineaRepository.historialDeCliente(clienteId, productoId, EstadoDocumento.ANULADA).stream()
                .map(l -> new PrecioHistorialResponse(
                        l.getDocumento().getFolio(),
                        l.getDocumento().getFecha(),
                        l.getPrecioUnit(),
                        l.getCantidad()))
                .toList();
    }
}
