package kr.hhplus.be.server.ranking.application.port.in;

public record RankingEntry(Long concertId, long soldCount, int rank) {
}
