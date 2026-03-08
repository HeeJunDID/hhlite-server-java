package kr.hhplus.be.server.queue.domain;

public sealed interface QueueStatusResult
        permits QueueStatusResult.Waiting, QueueStatusResult.Admitted, QueueStatusResult.Invalid {
    record Waiting(long position, long nextPollMs) implements QueueStatusResult {}

    record Admitted(String accessToken, long expiresInSec) implements QueueStatusResult {}

    record Invalid(String reason) implements QueueStatusResult {}

}
