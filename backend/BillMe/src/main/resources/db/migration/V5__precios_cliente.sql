-- Precio por cliente: un descuento sobre la lista y una tarifa propia para placas y platinas.
alter table clientes
    add column descuento_pct numeric(5, 2) not null default 0
        check (descuento_pct >= 0 and descuento_pct < 100),
    add column tarifa_kg_usd numeric(8, 4) check (tarifa_kg_usd > 0);
