package kr.hhplus.be.server.ranking.infrastructure.web;

import kr.hhplus.be.server.ranking.application.port.in.GetConcertRankingUseCase;
import kr.hhplus.be.server.ranking.infrastructure.web.dto.RankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/concerts/rankings")
@RequiredArgsConstructor
public class RankingController {

    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 100;

    private final GetConcertRankingUseCase getConcertRankingUseCase;

    @GetMapping
    public ResponseEntity<List<RankingResponse>> getTopRankings(
            @RequestParam(defaultValue = "10") int top
    ) {
        int count = Math.min(top, MAX_TOP);
        List<RankingResponse> rankings = getConcertRankingUseCase.getTopRankings(count)
                .stream()
                .map(RankingResponse::from)
                .toList();
        return ResponseEntity.ok(rankings);
    }
}
