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

    Optional<Producto> findFirstByNombreNormAndActivoTrue(String nombreNorm);

    boolean existsByCodigoInterno(String codigoInterno);

    List<Producto> findByCodigoInternoIn(java.util.Collection<String> codigos);

    /** Los productos tocados más recientemente (la lista completa puede tener miles). */
    List<Producto> findTop100ByOrderByUpdatedAtDesc();

    /**
     * Búsqueda tolerante a variantes de escritura sobre el nombre y los alias normalizados
     * (trigramas de pg_trgm) y por prefijo de código. Orden: primero los códigos que coinciden,
     * luego los que contienen más palabras de la búsqueda; entre esos, el que tiene menos palabras
     * de sobra ("1/2 x 1.5" prefiere "Tubo 1/2 x 1.50" antes que "Tubo 1 1/2 x 1.50"), y al final
     * los más parecidos.
     * Una palabra coincide si está completa en el nombre o seguida solo de letras de unidad:
     * "1.5" coincide con "1.5mm", pero "1" no con "1/2" ni "2" con "20pies". Así el orden no
     * depende de desempates alfabéticos (que cambian según el locale de Postgres).
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
                where (' ' || p.nombre_norm || ' ') ~ (' ' || replace(palabra, '.', '\\.') || '[a-z]* ')
            ) nombre
            left join lateral (
                select max(word_similarity(:q, a.alias_norm)) as similitud,
                       bool_or(a.alias_norm like :contiene) as contiene,
                       max((select count(*)
                            from unnest(string_to_array(:q, ' ')) palabra
                            where (' ' || a.alias_norm || ' ') ~ (' ' || replace(palabra, '.', '\\.') || '[a-z]* '))) as palabras
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
                     array_length(string_to_array(p.nombre_norm, ' '), 1),
                     greatest(word_similarity(:q, p.nombre_norm), coalesce(alias.similitud, 0)) desc,
                     similarity(:q, p.nombre_norm) desc,
                     p.nombre_principal
            limit 20
            """, nativeQuery = true)
    List<Producto> buscar(@Param("q") String q,
                          @Param("contiene") String contiene,
                          @Param("prefijo") String prefijo);
}
