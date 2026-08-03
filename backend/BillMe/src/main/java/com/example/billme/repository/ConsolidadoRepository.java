package com.example.billme.repository;

import com.example.billme.model.Consolidado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsolidadoRepository extends JpaRepository<Consolidado, UUID> {

    List<Consolidado> findByClienteId(UUID clienteId);

    boolean existsByFolioInterno(String folioInterno);
}
