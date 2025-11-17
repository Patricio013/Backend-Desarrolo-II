import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 5 },     
    { duration: '5s',  target: 200 },   
    { duration: '30s', target: 5 },     
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.03'],
    http_req_duration: ['p(95)<900'],
  },
};

const BASE_URL = __ENV.API_BASE || 'http://localhost:8080';
const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const r = http.post(`${BASE_URL}/solicitudes/invitar-top3`, "{}", { headers: HEADERS });
  check(r, { '200': x => x.status === 200 });
}
