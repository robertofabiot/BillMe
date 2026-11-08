-- Registros del histórico de precios: tipo, estado y serie propios.
alter table documentos drop constraint documentos_tipo_check;
alter table documentos add constraint documentos_tipo_check
    check (tipo in ('COTIZACION', 'FACTURA', 'HISTORICO'));

alter table documentos drop constraint documentos_estado_check;
alter table documentos add constraint documentos_estado_check
    check (estado in ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'ACTIVA', 'ANULADA', 'HISTORICA'));

create sequence seq_folio_his;
