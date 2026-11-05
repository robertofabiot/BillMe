package com.example.billme.controller;

import com.example.billme.dto.CompraRequest;
import com.example.billme.dto.CompraResponse;
import com.example.billme.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos/{id}/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse registrar(@PathVariable UUID id, @Valid @RequestBody CompraRequest request) {
        return compraService.registrar(id, request);
    }

    @GetMapping
    public List<CompraResponse> listar(@PathVariable UUID id) {
        return compraService.listar(id);
    }
}
