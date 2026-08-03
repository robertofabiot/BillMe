package com.example.billme.repository;

import com.example.billme.model.AliasProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AliasProductoRepository extends JpaRepository<AliasProducto, UUID> {

    List<AliasProducto> findByProductoId(UUID productoId);

    void deleteByProductoId(UUID productoId);
}
