package kr.hhplus.be.server.queue.application.port.in;

public record IssueTokenCommand(
        Long userId,
        Long concertId
) {
}
