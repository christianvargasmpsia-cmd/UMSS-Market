import http from 'k6/http';
import { check } from 'k6';

// E3: same read-only endpoints and load as the formal A/B benchmark.
// The only change between windows is Toxiproxy's enabled state.
const BASE_URL = __ENV.E3_BASE_URL;
const PHASE = __ENV.E3_PHASE;
const ID = '22222222-2222-4222-8222-222222222222';
const RATE = Number(__ENV.E3_RATE || '5');
const DURATION = __ENV.E3_DURATION || '30s';

if (!BASE_URL) throw new Error('Falta E3_BASE_URL');
if (!['antes', 'falla', 'despues'].includes(PHASE)) {
  throw new Error('E3_PHASE debe ser antes, falla o despues');
}
if (!Number.isInteger(RATE) || RATE < 1 || RATE > 100) {
  throw new Error('E3_RATE debe ser un entero entre 1 y 100');
}

const scenario = {
  executor: 'constant-arrival-rate',
  rate: RATE,
  timeUnit: '1s',
  duration: DURATION,
  preAllocatedVUs: 8,
  maxVUs: 20,
  gracefulStop: '5s',
};

const thresholds = {
  // During the fault, reviews must all return the intentionally expected 503.
  // Publication errors are MEASURED, not masked or used to abort a negative H2 result.
  'checks{ruta:resenas}': ['rate==1'],
};
if (PHASE !== 'falla') {
  thresholds['checks{ruta:publicacion}'] = ['rate==1'];
  thresholds['http_req_failed{ruta:publicacion}'] = ['rate==0'];
  thresholds['http_req_failed{ruta:resenas}'] = ['rate==0'];
}

export const options = {
  scenarios: {
    publicaciones: {
      ...scenario,
      exec: 'medirPublicaciones',
      tags: { ruta: 'publicacion', fase: PHASE },
    },
    resenas: {
      ...scenario,
      exec: 'medirResenas',
      tags: { ruta: 'resenas', fase: PHASE },
    },
  },
  thresholds,
};

export function medirPublicaciones() {
  const response = http.get(`${BASE_URL}/api/publications/${ID}`, {
    timeout: '5s',
    tags: { ruta: 'publicacion', fase: PHASE },
  });
  check(response, {
    'publicacion HTTP 200': (r) => r.status === 200,
    'UUID correcto': (r) => r.status === 200 && r.json('id') === ID,
  });
}

export function medirResenas() {
  const response = http.get(`${BASE_URL}/api/publications/${ID}/reviews?limit=20`, {
    timeout: '5s',
    tags: { ruta: 'resenas', fase: PHASE },
  });

  if (PHASE === 'falla') {
    // A deliberate 503 is a passing functional check during an injected fault.
    // k6 still counts it in http_req_failed; it is NOT an unexpected outage.
    check(response, {
      'resenas HTTP 503 esperado': (r) => r.status === 503,
    });
  } else {
    check(response, {
      'resenas HTTP 200': (r) => r.status === 200,
      '20 resenas recibidas': (r) =>
        r.status === 200 && Array.isArray(r.json()) && r.json().length === 20,
    });
  }
}
