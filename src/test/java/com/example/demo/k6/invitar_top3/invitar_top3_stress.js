import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '30s', target: 150 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],      
    http_req_duration: ['p(95)<800'],    
  },
};

const BASE_URL = __ENV.API_BASE || 'http://localhost:8080';
const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const r = http.post(`${BASE_URL}/api/solicitudes/invitar-top3`, "{}", { headers: HEADERS });
  check(r, { '200': x => x.status === 200 });
}
