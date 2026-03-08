package kr.hhplus.be.server.queue.infrastructure.web.dto;

public record IssueTokenRequest(
        Long userId,
        Long concertId
) {
}
