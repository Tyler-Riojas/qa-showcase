/**
 * booking-stress-test.js
 *
 * Ramping stress test — climbs from 0 to 20 VUs over 30 s, holds for 20 s,
 * then ramps back down over 10 s.  Exercises GET /booking and GET /booking/1
 * to simulate mixed real-world read traffic.
 *
 * Run:
 *   k6 run booking-stress-test.js
 *   k6 run --out json=stress-results.json booking-stress-test.js
 *
 * Override base URL:
 *   k6 run -e BASE_URL=https://restful-booker.herokuapp.com booking-stress-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:3001';

const errorRate        = new Rate('stress_errors');
const listLatency      = new Trend('list_bookings_latency', true);
const byIdLatency      = new Trend('booking_by_id_latency', true);
const notFoundCounter  = new Counter('booking_not_found');

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ramp up to peak load
    { duration: '20s', target: 20 },  // hold at peak
    { duration: '10s', target: 0  },  // ramp down
  ],

  thresholds: {
    // Measured across both endpoints combined
    'http_req_duration':        ['p(95)<500'],
    'http_req_failed':          ['rate<0.01'],
    'stress_errors':            ['rate<0.01'],
    // Per-endpoint latency breakdown
    'list_bookings_latency':    ['p(95)<500'],
    'booking_by_id_latency':    ['p(95)<500'],
  },
};

export default function () {
  group('GET /booking — list all', function () {
    const res = http.get(`${BASE_URL}/booking`, {
      headers: { 'Accept': 'application/json' },
      tags:    { endpoint: 'list_bookings' },
    });

    const failed = res.status !== 200;
    errorRate.add(failed);
    listLatency.add(res.timings.duration);

    check(res, {
      'list: status 200':              (r) => r.status === 200,
      'list: returns array':           (r) => {
        try { return Array.isArray(JSON.parse(r.body)); }
        catch (_) { return false; }
      },
      'list: response under 500 ms':  (r) => r.timings.duration < 500,
    });
  });

  sleep(0.3);

  group('GET /booking/{id} — single record', function () {
    // Rotate through IDs 1–5 so we don't hammer a single row
    const id  = ((__VU + __ITER) % 5) + 1;
    const res = http.get(`${BASE_URL}/booking/${id}`, {
      headers: { 'Accept': 'application/json' },
      tags:    { endpoint: 'booking_by_id' },
    });

    // 404 is acceptable if the booking doesn't exist (seed data may vary)
    const failed = res.status !== 200 && res.status !== 404;
    if (res.status === 404) notFoundCounter.add(1);
    errorRate.add(failed);
    byIdLatency.add(res.timings.duration);

    check(res, {
      'by-id: status 200 or 404':     (r) => r.status === 200 || r.status === 404,
      'by-id: response under 500 ms': (r) => r.timings.duration < 500,
    });
  });

  sleep(0.5);
}
