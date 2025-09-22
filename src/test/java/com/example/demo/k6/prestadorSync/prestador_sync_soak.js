import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '2m' };
const BASE = __ENV.API_BASE;

export default function () {
    const prestador = JSON.stringify({ id: Math.floor(Math.random() * 1000), nombre: "Soak Test", rubro: "Carpintero" });
    let res = http.post(`${BASE}/prestadores-sync`, prestador, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /prestadores-sync -> 200": r => r.status === 200 });

    sleep(1);
}
