package com.example.billme.controller;

import com.example.billme.dto.ConsolidadoGuardarRequest;
import com.example.billme.dto.ConsolidadoRangoRequest;
import com.example.billme.dto.ConsolidadoResponse;
import com.example.billme.exportacion.ArchivoExcel;
import com.example.billme.service.ConsolidadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        return ResponseEntity.ok(consolidadoService.listar(clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsolidadoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(consolidadoService.obtener(id));
    }

    @PostMapping("/vista-previa")
    public ResponseEntity<ConsolidadoResponse> vistaPrevia(@Valid @RequestBody ConsolidadoRangoRequest request) {
        return ResponseEntity.ok(consolidadoService.vistaPrevia(request));
    }

    @PostMapping
    public ResponseEntity<ConsolidadoResponse> crear(@Valid @RequestBody ConsolidadoGuardarRequest request) {
        ConsolidadoResponse response = consolidadoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsolidadoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ConsolidadoGuardarRequest request) {
        return ResponseEntity.ok(consolidadoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        consolidadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @PathVariable UUID id,
            @RequestParam(required = false) String encabezado) {
        ArchivoExcel archivo = consolidadoService.exportarExcel(id, encabezado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo.contenido());
    }
}
