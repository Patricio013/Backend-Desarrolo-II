import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '5s', target: 5 },
        { duration: '5s', target: 200 },
        { duration: '10s', target: 5 },
        { duration: '5s', target: 0 },
    ],
};
const BASE = __ENV.API_BASE;

export default function () {
    let res = http.post(`${BASE}/api/solicitudes/invitar-top3`);
    check(res, { "invitar top3 spike ok": r => r.status === 200 });
}
