import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const failedChecks = new Rate('catalog_failed_checks');
const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const catalogQuery = __ENV.CATALOG_QUERY || '';
const endpoint = `${baseUrl}/api/publications${catalogQuery ? `?texto=${encodeURIComponent(catalogQuery)}` : ''}`;

export const options = {
  scenarios: {
    catalog_fixed_rate: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 10),
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: Number(__ENV.PREALLOCATED_VUS || 10),
      maxVUs: Number(__ENV.MAX_VUS || 30),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    catalog_failed_checks: ['rate<0.01'],
  },
};


export default function () {
  const response = http.get(endpoint, {
    headers: { Accept: 'application/json' },
    tags: {
      feature: 'feature-2-k6',
      endpoint: '/api/publications',
    },
  });

  const httpOk = check(response, {
    'catalog responds HTTP 200': (r) => r.status === 200,
  });

  const jsonOk = check(response, {
    'catalog response is JSON': (r) =>
      (r.headers['Content-Type'] || '').includes('application/json'),
  });

  failedChecks.add(!(httpOk && jsonOk));
}

