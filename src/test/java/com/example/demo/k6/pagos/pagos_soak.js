import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '2m' };
const BASE = __ENV.API_BASE;

export default function () {
    const payload = JSON.stringify({ monto: 500, descripcion: "Soak test" });

    let res = http.post(`${BASE}/pagos`, payload, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /pagos -> 200": r => r.status === 200 });

    res = http.get(`${BASE}/pagos/ultimas`);
    check(res, { "GET /pagos/ultimas -> 200": r => r.status === 200 });

    sleep(1);
}
