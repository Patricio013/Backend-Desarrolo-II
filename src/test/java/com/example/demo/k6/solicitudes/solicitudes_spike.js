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
    let res1 = http.post(`${BASE}/solicitudes/invitar-top3`, "{}",
        { headers: { "Content-Type": "application/json" } });
    check(res1, { "POST /invitar-top3 200": r => r.status === 200 });

    let res2 = http.post(`${BASE}/solicitudes/crear`,
        JSON.stringify([{ descripcion: "Solicitud spike test" }]),
        { headers: { "Content-Type": "application/json" } });
    check(res2, { "POST /crear 200": r => r.status === 200 });

    let res3 = http.patch(`${BASE}/solicitudes/1/cancelar`);
    check(res3, { "PATCH /cancelar 200|204": r => r.status === 200 || r.status === 204 });

    let res4 = http.put(`${BASE}/solicitudes/path/1/recotizar`);
    check(res4, { "PUT /recotizar 200|204": r => r.status === 200 || r.status === 204 });
}
