package kr.hhplus.be.server.ranking.infrastructure.web.dto;

import kr.hhplus.be.server.ranking.application.port.in.RankingEntry;

public record RankingResponse(int rank, Long concertId, long soldCount) {

    public static RankingResponse from(RankingEntry entry) {
        return new RankingResponse(entry.rank(), entry.concertId(), entry.soldCount());
    }
}
