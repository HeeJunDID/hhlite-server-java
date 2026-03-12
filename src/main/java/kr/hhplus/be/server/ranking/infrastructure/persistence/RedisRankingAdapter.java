package kr.hhplus.be.server.ranking.infrastructure.persistence;

import kr.hhplus.be.server.ranking.application.port.in.RankingEntry;
import kr.hhplus.be.server.ranking.application.port.out.ConcertRankingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisRankingAdapter implements ConcertRankingPort {

    private static final String RANKING_KEY = "ranking:concert:sold";

    private final StringRedisTemplate redis;

    @Override
    public void incrementSoldCount(Long concertId) {
        redis.opsForZSet().incrementScore(RANKING_KEY, concertId.toString(), 1.0);
    }

    @Override
    public List<RankingEntry> getTopRankings(int count) {
        Set<ZSetOperations.TypedTuple<String>> result =
                redis.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, count - 1);

        if (result == null) return List.of();

        List<RankingEntry> rankings = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : result) {
            Long concertId = Long.parseLong(tuple.getValue());
            long soldCount = tuple.getScore().longValue();
            rankings.add(new RankingEntry(concertId, soldCount, rank++));
        }
        return rankings;
    }
}
