-- Los abonos pasan de la factura a la cuenta del cliente, con proyecto opcional.
alter table clientes
    add column saldo_inicial       numeric(14, 2) not null default 0,
    add column fecha_inicio_cuenta date;

alter table abonos
    add column cliente_id  uuid constraint fk_abonos_cliente references clientes,
    add column proyecto_id uuid constraint fk_abonos_proyecto references proyectos,
    add column nota        text;

update abonos a
set cliente_id  = d.cliente_id,
    proyecto_id = d.proyecto_id
from documentos d
where d.id = a.factura_id;

alter table abonos alter column cliente_id set not null;
alter table abonos alter column monto type numeric(14, 2);
alter table abonos drop column factura_id;

create index idx_abonos_cliente_fecha on abonos (cliente_id, fecha_pago);
create index idx_abonos_proyecto on abonos (proyecto_id);
