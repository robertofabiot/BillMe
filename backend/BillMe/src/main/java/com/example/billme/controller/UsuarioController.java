package com.example.billme.controller;

import com.example.billme.dto.CambioPasswordRequest;
import com.example.billme.dto.RestablecerPasswordRequest;
import com.example.billme.dto.UsuarioRequest;
import com.example.billme.dto.UsuarioResponse;
import com.example.billme.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/yo/password")
    public ResponseEntity<Void> cambiarMiPassword(@Valid @RequestBody CambioPasswordRequest request,
                                                  Authentication authentication) {
        usuarioService.cambiarMiPassword(authentication.getName(), request.actual(), request.nueva());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> restablecerPassword(@PathVariable UUID id,
                                                    @Valid @RequestBody RestablecerPasswordRequest request,
                                                    Authentication authentication) {
        usuarioService.restablecerPassword(id, request.nueva(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
