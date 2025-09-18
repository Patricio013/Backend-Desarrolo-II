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
    const payload = JSON.stringify({ monto: 999, descripcion: "Spike test" });

    let res = http.post(`${BASE}/api/pagos`, payload, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /api/pagos -> 200": r => r.status === 200 });

    res = http.get(`${BASE}/api/pagos/ultimas`);
    check(res, { "GET /api/pagos/ultimas -> 200": r => r.status === 200 });
}
