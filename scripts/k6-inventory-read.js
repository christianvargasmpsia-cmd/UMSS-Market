import http from 'k6/http';
import { check } from 'k6';

// SOLO contra entorno de PRUEBAS. No generar carga en produccion.
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const storeId = __ENV.STORE_ID;
const jwt = __ENV.JWT;
if (!storeId || !jwt) throw new Error('Faltan variables STORE_ID y JWT');

export const options = {
    vus: 2,
    duration: '15s',
    summaryTrendStats: ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const url = `${baseUrl}/api/stores/${storeId}/inventory/low-stock?threshold=5&page=0&size=10`;
    const response = http.get(url, { headers: { Authorization: `Bearer ${jwt}` } });
    check(response, {
        'HTTP 200': r => r.status === 200,
        'cuerpo con lista': r => {
            try { return Array.isArray(r.json('items')); }
            catch (e) { return false; }
        },
    });
}
