import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    const payload = JSON.stringify([
        { id: 1, puntaje: 5, comentario: "Excelente" }
    ]);

    const res = http.post(`${BASE}/prestadores/calificaciones`, payload, {
        headers: { "Content-Type": "application/json" },
    });

    check(res, { "status 200": r => r.status === 200 });
    sleep(1);
}
