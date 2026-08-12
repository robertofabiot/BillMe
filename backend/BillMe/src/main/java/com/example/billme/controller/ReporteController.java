package com.example.billme.controller;

import com.example.billme.dto.ReporteResponse;
import com.example.billme.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/general")
    public ResponseEntity<ReporteResponse> general(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID proyectoId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        
        ReporteResponse reporte = reporteService.generarReporte(clienteId, proyectoId, desde, hasta);
        return ResponseEntity.ok(reporte);
    }
}
