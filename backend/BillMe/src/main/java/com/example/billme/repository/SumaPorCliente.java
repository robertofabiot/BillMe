package com.example.billme.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface SumaPorCliente {
    UUID getClienteid();
    BigDecimal getSuma();
}
