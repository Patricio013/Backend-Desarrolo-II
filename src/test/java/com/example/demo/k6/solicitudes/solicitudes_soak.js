import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '2m' };
const BASE = __ENV.API_BASE;

export default function () {
    let res = http.post(`${BASE}/api/solicitudes/crear`,
        JSON.stringify([{ descripcion: "Soak test solicitud" }]),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res, { "POST /crear 200": r => r.status === 200 });
    sleep(1);
}
