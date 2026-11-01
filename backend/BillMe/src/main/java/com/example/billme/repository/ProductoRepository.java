package com.example.billme.repository;

import com.example.billme.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    Optional<Producto> findByCodigoInterno(String codigoInterno);

    boolean existsByCodigoInterno(String codigoInterno);

    /** Los productos tocados más recientemente (la lista completa puede tener miles). */
    List<Producto> findTop100ByOrderByUpdatedAtDesc();

    /**
     * Búsqueda tolerante a variantes de escritura sobre el nombre y los alias normalizados
     * (trigramas de pg_trgm) y por prefijo de código. Primero los códigos que coinciden y
     * luego los nombres más parecidos; los empates se resuelven por parecido del nombre completo.
     *
     * @param q        texto normalizado (NormalizadorTexto)
     * @param contiene "%q%" para coincidencias exactas dentro del nombre
     * @param prefijo  código en minúsculas seguido de "%"
     */
    @Query(value = """
            select p.* from productos p
            left join lateral (
                select max(word_similarity(:q, a.alias_norm)) as similitud,
                       bool_or(a.alias_norm like :contiene) as contiene
                from alias_productos a
                where a.producto_id = p.id
            ) alias on true
            where p.activo and (
                   lower(p.codigo_interno) like :prefijo
                or p.nombre_norm like :contiene
                or coalesce(alias.contiene, false)
                or word_similarity(:q, p.nombre_norm) >= 0.45
                or coalesce(alias.similitud, 0) >= 0.45)
            order by (lower(p.codigo_interno) like :prefijo) desc,
                     greatest(word_similarity(:q, p.nombre_norm), coalesce(alias.similitud, 0)) desc,
                     similarity(:q, p.nombre_norm) desc,
                     p.nombre_principal
            limit 20
            """, nativeQuery = true)
    List<Producto> buscar(@Param("q") String q,
                          @Param("contiene") String contiene,
                          @Param("prefijo") String prefijo);
}
