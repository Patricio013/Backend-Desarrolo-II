import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    const payload = JSON.stringify({ monto: 1000, descripcion: "Pago test" });
    let res = http.post(`${BASE}/pagos`, payload, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /pagos -> 200": r => r.status === 200 });

    res = http.get(`${BASE}/pagos/ultimas`);
    check(res, { "GET /pagos/ultimas -> 200": r => r.status === 200 });

    res = http.get(`${BASE}/pagos/1`);
    check(res, { "GET /pagos/{id} -> 200/404": r => r.status === 200 || r.status === 404 });

    sleep(1);
}
