-- La tarifa por kg de cada cliente puede estar en dólares o en córdobas.
alter table clientes rename column tarifa_kg_usd to tarifa_kg;
alter table clientes add column tarifa_moneda varchar(3) not null default 'USD'
    check (tarifa_moneda in ('USD', 'NIO'));
