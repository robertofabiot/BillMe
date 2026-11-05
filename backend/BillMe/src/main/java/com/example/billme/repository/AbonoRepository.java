package com.example.billme.repository;

import com.example.billme.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AbonoRepository extends JpaRepository<Abono, UUID> {

    boolean existsByClienteId(UUID clienteId);

    List<Abono> findByClienteIdOrderByFechaPagoAscCreatedAtAsc(UUID clienteId);

    @Query("SELECT COALESCE(SUM(a.monto), 0) FROM Abono a WHERE (cast(:clienteId as uuid) IS NULL OR a.cliente.id = :clienteId) " +
           "AND (cast(:proyectoId as uuid) IS NULL OR a.proyecto.id = :proyectoId) " +
           "AND (cast(:desde as date) IS NULL OR a.fechaPago >= :desde) " +
           "AND (cast(:hasta as date) IS NULL OR a.fechaPago <= :hasta)")
    BigDecimal sumar(@Param("clienteId") UUID clienteId, @Param("proyectoId") UUID proyectoId,
                     @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
