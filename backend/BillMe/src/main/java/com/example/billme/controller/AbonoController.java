package com.example.billme.controller;

import com.example.billme.dto.AbonoRequest;
import com.example.billme.dto.AbonoResponse;
import com.example.billme.service.AbonoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/abonos")
@RequiredArgsConstructor
public class AbonoController {

    private final AbonoService abonoService;

    @PostMapping
    public ResponseEntity<AbonoResponse> registrar(@Valid @RequestBody AbonoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonoService.registrarAbono(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AbonoResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody AbonoRequest request) {
        return ResponseEntity.ok(abonoService.actualizarAbono(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        abonoService.eliminarAbono(id);
        return ResponseEntity.noContent().build();
    }
}
