package com.example.billme.repository;

import com.example.billme.enums.EstadoProyecto;
import com.example.billme.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {

    Optional<Proyecto> findFirstByClienteIdAndNombreIgnoreCase(UUID clienteId, String nombre);

    List<Proyecto> findAllByOrderByNombreAsc();

    /** Todos los proyectos de un cliente, ordenados por nombre. */
    List<Proyecto> findByClienteIdOrderByNombreAsc(UUID clienteId);

    /**
     * Solo proyectos activos de un cliente, ordenados por nombre.
     * Se usa al crear una nueva factura para poblar el selector de proyectos.
     */
    List<Proyecto> findByClienteIdAndEstadoOrderByNombreAsc(UUID clienteId, EstadoProyecto estado);
}
