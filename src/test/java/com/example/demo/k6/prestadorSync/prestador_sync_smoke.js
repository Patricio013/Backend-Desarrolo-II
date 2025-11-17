import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    const prestador = JSON.stringify({ id: 1, nombre: "Prestador Test", rubro: "Electricista" });
    let res = http.post(`${BASE}/prestadores-sync`, prestador, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /prestadores-sync -> 200": r => r.status === 200 });

    const batch = JSON.stringify([
        { id: 2, nombre: "Prestador Batch 1", rubro: "Plomero" },
        { id: 3, nombre: "Prestador Batch 2", rubro: "Gasista" },
    ]);
    res = http.post(`${BASE}/prestadores-sync/batch`, batch, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /prestadores-sync/batch -> 200": r => r.status === 200 });

    sleep(1);
}
