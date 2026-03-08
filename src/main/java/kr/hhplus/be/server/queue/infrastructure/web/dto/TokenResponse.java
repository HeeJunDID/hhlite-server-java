package kr.hhplus.be.server.queue.infrastructure.web.dto;

import kr.hhplus.be.server.queue.application.port.in.TokenResult;
import kr.hhplus.be.server.queue.domain.TokenStatus;

import java.time.LocalDateTime;

public record TokenResponse(
        Long tokenId,
        String token,
        Long userId,
        Long concertId,
        TokenStatus status,
        Long position,
        LocalDateTime expiredAt
) {
    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(
                result.tokenId(),
                result.token(),
                result.userId(),
                result.concertId(),
                result.status(),
                result.position(),
                result.expiredAt()
        );
    }
}
