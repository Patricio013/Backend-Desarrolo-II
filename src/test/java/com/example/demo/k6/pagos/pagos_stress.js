import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '20s', target: 20 },
        { duration: '20s', target: 50 },
        { duration: '20s', target: 100 },
        { duration: '20s', target: 0 },
    ],
};
const BASE = __ENV.API_BASE;

export default function () {
    const payload = JSON.stringify({ monto: 200, descripcion: "Stress test" });

    let res = http.post(`${BASE}/pagos`, payload, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /pagos -> 200": r => r.status === 200 });

    res = http.get(`${BASE}/pagos/ultimas`);
    check(res, { "GET /pagos/ultimas -> 200": r => r.status === 200 });
}
