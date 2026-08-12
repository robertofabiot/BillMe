const http = require('http');

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/consolidados',
  method: 'GET'
};

const req = http.request(options, res => {
  let body = '';
  res.on('data', chunk => body += chunk);
  res.on('end', () => {
    const data = JSON.parse(body);
    console.log("Current consolidados:", data.length);
    // Since we don't have a DELETE endpoint, we can't delete via API.
  });
});
req.on('error', e => console.error(e));
req.end();
