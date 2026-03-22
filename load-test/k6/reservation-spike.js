/**
 * 시나리오 B: 스파이크 테스트 (콘서트 오픈 시뮬레이션)
 *
 * 목적: 인기 콘서트 티켓 오픈 시 급격한 트래픽 유입에 대한 시스템 내성 검증
 *
 * 실행 방법:
 *   k6 run load-test/k6/reservation-spike.js
 *
 * 결과 저장:
 *   k6 run --out json=load-test/results/spike-result.json load-test/k6/reservation-spike.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URL, SPIKE_THRESHOLDS, TEST_DATA, randomItem, headers } from './setup.js';

const tokenIssueDuration = new Trend('token_issue_duration');
const spikeErrors = new Rate('spike_errors');
const successCount = new Counter('success_count');

export const options = {
    stages: [
        { duration: '30s', target: 10 },    // 평상시 트래픽
        { duration: '10s', target: 500 },   // 스파이크: 10 → 500명 (콘서트 오픈)
        { duration: '1m', target: 500 },    // 최대 부하 유지
        { duration: '30s', target: 10 },    // 회복 구간
        { duration: '30s', target: 0 },     // 종료
    ],
    thresholds: SPIKE_THRESHOLDS,
};

export default function () {
    const userId = randomItem(TEST_DATA.userIds);

    // Step 1: 대기열 토큰 발급 (모든 유저가 먼저 대기열에 진입)
    const tokenStart = Date.now();
    const tokenRes = http.post(
        `${BASE_URL}/api/queue/token`,
        JSON.stringify({ userId: userId, concertId: TEST_DATA.concertId }),
        { headers: headers() }
    );
    tokenIssueDuration.add(Date.now() - tokenStart);

    const tokenOk = check(tokenRes, {
        'token issued': (r) => r.status === 200,
    });

    if (!tokenOk) {
        spikeErrors.add(1);
        sleep(0.1);
        return;
    }

    successCount.add(1);

    // 대기열 처리 지연 시뮬레이션
    sleep(Math.random() * 0.5);

    // Step 2: 좌석 예약 시도
    const seatId = randomItem(TEST_DATA.seatIds);
    const reserveRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
            userId: userId,
            scheduleId: TEST_DATA.scheduleId,
            seatId: seatId,
        }),
        { headers: headers() }
    );

    check(reserveRes, {
        'reservation attempted': (r) => r.status === 200 || r.status === 409, // 409: 좌석 이미 점유
    });

    sleep(0.5);
}
