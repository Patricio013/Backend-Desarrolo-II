import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    // Crear pago
    let res = http.post(`${BASE}/api/pagos`, JSON.stringify({
        descripcion: "Pago smoke test",
        monto: 1500.0
    }), { headers: { "Content-Type": "application/json" } });
    check(res, { "crear pago ok": r => r.status === 200 || r.status === 201 });

    res = http.get(`${BASE}/api/pagos/ultimas`);
    check(res, { "ultimas pagos ok": r => r.status === 200 });

    res = http.get(`${BASE}/api/pagos/1`);
    check(res, { "get pago ok": r => r.status === 200 || r.status === 404 });

    sleep(1);
}
