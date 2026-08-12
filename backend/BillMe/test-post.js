const http = require('http');

const data = JSON.stringify({
  clienteId: "25fd12f4-fad4-464a-9a24-aca10dc68f82",
  nombre: "Test Consolidado Frontend",
  grupos: [
    {
      id: "g-uuid-1234",
      nombre: "Group 1",
      items: [
        {
          id: "item-uuid-456",
          facturaId: "123e4567-e89b-12d3-a456-426614174000",
          productoNombre: "Product 1",
          cantidad: 10,
          precioUnitario: 100
        }
      ]
    }
  ]
});

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/consolidados',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

const req = http.request(options, res => {
  console.log(`statusCode: ${res.statusCode}`);
  res.on('data', d => process.stdout.write(d));
});
req.on('error', error => console.error(error));
req.write(data);
req.end();
