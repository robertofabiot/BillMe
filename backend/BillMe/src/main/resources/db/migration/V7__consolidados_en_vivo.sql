-- El consolidado se arma en vivo desde las facturas: solo se guardan el rango y los nombres editados.
drop table consolidado_items;
drop table consolidado_groups;

alter table consolidados rename column folio_interno to folio;
alter table consolidados
    add column proyecto_id uuid constraint fk_consolidados_proyecto references proyectos,
    add column desde       date,
    add column hasta       date;

update consolidados
set desde = (created_at at time zone 'America/Managua')::date,
    hasta = (created_at at time zone 'America/Managua')::date;

alter table consolidados
    alter column desde set not null,
    alter column hasta set not null,
    add constraint chk_consolidados_rango check (desde <= hasta);

create table consolidado_grupos (
    id             uuid                        not null primary key,
    created_at     timestamp(6) with time zone not null,
    updated_at     timestamp(6) with time zone not null,
    consolidado_id uuid                        not null constraint fk_grupos_consolidado references consolidados,
    fecha_entrega  date                        not null,
    nombre         varchar(200)                not null,
    constraint uq_grupos_consolidado_fecha unique (consolidado_id, fecha_entrega)
);

create index idx_consolidados_proyecto on consolidados (proyecto_id);

create sequence seq_folio_cons;
select setval('seq_folio_cons',
              coalesce((select max(substring(folio from 6)::int) from consolidados where folio ~ '^CONS-[0-9]+$'), 0) + 1,
              false);
