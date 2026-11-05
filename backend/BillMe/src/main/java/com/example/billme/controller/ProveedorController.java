package com.example.billme.controller;

import com.example.billme.dto.ProveedorRequest;
import com.example.billme.dto.ProveedorResponse;
import com.example.billme.model.Proveedor;
import com.example.billme.repository.ProveedorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorRepository proveedorRepository;

    @GetMapping
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findByActivoTrueOrderByNombre().stream()
                .map(p -> new ProveedorResponse(p.getId(), p.getNombre()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorResponse crear(@Valid @RequestBody ProveedorRequest request) {
        String nombre = request.nombre().trim();
        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe ese proveedor");
        }
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder().nombre(nombre).build());
        return new ProveedorResponse(proveedor.getId(), proveedor.getNombre());
    }
}
