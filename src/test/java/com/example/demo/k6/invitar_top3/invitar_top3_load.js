// k6/invitar_top3_load.js
import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  scenarios: {
    ramping: {
      executor: 'ramping-arrival-rate',
      startRate: 5,          
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 200,
      stages: [
        { duration: '2m', target: 50 },  
        { duration: '1m', target: 20 },  
        { duration: '2m', target: 0 },   
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],        
    http_req_duration: ['p(95)<500'],      
  },
}

const BASE = __ENV.API_BASE || 'http://localhost:8080'

export default function () {
  const url = `${BASE}/api/solicitudes/invitar-top3`
  const res = http.post(url, JSON.stringify({}), {
    headers: { 'Content-Type': 'application/json' },
  })

  check(res, {
    'status is 200': r => r.status === 200,
    'body is array': r => {
      try {
        const data = r.json()
        return Array.isArray(data)
      } catch (e) {
        return false
      }
    },
  })

  sleep(0.2)
}
