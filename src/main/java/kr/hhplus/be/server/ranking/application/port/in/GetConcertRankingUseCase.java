package kr.hhplus.be.server.ranking.application.port.in;

import java.util.List;

public interface GetConcertRankingUseCase {
    List<RankingEntry> getTopRankings(int count);
}
