import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    // === Solicitudes ===
    let res1 = http.post(`${BASE}/api/solicitudes/invitar-top3`, "{}", {
        headers: { "Content-Type": "application/json" },
    });
    check(res1, { "POST /solicitudes/invitar-top3 200": r => r.status === 200 });

    let res2 = http.post(`${BASE}/api/solicitudes/crear`,
        JSON.stringify([{ descripcion: "Nueva solicitud smoke" }]),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res2, { "POST /solicitudes/crear 200": r => r.status === 200 });

    let res3 = http.patch(`${BASE}/api/solicitudes/1/cancelar`);
    check(res3, { "PATCH /solicitudes/{id}/cancelar 200|204": r => r.status === 200 || r.status === 204 });

    let res4 = http.put(`${BASE}/api/solicitudes/path/1/recotizar`);
    check(res4, { "PUT /solicitudes/path/{id}/recotizar 200|204": r => r.status === 200 || r.status === 204 });

    // === Pagos ===
    let res5 = http.post(`${BASE}/api/pagos`,
        JSON.stringify({ monto: 1000, descripcion: "Smoke pago" }),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res5, { "POST /pagos 200": r => r.status === 200 });

    let res6 = http.get(`${BASE}/api/pagos/ultimas`);
    check(res6, { "GET /pagos/ultimas 200": r => r.status === 200 });

    let res7 = http.get(`${BASE}/api/pagos/1`);
    check(res7, { "GET /pagos/{id} 200|404": r => r.status === 200 || r.status === 404 });

    // === Prestadores Sync ===
    let res8 = http.post(`${BASE}/api/prestadores-sync`,
        JSON.stringify({ id: 1, nombre: "Prestador Smoke" }),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res8, { "POST /prestadores-sync 200": r => r.status === 200 });

    let res9 = http.post(`${BASE}/api/prestadores-sync/batch`,
        JSON.stringify([{ id: 1, nombre: "Batch Prestador Smoke" }]),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res9, { "POST /prestadores-sync/batch 200": r => r.status === 200 });

    // === Calificaciones ===
    let res10 = http.post(`${BASE}/api/prestadores/calificaciones`,
        JSON.stringify([{ id: 1, score: 5, comentario: "Muy bien" }]),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res10, { "POST /prestadores/calificaciones 200": r => r.status === 200 });

    sleep(1);
}
