package com.example.billme.controller;

import com.example.billme.dto.*;
import com.example.billme.service.CalculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculos")
@RequiredArgsConstructor
public class CalculoController {

    private final CalculoService calculoService;

    @PostMapping("/peso")
    public ResponseEntity<CalculoPesoResponse> calcularPeso(@Valid @RequestBody CalculoPesoRequest request) {
        return ResponseEntity.ok(calculoService.calcularPeso(request));
    }

    @PostMapping("/precio-por-peso")
    public ResponseEntity<PrecioCalculadoResponse> calcularPrecioPorPeso(@Valid @RequestBody PrecioPorPesoRequest request) {
        return ResponseEntity.ok(calculoService.calcularPrecioPorPeso(request));
    }

    @PostMapping("/precio-por-metro")
    public ResponseEntity<PrecioCalculadoResponse> calcularPrecioPorMetro(@Valid @RequestBody PrecioPorMetroRequest request) {
        return ResponseEntity.ok(calculoService.calcularPrecioPorMetro(request));
    }
}
