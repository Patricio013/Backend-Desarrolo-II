import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '2m' };
const BASE = __ENV.API_BASE;

export default function () {
    let res1 = http.post(`${BASE}/api/solicitudes/invitar-top3`, "{}",
        { headers: { "Content-Type": "application/json" } });
    check(res1, { "POST /invitar-top3 200": r => r.status === 200 });

    let res2 = http.post(`${BASE}/api/solicitudes/crear`,
        JSON.stringify([{ descripcion: "Solicitud soak test" }]),
        { headers: { "Content-Type": "application/json" } });
    check(res2, { "POST /crear 200": r => r.status === 200 });

    let res3 = http.patch(`${BASE}/api/solicitudes/1/cancelar`);
    check(res3, { "PATCH /cancelar 200|204": r => r.status === 200 || r.status === 204 });

    let res4 = http.put(`${BASE}/api/solicitudes/path/1/recotizar`);
    check(res4, { "PUT /recotizar 200|204": r => r.status === 200 || r.status === 204 });

    sleep(1);
}
