// 공통 설정 및 유틸리티
export const BASE_URL = 'http://localhost:8080';

export const THRESHOLDS = {
    http_req_duration: ['p(95)<500'],  // 95%의 요청이 500ms 이내
    http_req_failed: ['rate<0.01'],    // 에러율 1% 미만
};

export const SPIKE_THRESHOLDS = {
    http_req_duration: ['p(95)<2000'], // 스파이크 구간은 2초 허용
    http_req_failed: ['rate<0.05'],    // 에러율 5% 미만
};

// 테스트용 시드 데이터 (DB에 미리 입력되어 있다고 가정)
export const TEST_DATA = {
    userIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
    concertId: 1,
    scheduleId: 1,
    seatIds: Array.from({ length: 50 }, (_, i) => i + 1), // 좌석 1~50
};

export function randomItem(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

export function headers(token = null) {
    const h = { 'Content-Type': 'application/json' };
    if (token) h['Authorization'] = `Bearer ${token}`;
    return h;
}
