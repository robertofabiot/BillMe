package com.example.billme.controller;

import com.example.billme.dto.PrecioFormulaResponse;
import com.example.billme.dto.PrecioHistorialResponse;
import com.example.billme.dto.PrecioSugeridoResponse;
import com.example.billme.service.PrecioSugeridoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/precios")
@RequiredArgsConstructor
public class PrecioController {

    private final PrecioSugeridoService precioSugeridoService;

    @GetMapping("/sugerido")
    public ResponseEntity<PrecioSugeridoResponse> sugerido(
            @RequestParam UUID clienteId,
            @RequestParam UUID productoId,
            @RequestParam(required = false) String descripcion) {
        return ResponseEntity.ok(precioSugeridoService.sugerir(clienteId, productoId, descripcion));
    }

    @GetMapping("/formula")
    public ResponseEntity<PrecioFormulaResponse> formula(
            @RequestParam UUID productoId,
            @RequestParam(required = false) BigDecimal descuentoPct) {
        return ResponseEntity.ok(precioSugeridoService.calcularFormula(productoId, descuentoPct));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<PrecioHistorialResponse>> historial(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID clienteId) {
        return ResponseEntity.ok(precioSugeridoService.historial(q, clienteId));
    }
}
