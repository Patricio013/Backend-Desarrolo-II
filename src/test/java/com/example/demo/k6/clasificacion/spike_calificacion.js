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
    const payload = JSON.stringify([{ id: 4, puntaje: 1, comentario: "Muy malo" }]);

    const res = http.post(`${BASE}/prestadores/calificaciones`, payload, {
        headers: { "Content-Type": "application/json" },
    });

    check(res, { "status 200": r => r.status === 200 });
}
