package kr.hhplus.be.server.queue.application.port.in;

import kr.hhplus.be.server.queue.domain.TokenStatus;

import java.time.LocalDateTime;

public record TokenResult(
        Long tokenId,
        String token,
        Long userId,
        Long concertId,
        TokenStatus status,
        Long position,
        LocalDateTime expiredAt
) {
}
