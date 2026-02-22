package kr.hhplus.be.server.queue.domain;

import java.util.UUID;

public record QueueToken(UUID value) {

    // 토큰 생성
    public static QueueToken generate() {
        return new QueueToken(UUID.randomUUID());
    }

    // UUID 출력
    public String asString() {
        return value.toString();
    }


}
