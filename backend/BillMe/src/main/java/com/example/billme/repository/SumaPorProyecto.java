package com.example.billme.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface SumaPorProyecto {
    UUID getProyectoid();
    BigDecimal getSuma();
    default UUID getProyectoId() {
        return getProyectoid();
    }
}
