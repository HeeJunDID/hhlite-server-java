package kr.hhplus.be.server.queue.domain;

public enum TokenStatus {
    WAITING,    // 대기 중
    ACTIVE,     // 활성화 (입장 가능)
    EXPIRED     // 만료됨
}
