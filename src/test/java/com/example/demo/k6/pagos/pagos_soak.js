import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '2m' };
const BASE = __ENV.API_BASE;

export default function () {
    let res = http.get(`${BASE}/api/pagos/ultimas`);
    check(res, { "ultimas pagos soak ok": r => r.status === 200 });
    sleep(1);
}
