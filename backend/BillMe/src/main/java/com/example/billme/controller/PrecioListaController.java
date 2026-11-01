package com.example.billme.controller;

import com.example.billme.dto.CalculoListaResponse;
import com.example.billme.dto.PrecioListaRequest;
import com.example.billme.dto.PrecioListaResponse;
import com.example.billme.dto.PreciosListaProductoResponse;
import com.example.billme.service.PrecioListaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/productos/{productoId}/precios-lista")
@RequiredArgsConstructor
public class PrecioListaController {

    private final PrecioListaService precioListaService;

    @GetMapping
    public ResponseEntity<PreciosListaProductoResponse> consultar(@PathVariable UUID productoId) {
        return ResponseEntity.ok(precioListaService.consultar(productoId));
    }

    /** Vista previa: calcula el precio de lista sin guardarlo. */
    @PostMapping("/calcular")
    public ResponseEntity<CalculoListaResponse> calcular(@PathVariable UUID productoId,
                                                         @Valid @RequestBody PrecioListaRequest request) {
        return ResponseEntity.ok(new CalculoListaResponse(
                precioListaService.calcular(request.fuente(), request.precio(), request.descuentoPct())));
    }

    @PostMapping
    public ResponseEntity<PrecioListaResponse> registrar(@PathVariable UUID productoId,
                                                         @Valid @RequestBody PrecioListaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(precioListaService.registrar(productoId, request));
    }
}
