package com.example.billme.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConsolidadoGroupResponse(
    UUID id,
    String nombre,
    Integer orden,
    List<ConsolidadoItemResponse> items,
    BigDecimal subtotal
) {}
