package com.example.billme.controller;

import com.example.billme.dto.CuentaClienteResponse;
import com.example.billme.service.CuentaClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class CuentaClienteController {

    private final CuentaClienteService cuentaClienteService;

    @GetMapping("/{id}/cuenta")
    public ResponseEntity<CuentaClienteResponse> cuenta(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaClienteService.cuenta(id));
    }
}
