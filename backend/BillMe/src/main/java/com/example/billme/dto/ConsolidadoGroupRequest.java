package com.example.billme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ConsolidadoGroupRequest(
    @NotBlank @Size(max = 200) String nombre,
    Integer orden,
    List<@Valid ConsolidadoItemRequest> items
) {}
