import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '15s', target: 20 },
        { duration: '15s', target: 50 },
        { duration: '15s', target: 100 },
        { duration: '15s', target: 0 },
    ],
};
const BASE = __ENV.API_BASE;

export default function () {
    let res = http.get(`${BASE}/api/pagos/ultimas`);
    check(res, { "ultimas pagos stress ok": r => r.status === 200 });
}
