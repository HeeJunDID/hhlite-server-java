package kr.hhplus.be.server.ranking.application.port.out;

import kr.hhplus.be.server.ranking.application.port.in.RankingEntry;

import java.util.List;

public interface ConcertRankingPort {
    void incrementSoldCount(Long concertId);
    List<RankingEntry> getTopRankings(int count);
}
