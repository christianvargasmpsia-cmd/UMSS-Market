import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1,
    iterations: 3,
    thresholds: {
        checks: ['rate==1'],
        http_req_failed: ['rate==0'],
    },
};

const BASE_URL = __ENV.E3_BASE_URL;
const ID = '22222222-2222-4222-8222-222222222222';

if (!BASE_URL) {
    throw new Error('Falta E3_BASE_URL');
}

export default function () {
    const publicacion = http.get(
        `${BASE_URL}/api/publications/${ID}`,
        { tags: { ruta: 'publicacion' } }
    );

    check(publicacion, {
        'publicacion HTTP 200': (r) => r.status === 200,
        'publicacion UUID correcto': (r) =>
            r.status === 200 && r.json('id') === ID,
    });

    const resenas = http.get(
        `${BASE_URL}/api/publications/${ID}/reviews?limit=20`,
        { tags: { ruta: 'resenas' } }
    );

    check(resenas, {
        'resenas HTTP 200': (r) => r.status === 200,
        'resenas devuelve 20 registros': (r) =>
            r.status === 200 &&
            Array.isArray(r.json()) &&
            r.json().length === 20,
    });
}
