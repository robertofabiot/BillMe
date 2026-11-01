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

    List<Producto> findByCodigoInternoIn(java.util.Collection<String> codigos);

    /** Los productos tocados más recientemente (la lista completa puede tener miles). */
    List<Producto> findTop100ByOrderByUpdatedAtDesc();

    /**
     * Búsqueda tolerante a variantes de escritura sobre el nombre y los alias normalizados
     * (trigramas de pg_trgm) y por prefijo de código. Orden: primero los códigos que coinciden,
     * luego los que contienen más palabras completas de la búsqueda (así "2" no coincide con
     * "1/2", pero "1.5" sí con "1.5mm"), luego los más parecidos.
     *
     * @param q        texto normalizado (NormalizadorTexto)
     * @param contiene "%q%" para coincidencias exactas dentro del nombre
     * @param prefijo  código en minúsculas seguido de "%"
     */
    @Query(value = """
            select p.* from productos p
            cross join lateral (
                select count(*) as palabras
                from unnest(string_to_array(:q, ' ')) palabra
                where (' ' || p.nombre_norm) like ('% ' || palabra || '%')
            ) nombre
            left join lateral (
                select max(word_similarity(:q, a.alias_norm)) as similitud,
                       bool_or(a.alias_norm like :contiene) as contiene,
                       max((select count(*)
                            from unnest(string_to_array(:q, ' ')) palabra
                            where (' ' || a.alias_norm) like ('% ' || palabra || '%'))) as palabras
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
                     greatest(nombre.palabras, coalesce(alias.palabras, 0)) desc,
                     greatest(word_similarity(:q, p.nombre_norm), coalesce(alias.similitud, 0)) desc,
                     similarity(:q, p.nombre_norm) desc,
                     p.nombre_principal
            limit 20
            """, nativeQuery = true)
    List<Producto> buscar(@Param("q") String q,
                          @Param("contiene") String contiene,
                          @Param("prefijo") String prefijo);
}
