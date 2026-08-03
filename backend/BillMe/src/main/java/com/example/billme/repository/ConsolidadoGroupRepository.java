package com.example.billme.repository;

import com.example.billme.model.ConsolidadoGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsolidadoGroupRepository extends JpaRepository<ConsolidadoGroup, UUID> {

    /** Devuelve los grupos de un consolidado ya ordenados por su campo `orden`. */
    List<ConsolidadoGroup> findByConsolidadoIdOrderByOrdenAsc(UUID consolidadoId);
}
