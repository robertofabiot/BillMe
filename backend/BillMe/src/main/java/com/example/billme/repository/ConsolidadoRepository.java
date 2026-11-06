package com.example.billme.repository;

import com.example.billme.model.Consolidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ConsolidadoRepository extends JpaRepository<Consolidado, UUID> {

    @Query(value = "select nextval('seq_folio_cons')", nativeQuery = true)
    long siguienteFolio();

    List<Consolidado> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    List<Consolidado> findAllByOrderByCreatedAtDesc();

    boolean existsByClienteId(UUID clienteId);

    boolean existsByProyectoId(UUID proyectoId);
}
