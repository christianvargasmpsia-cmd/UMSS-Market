import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.E3_BASE_URL;
const ID = '22222222-2222-4222-8222-222222222222';

const RATE = Number(__ENV.E3_RATE || '5');
const DURATION = __ENV.E3_DURATION || '30s';

if (!BASE_URL) {
    throw new Error('Falta E3_BASE_URL');
}

if (!Number.isInteger(RATE) || RATE < 1) {
    throw new Error('E3_RATE debe ser entero positivo');
}

const carga = {
    executor: 'constant-arrival-rate',
    rate: RATE,
    timeUnit: '1s',
    duration: DURATION,
    preAllocatedVUs: 8,
    maxVUs: 20,
    gracefulStop: '5s',
};

export const options = {
    scenarios: {
        publicaciones: {
            ...carga,
            exec: 'medirPublicaciones',
            tags: { ruta: 'publicacion' },
        },
        resenas: {
            ...carga,
            exec: 'medirResenas',
            tags: { ruta: 'resenas' },
        },
    },
    thresholds: {
        'checks{ruta:publicacion}': ['rate==1'],
        'checks{ruta:resenas}': ['rate==1'],
        'http_req_failed{ruta:publicacion}': ['rate==0'],
        'http_req_failed{ruta:resenas}': ['rate==0'],
    },
};

export function medirPublicaciones() {
    const response = http.get(
        `${BASE_URL}/api/publications/${ID}`,
        {
            timeout: '5s',
            tags: { ruta: 'publicacion' },
        }
    );

    check(response, {
        'publicacion HTTP 200': (r) => r.status === 200,
        'UUID correcto': (r) =>
            r.status === 200 && r.json('id') === ID,
    });
}

export function medirResenas() {
    const response = http.get(
        `${BASE_URL}/api/publications/${ID}/reviews?limit=20`,
        {
            timeout: '5s',
            tags: { ruta: 'resenas' },
        }
    );

    check(response, {
        'resenas HTTP 200': (r) => r.status === 200,
        '20 resenas recibidas': (r) =>
            r.status === 200 &&
            Array.isArray(r.json()) &&
            r.json().length === 20,
    });
}
