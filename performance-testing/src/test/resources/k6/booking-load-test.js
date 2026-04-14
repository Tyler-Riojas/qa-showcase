/**
 * booking-load-test.js
 *
 * Simple load test — 5 VUs, 20 s constant load against GET /booking.
 *
 * Run:
 *   k6 run booking-load-test.js
 *   k6 run --out json=results.json booking-load-test.js
 *
 * Override base URL:
 *   k6 run -e BASE_URL=https://restful-booker.herokuapp.com booking-load-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';

// Custom metrics for cleaner reporting
const errorRate   = new Rate('booking_errors');
const loadLatency = new Trend('booking_load_latency', true); // true = display as ms

export const options = {
  vus:      5,
  duration: '20s',

  thresholds: {
    // 95th-percentile response time must stay under 500 ms
    'http_req_duration': ['p(95)<500'],
    // Fewer than 1% of requests may fail
    'http_req_failed': ['rate<0.01'],
    // Custom metric mirrors the http_req_failed threshold
    'booking_errors': ['rate<0.01'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/booking`, {
    headers: { 'Accept': 'application/json' },
    tags:    { endpoint: 'get_bookings' },
  });

  // Record custom metrics
  const failed = res.status !== 200;
  errorRate.add(failed);
  loadLatency.add(res.timings.duration);

  // Inline checks — failures are counted but do NOT abort the test
  check(res, {
    'status is 200':               (r) => r.status === 200,
    'response is JSON array':      (r) => {
      try { return Array.isArray(JSON.parse(r.body)); }
      catch (_) { return false; }
    },
    'response time under 500 ms':  (r) => r.timings.duration < 500,
    'content-type is json':        (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });

  // Short think-time between iterations (realistic pacing)
  sleep(0.5);
}
