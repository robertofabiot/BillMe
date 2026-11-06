-- Historial que no se puede alterar, tipo de cambio por documento y opción de total en dólares.
alter table documento_revisiones add column motivo varchar(200);
update documento_revisiones set motivo = 'Edición';
alter table documento_revisiones alter column motivo set not null;

create function impedir_cambio_revision() returns trigger language plpgsql as $$
begin
    raise exception 'El historial de un documento no se puede modificar ni borrar';
end;
$$;

create trigger revisiones_inmutables
    before update or delete on documento_revisiones
    for each row execute function impedir_cambio_revision();

alter table documentos
    add column mostrar_total_usd boolean not null default false,
    add column tipo_cambio numeric(12, 6);
update documentos
set tipo_cambio = (select valor::numeric from configuracion where clave = 'tipo_cambio');
alter table documentos alter column tipo_cambio set not null;

alter table consolidados
    add column mostrar_total_usd boolean not null default false,
    add column tipo_cambio numeric(12, 6);
update consolidados
set tipo_cambio = (select valor::numeric from configuracion where clave = 'tipo_cambio');
alter table consolidados alter column tipo_cambio set not null;
