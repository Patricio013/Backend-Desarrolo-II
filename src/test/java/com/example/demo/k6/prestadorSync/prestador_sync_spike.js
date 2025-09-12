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
    const prestador = JSON.stringify({
        id: Math.floor(Math.random() * 10000),
        nombre: "Spike Test",
        rubro: "Jardinero"
    });

    let res = http.post(`${BASE}/api/prestadores-sync`, prestador, {
        headers: { "Content-Type": "application/json" },
    });
    check(res, { "POST /api/prestadores-sync -> 200": r => r.status === 200 });
}
