package com.example.billme.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface GananciaPorFactura {
    UUID getId();
    String getFolio();
    LocalDate getFecha();
    String getCliente();
    BigDecimal getTotal();
    BigDecimal getCosto();
    BigDecimal getVentasConCosto();
    int getLineasSinCosto();
}
