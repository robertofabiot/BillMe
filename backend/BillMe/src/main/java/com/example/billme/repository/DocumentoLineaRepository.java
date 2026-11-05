package com.example.billme.repository;

import com.example.billme.model.DocumentoLinea;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface DocumentoLineaRepository extends JpaRepository<DocumentoLinea, UUID> {

    boolean existsByProductoId(UUID productoId);

    @Query("SELECT l.importKey FROM DocumentoLinea l WHERE l.importKey IN :keys")
    Set<String> findImportKeysIn(@Param("keys") Collection<String> keys);

    @Query("SELECT l FROM DocumentoLinea l JOIN FETCH l.documento d JOIN FETCH d.cliente " +
           "WHERE l.producto.id = :productoId AND d.estado <> com.example.billme.enums.EstadoDocumento.ANULADA " +
           "AND (cast(:clienteId as uuid) IS NULL OR d.cliente.id = :clienteId) " +
           "AND (cast(:descripcionNorm as string) IS NULL OR l.descripcionNorm = :descripcionNorm) " +
           "ORDER BY d.fecha DESC, d.createdAt DESC, l.orden ASC")
    List<DocumentoLinea> ultimosPrecios(@Param("productoId") UUID productoId, @Param("clienteId") UUID clienteId,
                                        @Param("descripcionNorm") String descripcionNorm, Pageable pagina);

    @Query("SELECT l FROM DocumentoLinea l JOIN l.documento d " +
           "WHERE d.cliente.id = :clienteId AND l.metodoPrecio = com.example.billme.enums.MetodoPrecio.POR_PESO " +
           "AND d.estado <> com.example.billme.enums.EstadoDocumento.ANULADA " +
           "ORDER BY d.fecha DESC, d.createdAt DESC")
    List<DocumentoLinea> ultimasPorPeso(@Param("clienteId") UUID clienteId, Pageable pagina);

    @Query(value = """
            select l.* from documento_lineas l join documentos d on d.id = l.documento_id
            where d.estado <> 'ANULADA'
              and (cast(:clienteId as uuid) is null or d.cliente_id = cast(:clienteId as uuid))
              and (:q = '' or l.descripcion_norm like :contiene or word_similarity(:q, l.descripcion_norm) >= 0.45)
            order by d.fecha desc, d.created_at desc
            limit 100
            """, nativeQuery = true)
    List<DocumentoLinea> historial(@Param("q") String q, @Param("contiene") String contiene, @Param("clienteId") UUID clienteId);
}
