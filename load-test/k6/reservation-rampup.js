/**
 * 시나리오 A: 점진적 부하 증가 (Ramp-up Test)
 *
 * 목적: 시스템이 감당할 수 있는 최대 처리량과 병목 지점 파악
 *
 * 실행 방법:
 *   k6 run load-test/k6/reservation-rampup.js
 *
 * 결과 저장:
 *   k6 run --out json=load-test/results/rampup-result.json load-test/k6/reservation-rampup.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, THRESHOLDS, TEST_DATA, randomItem, headers } from './setup.js';

// 커스텀 메트릭
const reservationDuration = new Trend('reservation_duration');
const paymentDuration = new Trend('payment_duration');
const reservationErrors = new Rate('reservation_errors');

export const options = {
    stages: [
        { duration: '30s', target: 10 },   // 워밍업: 10명
        { duration: '1m', target: 100 },   // 부하 증가: 10 → 100명
        { duration: '2m', target: 100 },   // 최대 부하 유지: 100명
        { duration: '30s', target: 0 },    // 부하 감소
    ],
    thresholds: THRESHOLDS,
};

export default function () {
    const userId = randomItem(TEST_DATA.userIds);
    const seatId = randomItem(TEST_DATA.seatIds);

    // Step 1: 대기열 토큰 발급
    const tokenRes = http.post(
        `${BASE_URL}/api/queue/token`,
        JSON.stringify({ userId: userId, concertId: TEST_DATA.concertId }),
        { headers: headers() }
    );
    check(tokenRes, {
        'token issued': (r) => r.status === 200,
    });

    if (tokenRes.status !== 200) {
        reservationErrors.add(1);
        return;
    }

    sleep(0.5);

    // Step 2: 좌석 예약
    const reserveStart = Date.now();
    const reserveRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
            userId: userId,
            scheduleId: TEST_DATA.scheduleId,
            seatId: seatId,
        }),
        { headers: headers() }
    );
    reservationDuration.add(Date.now() - reserveStart);

    const reserveOk = check(reserveRes, {
        'reservation success': (r) => r.status === 200,
    });

    if (!reserveOk) {
        reservationErrors.add(1);
        sleep(1);
        return;
    }

    const reservation = reserveRes.json();
    sleep(0.3);

    // Step 3: 결제
    const payStart = Date.now();
    const payRes = http.post(
        `${BASE_URL}/api/payments`,
        JSON.stringify({
            reservationId: reservation.reservationId,
            userId: userId,
        }),
        { headers: headers() }
    );
    paymentDuration.add(Date.now() - payStart);

    check(payRes, {
        'payment success': (r) => r.status === 200,
    });

    sleep(1);
}
