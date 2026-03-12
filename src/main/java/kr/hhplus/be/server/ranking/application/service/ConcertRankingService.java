package kr.hhplus.be.server.ranking.application.service;

import kr.hhplus.be.server.ranking.application.port.in.GetConcertRankingUseCase;
import kr.hhplus.be.server.ranking.application.port.in.RankingEntry;
import kr.hhplus.be.server.ranking.application.port.out.ConcertRankingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertRankingService implements GetConcertRankingUseCase {

    private final ConcertRankingPort concertRankingPort;

    @Override
    public List<RankingEntry> getTopRankings(int count) {
        return concertRankingPort.getTopRankings(count);
    }
}
