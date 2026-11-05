package com.example.billme.repository;

import com.example.billme.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, UUID> {
    List<Compra> findByDocumentoIdOrderByFechaDesc(UUID documentoId);
}
