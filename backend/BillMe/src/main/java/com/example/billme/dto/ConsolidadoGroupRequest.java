package com.example.billme.dto;

import java.util.List;

public record ConsolidadoGroupRequest(
    String nombre,
    Integer orden,
    List<ConsolidadoItemRequest> items
) {}
