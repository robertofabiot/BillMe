package com.example.billme.repository;

import com.example.billme.model.ConsolidadoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsolidadoItemRepository extends JpaRepository<ConsolidadoItem, UUID> {

    /** Ítems de un grupo ya ordenados por su posición visual. */
    List<ConsolidadoItem> findByGrupoIdOrderByOrdenAsc(UUID grupoId);
}
