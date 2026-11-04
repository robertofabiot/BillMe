package com.example.billme.repository;

import com.example.billme.model.DocumentoRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoRevisionRepository extends JpaRepository<DocumentoRevision, UUID> {

    List<DocumentoRevision> findByDocumentoIdOrderByNumeroDesc(UUID documentoId);

    int countByDocumentoId(UUID documentoId);
}
