package kr.hhplus.be.server.queue.domain;

import java.time.Instant;

public record QueueEntry(
        QueueToken token,
        Long concertId,
        Long userId,
        Instant enqueuedAt
) {
    public static QueueEntry of(QueueToken token, Long concertId, Long userId, Instant now) {
        return new QueueEntry(token, concertId, userId, now);
    }
}
