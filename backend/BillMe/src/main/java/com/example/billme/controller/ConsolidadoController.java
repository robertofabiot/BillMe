package com.example.billme.controller;

import com.example.billme.dto.ConsolidadoRequest;
import com.example.billme.dto.ConsolidadoResponse;
import com.example.billme.service.ConsolidadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consolidados")
@RequiredArgsConstructor
public class ConsolidadoController {

    private final ConsolidadoService consolidadoService;

    @GetMapping
    public ResponseEntity<List<ConsolidadoResponse>> listar(
            @RequestParam(required = false) UUID clienteId) {
        if (clienteId != null) {
            return ResponseEntity.ok(consolidadoService.listarPorCliente(clienteId));
        }
        return ResponseEntity.ok(consolidadoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsolidadoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(consolidadoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ConsolidadoResponse> crear(@RequestBody ConsolidadoRequest request) {
        ConsolidadoResponse response = consolidadoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        consolidadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
