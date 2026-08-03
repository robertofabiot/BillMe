package com.example.billme.repository;

import com.example.billme.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AbonoRepository extends JpaRepository<Abono, UUID> {

    List<Abono> findByFacturaId(UUID facturaId);

    /**
     * Suma total abonado a una factura.
     * Usada para calcular el saldo pendiente sin cargar todos los objetos.
     */
    @Query("SELECT COALESCE(SUM(a.monto), 0) FROM Abono a WHERE a.factura.id = :facturaId")
    BigDecimal sumMontoByFacturaId(@Param("facturaId") UUID facturaId);
}
