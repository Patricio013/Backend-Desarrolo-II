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
    // Invitar top3
    let res1 = http.post(`${BASE}/solicitudes/invitar-top3`, "{}", {
        headers: { "Content-Type": "application/json" },
    });
    check(res1, { "POST /invitar-top3 200": r => r.status === 200 });

    // Crear solicitudes
    let res2 = http.post(`${BASE}/solicitudes/crear`,
        JSON.stringify([{ descripcion: "Nueva solicitud de prueba" }]),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res2, { "POST /crear 200": r => r.status === 200 });

    // Cancelar
    let res3 = http.patch(`${BASE}/solicitudes/1/cancelar`);
    check(res3, { "PATCH /cancelar 200|204": r => r.status === 200 || r.status === 204 });

    // Recotizar
    let res4 = http.put(`${BASE}/solicitudes/path/1/recotizar`);
    check(res4, { "PUT /recotizar 200|204": r => r.status === 200 || r.status === 204 });
}
