import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
  },
};

const BASE_URL = __ENV.API_BASE || 'http://localhost:8080';
const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const r = http.post(`${BASE_URL}/api/solicitudes/invitar-top3`, "{}", { headers: HEADERS });
  check(r, { '200': x => x.status === 200, 'array': x => Array.isArray(x.json()) });
  sleep(1);
}
