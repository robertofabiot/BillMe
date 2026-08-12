package com.example.billme.controller;

import com.example.billme.dto.ProyectoRequest;
import com.example.billme.dto.ProyectoResponse;
import com.example.billme.service.ProyectoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
public class ProyectoController {

    private final ProyectoService proyectoService;

    @GetMapping
    public ResponseEntity<List<ProyectoResponse>> listar(
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false, defaultValue = "false") boolean activos) {
        
        List<ProyectoResponse> proyectos;
        if (clienteId != null) {
            if (activos) {
                proyectos = proyectoService.listarActivosPorCliente(clienteId);
            } else {
                proyectos = proyectoService.listarPorCliente(clienteId);
            }
        } else {
            proyectos = proyectoService.listarTodos();
        }
        
        return ResponseEntity.ok(proyectos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crear(@RequestBody ProyectoRequest request) {
        ProyectoResponse response = proyectoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProyectoResponse> actualizar(@PathVariable UUID id, @RequestBody ProyectoRequest request) {
        return ResponseEntity.ok(proyectoService.actualizar(id, request));
    }
}
