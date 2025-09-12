import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 1, duration: '10s' };
const BASE = __ENV.API_BASE;

export default function () {
    let res = http.post(`${BASE}/api/solicitudes/invitar-top3`);
    check(res, { "invitar top3 ok": r => r.status === 200 });

    res = http.patch(`${BASE}/api/solicitudes/1/cancelar`);
    check(res, { "cancelar solicitud ok": r => r.status === 200 || r.status === 404 });

    res = http.put(`${BASE}/api/solicitudes/path/1/recotizar`);
    check(res, { "recotizar solicitud ok": r => r.status === 200 || r.status === 204 || r.status === 404 });

    sleep(1);
}
