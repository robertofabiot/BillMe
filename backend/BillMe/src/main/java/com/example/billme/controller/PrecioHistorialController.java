package com.example.billme.controller;

import com.example.billme.dto.PrecioHistorialResponse;
import com.example.billme.service.PrecioHistorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/precios")
@RequiredArgsConstructor
public class PrecioHistorialController {

    private final PrecioHistorialService precioHistorialService;

    @GetMapping("/historial")
    public ResponseEntity<List<PrecioHistorialResponse>> historial(
            @RequestParam UUID clienteId,
            @RequestParam UUID productoId) {
        
        List<PrecioHistorialResponse> historial = precioHistorialService.obtenerHistorial(clienteId, productoId);
        return ResponseEntity.ok(historial);
    }
}
