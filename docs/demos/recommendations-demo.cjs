// Node 22+. Only run against the dedicated local demo instance seeded by recommendations-seed.sql.
const assert = require('node:assert/strict');
const base = process.env.DEMO_BASE_URL || 'http://127.0.0.1:18089';
assert.ok(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Only a local isolated demo instance');
assert.equal(process.env.DEMO_CONFIRM_ISOLATED, 'umss_recommendations_demo', 'Confirm isolated database name');
const password = process.env.UMSS_DEMO_BUYER_PASSWORD;
assert.ok(password, 'Set UMSS_DEMO_BUYER_PASSWORD; never commit it');
async function request(path, token, method = 'GET', body, expected = 200) {
  const response = await fetch(base + path, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  assert.equal(response.status, expected, `${method} ${path}`);
  const text = await response.text();
  return text ? JSON.parse(text) : undefined;
}
async function main() {
  const accounts = [];
  for (const name of ['a', 'b', 'c']) {
    const login = await request('/api/auth/login', null, 'POST', { email: `buyer-${name}@demo.invalid`, password });
    // Reset ONLY this synthetic account's interactions so the demo can be repeated.
    for (const item of await request('/api/interactions', login.token))
      await request(`/api/interactions/${item.id}`, login.token, 'DELETE', undefined, 204);
    accounts.push(login);
  }
  const [a,b,c] = accounts;
  const before = await request('/api/recommendations?limit=10', a.token);
  assert.equal(before.personalized, false);
  const sourceA = '00000000-0000-0000-0000-000000000101';
  const sourceB = '00000000-0000-0000-0000-000000000201';
  const interactionA = await request('/api/interactions', a.token, 'POST', { publicationId: sourceA, type: 'FAVORITE' }, 201);
  await request('/api/interactions', b.token, 'POST', { publicationId: sourceB, type: 'PURCHASE' }, 201);
  const recA = await request('/api/recommendations?limit=10', a.token);
  const recB = await request('/api/recommendations?limit=10', b.token);
  assert.equal(recA.personalized, true); assert.equal(recB.personalized, true);
  assert.notEqual(recA.items[0].id, recB.items[0].id);
  assert.match(recA.items[0].name, /Tecnologia/); assert.match(recB.items[0].name, /Deporte/);
  assert.ok(recA.items[0].affinity > 0); assert.ok(recB.items[0].affinity > 0);
  assert.ok(!recA.items.some(p => p.id === sourceA)); assert.ok(!recB.items.some(p => p.id === sourceB));
  const general = await request('/api/recommendations?limit=50', c.token);
  assert.equal(general.personalized, false); assert.equal(general.items.length, 12);
  for (const result of [recA,recB,general]) {
    assert.ok(result.items.every(p => p.stock > 0 && !['00000000-0000-0000-0000-000000000301','00000000-0000-0000-0000-000000000302'].includes(p.id)));
    if (process.env.DEMO_EXPECT_FALLBACK === 'true') assert.equal(result.aiStatus, 'FALLBACK');
  }
  const spoof = await request(`/api/recommendations?limit=10&userId=${b.userId}`, a.token);
  assert.deepEqual(spoof.items, recA.items);
  await request(`/api/interactions/${interactionA.id}`, b.token, 'GET', undefined, 404);
  await request(`/api/interactions/${interactionA.id}`, b.token, 'DELETE', undefined, 404);
  await request(`/api/interactions/${interactionA.id}`, a.token);
  for (const limit of [1,5,10]) assert.equal((await request(`/api/recommendations?limit=${limit}`, a.token)).items.length, limit);
  for (const limit of ['0','51','abc']) await request(`/api/recommendations?limit=${limit}`, a.token, 'GET', undefined, 400);
  await request('/api/recommendations', null, 'GET', undefined, 403);
  await request('/api/recommendations', 'invalid', 'GET', undefined, 401);
  const spec = await request('/v3/api-docs');
  assert.ok(spec.paths['/api/recommendations'].get);
  console.log(JSON.stringify({ result: 'PASS', scenarios: ['A personalization','B no history','C availability','D isolation','E limits','interaction -> ranking','authentication','OpenAPI'],
    buyerA: recA, buyerB: recB, noHistory: general }, null, 2));
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
