package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.queue.application.port.out.QueueStore;
import kr.hhplus.be.server.queue.application.port.out.QueueStore.TokenMapping;
import kr.hhplus.be.server.queue.domain.QueueEntry;
import kr.hhplus.be.server.queue.domain.QueueStatusResult;
import kr.hhplus.be.server.queue.domain.QueueToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class QueueService {

    private final QueueStore queueStore;

    public QueueService(QueueStore queueStore) {
        this.queueStore = queueStore;
    }


    public QueueToken enter(Long concertId, Long userId) {
        long score = System.currentTimeMillis();

        QueueToken queueToken = new QueueToken(UUID.randomUUID());

        this.queueStore.enqueue(concertId, userId, score);
        this.queueStore.saveQueueToken(queueToken, concertId, userId, 300);

        return queueToken;
    }

    public QueueStatusResult status(Long concertId, QueueToken token) {

        //queueToken 값을 기준으로 유저ID를 찾는다.
        TokenMapping mapping = this.queueStore.findByQueueToken(token)
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 토큰입니다."));

        long userId = mapping.userId();
        // 유저의 순번을 구한다.
        Long rank = this.queueStore.findRank(concertId, userId)
                .orElseThrow(() -> new IllegalStateException("대기열에 등록된 정보가 없습니다."));

        // gate가 열려있는지 여부 확인
        long gate = this.queueStore.getGate(concertId);

        // gate가 열려있지 않다면 Waiting 전달
        if (gate < 0) {
            return new QueueStatusResult.Waiting(rank + 1, 1000);
        }

        // 입장 가능 여부 판단.
        if (rank <= gate) {
            String accessToken = UUID.randomUUID().toString();
            queueStore.saveAccessToken(accessToken, concertId, userId, 120);

            return new QueueStatusResult.Admitted(accessToken, 120);
        }

        return new QueueStatusResult.Waiting(rank + 1, 1000);
    }
}
