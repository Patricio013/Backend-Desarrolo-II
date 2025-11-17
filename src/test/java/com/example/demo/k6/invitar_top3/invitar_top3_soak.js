import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30m',                       
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<700'],
  },
};

const BASE_URL = __ENV.API_BASE || 'http://localhost:8080';
const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const r = http.post(`${BASE_URL}/solicitudes/invitar-top3`, "{}", { headers: HEADERS });
  check(r, { '200': x => x.status === 200 });
  sleep(1);
}
