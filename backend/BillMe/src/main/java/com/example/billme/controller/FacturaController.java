package com.example.billme.controller;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.dto.FacturaRequest;
import com.example.billme.dto.FacturaResponse;
import com.example.billme.service.AbonoService;
import com.example.billme.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final AbonoService abonoService;

    @GetMapping
    public ResponseEntity<List<FacturaResponse>> obtenerFacturas(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID proyectoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        
        if (clienteId != null || proyectoId != null || estado != null || desde != null || hasta != null) {
            return ResponseEntity.ok(facturaService.filtrar(clienteId, proyectoId, desde, hasta, estado));
        }
        return ResponseEntity.ok(facturaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(facturaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<FacturaResponse> crear(@RequestBody FacturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaResponse> actualizar(@PathVariable UUID id, @RequestBody FacturaRequest request) {
        return ResponseEntity.ok(facturaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<FacturaResponse> confirmarVenta(@PathVariable UUID id) {
        return ResponseEntity.ok(facturaService.confirmarVenta(id));
    }

    @GetMapping("/{facturaId}/abonos")
    public ResponseEntity<List<AbonoResponse>> listarAbonos(@PathVariable UUID facturaId) {
        return ResponseEntity.ok(abonoService.listarPorFactura(facturaId));
    }

    @PostMapping("/{facturaId}/abonos")
    public ResponseEntity<AbonoResponse> registrarAbono(@PathVariable UUID facturaId, @RequestBody AbonoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonoService.registrarAbono(facturaId, request));
    }
}
