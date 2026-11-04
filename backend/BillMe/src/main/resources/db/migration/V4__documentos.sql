-- Cotizaciones y facturas: misma estructura, series de folio separadas (COT- y FAC-),
-- fecha real del documento, líneas con descripción libre, pesos e historial de versiones.
-- Las tablas se renombran (no se recrean) para conservar las llaves foráneas de abonos y consolidados.

alter table facturas rename to documentos;
alter table detalles_factura rename to documento_lineas;
alter table documento_lineas rename column factura_id to documento_id;

-- ── Documentos ───────────────────────────────────────────────────────────────

alter table documentos rename column folio_interno to folio;
alter table documentos rename column total_venta to total;
alter table documentos drop column costo_real_empresa;  -- el costo se suma desde las líneas
alter table documentos drop constraint facturas_estado_check;

alter table documentos add column tipo varchar(20);
update documentos set tipo = case when estado = 'COTIZACION' then 'COTIZACION' else 'FACTURA' end;
update documentos set estado = case when tipo = 'COTIZACION' then 'PENDIENTE' else 'ACTIVA' end;
alter table documentos alter column tipo set not null;
alter table documentos add constraint documentos_tipo_check check (tipo in ('COTIZACION', 'FACTURA'));
alter table documentos add constraint documentos_estado_check
    check (estado in ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'ACTIVA', 'ANULADA'));

alter table documentos add column fecha date;
update documentos set fecha = (created_at at time zone 'America/Managua')::date;
alter table documentos alter column fecha set not null;

alter table documentos
    add column total_kg             numeric(12, 2) not null default 0,
    add column notas                text,
    add column mostrar_entrega      boolean        not null default false,
    add column cotizacion_origen_id uuid constraint fk_documentos_cotizacion_origen references documentos,
    add column import_key           varchar(64) unique;

create index idx_documentos_tipo_fecha on documentos (tipo, fecha desc);
create index idx_documentos_cliente_fecha on documentos (cliente_id, fecha desc);

-- ── Líneas ───────────────────────────────────────────────────────────────────

alter table documento_lineas rename column precio_unitario_venta to precio_unit;
alter table documento_lineas rename column costo_unitario_snapshot to costo_unit_con_iva;

alter table documento_lineas
    add column orden            integer        not null default 0,
    add column descripcion      varchar(500)   not null default '',
    add column descripcion_norm varchar(500)   not null default '',
    add column peso_unit_kg     numeric(10, 2),
    add column peso_total_kg    numeric(12, 2),
    add column total            numeric(14, 2) not null default 0,
    add column metodo_precio    varchar(20)    not null default 'MANUAL'
        check (metodo_precio in ('ULTIMO_PRECIO', 'FORMULA_LISTA', 'POR_PESO', 'POR_METRO', 'MANUAL')),
    add column parametros       text,
    add column fecha_entrega    date,
    add column lugar_entrega    varchar(200),
    add column import_key       varchar(64) unique;

update documento_lineas l
set descripcion      = p.nombre_principal,
    descripcion_norm = p.nombre_norm,
    total            = round(l.cantidad * l.precio_unit, 2)
from productos p
where p.id = l.producto_id;

create index idx_lineas_documento_orden on documento_lineas (documento_id, orden);
create index idx_lineas_descripcion_norm_trgm on documento_lineas using gin (descripcion_norm gin_trgm_ops);

-- ── Versiones anteriores (al editar un documento) ───────────────────────────

create table documento_revisiones (
    id           uuid                        not null primary key,
    created_at   timestamp(6) with time zone not null,
    updated_at   timestamp(6) with time zone not null,
    documento_id uuid                        not null constraint fk_revisiones_documento references documentos,
    numero       integer                     not null,
    snapshot     text                        not null,
    usuario_id   uuid                        constraint fk_revisiones_usuario references usuarios,
    constraint uq_revisiones_documento_numero unique (documento_id, numero)
);

-- ── Folios: una serie por tipo ───────────────────────────────────────────────

create sequence seq_folio_cot;
create sequence seq_folio_fac;
select setval('seq_folio_fac',
              coalesce((select max(substring(folio from 5)::int) from documentos where folio ~ '^FAC-[0-9]+$'), 0) + 1,
              false);
