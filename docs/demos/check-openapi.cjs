
// Stable structural comparison: ignore whitespace and object-key order, not API content.
const fs = require('node:fs');
const assert = require('node:assert/strict');

const generated = process.argv[2] || 'backend/umss-market-api/target/openapi.json';
const committed = process.argv[3] || 'openapi.json';

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.keys(value).sort().map(key => [key, canonical(value[key])])
    );
  }
  return value;
}

const contract = JSON.parse(fs.readFileSync(generated, 'utf8').replace(/^\uFEFF/, ''));

// All required operations for the existing modules and Demo 4.
const requiredOperations = {
  '/api/recommendations': ['get'],
  '/api/interactions': ['get', 'post'],
  '/api/interactions/{id}': ['get', 'delete'],

  '/api/publications/{publicationId}/images': ['get', 'post'],
  '/api/publications/{publicationId}/images/{imageId}': ['delete'],
  '/api/publications/{publicationId}/images/{imageId}/content': ['get'],

  '/api/stores/{storeId}/inventory/low-stock': ['get'],
  '/api/stores/{storeId}/inventory/summary': ['get'],

  '/api/stores/{storeId}/orders': ['get'],
  '/api/stores/{storeId}/orders/{orderId}': ['get'],
  '/api/stores/{storeId}/orders/{orderId}/status': ['patch']
};

for (const [path, methods] of Object.entries(requiredOperations)) {
  for (const method of methods) {
    assert.ok(
      contract.paths?.[path]?.[method],
      `Missing OpenAPI operation: ${method.toUpperCase()} ${path}`
    );
  }
}

const content = JSON.stringify(canonical(contract), null, 2) + '\n';

if (process.argv.includes('--update')) {
  fs.writeFileSync(committed, content);
  console.log(`Contrato generado actualizado: ${committed}`);
} else {
  const expected = JSON.stringify(
    canonical(JSON.parse(fs.readFileSync(committed, 'utf8'))),
    null,
    2
  ) + '\n';

  assert.equal(
    content,
    expected,
    'OpenAPI drift: regenerar openapi.json desde Spring Boot y revisar el diff'
  );

  const count = Object.values(requiredOperations)
    .reduce((total, methods) => total + methods.length, 0);

  console.log(`OpenAPI: ${count} operaciones requeridas presentes; sin drift.`);
}
