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
    const payload = JSON.stringify([{ id: 2, puntaje: 3, comentario: "Regular" }]);

    const res = http.post(`${BASE}/api/prestadores/calificaciones`, payload, {
        headers: { "Content-Type": "application/json" },
    });

    check(res, { "status 200": r => r.status === 200 });
}
