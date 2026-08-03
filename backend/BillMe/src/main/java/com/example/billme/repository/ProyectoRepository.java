package com.example.billme.repository;

import com.example.billme.enums.EstadoProyecto;
import com.example.billme.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {

    /** Todos los proyectos de un cliente. */
    List<Proyecto> findByClienteId(UUID clienteId);

    /**
     * Solo proyectos activos de un cliente.
     * Se usa al crear una nueva factura para poblar el selector de proyectos.
     */
    List<Proyecto> findByClienteIdAndEstado(UUID clienteId, EstadoProyecto estado);
}
