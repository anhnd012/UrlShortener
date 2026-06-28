import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:10689';
const SHORT_CODE = __ENV.SHORT_CODE || 'aB12xYz9';

export default function () {
  const response = http.get(`${BASE_URL}/${SHORT_CODE}`, {
    redirects: 0,
  });

  check(response, {
    'returns 302': (res) => res.status === 302,
    'has Location header': (res) => Boolean(res.headers.Location),
  });
}
