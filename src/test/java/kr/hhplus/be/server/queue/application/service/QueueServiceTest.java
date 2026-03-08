package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.queue.application.port.out.QueueStore;
import kr.hhplus.be.server.queue.domain.QueueStatusResult;
import kr.hhplus.be.server.queue.domain.QueueToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class QueueServiceTest {
    private QueueService queueService;
    private FakeQueueStore fakeQueueStore;

    @BeforeEach
    void setup() {
        fakeQueueStore = new FakeQueueStore();
        queueService = new QueueService(fakeQueueStore);
    }

    @Test
    @DisplayName("RED: gate가 열리지 않았을때 status는 WAITING으로")
    void enter_queue_gate_is_not_opened_status_is_WAITING() {
        //given
        Long concertId = 1L;
        Long userId = 100L;
        Long gate = -1L;

        QueueToken queueToken = queueService.enter(concertId, userId);
        //when
        QueueStatusResult queueStatusResult = queueService.status(concertId, queueToken);

        //then
        assertThat(queueStatusResult).isInstanceOf(QueueStatusResult.Waiting.class);
    }

    @Test
    @DisplayName("Green: queueToken 정상 발급 및 대기열에 정상 등록")
    void enter_queue_enroll_waitingList_success() {
        //given
        Long concertId = 1L;
        Long userId = 100L;

        //when
        QueueToken queueToken = queueService.enter(concertId, userId);

        //then
        assertThat(queueToken).isNotNull();
        assertThat(fakeQueueStore.findRank(concertId, userId)).hasValue(0L);
    }

    @Test
    @DisplayName("Green: gate 입장")
    void enter_gate() {
        //given
        Long concertId = 1L;
        Long userId = 100L;
        fakeQueueStore.setGate(concertId, 0);
        QueueToken queueToken = queueService.enter(concertId, userId);

        //when
        QueueStatusResult queueStatusResult = queueService.status(concertId, queueToken);

        //then
        assertThat(queueStatusResult).isInstanceOf(QueueStatusResult.Admitted.class);
    }

    @Test
    @DisplayName("Green : AccessToken 검증")
    void check_accessToken() {
        //given
        Long concertId = 1L;
        Long userId = 100L;
        fakeQueueStore.setGate(concertId, 0L);

        //when
        QueueToken queueToken = queueService.enter(concertId, userId);
        QueueStatusResult result = queueService.status(concertId, queueToken);

        //then
        assertThat(result).isInstanceOf(QueueStatusResult.Admitted.class);
        QueueStatusResult.Admitted admitted = (QueueStatusResult.Admitted) result;
        String accessToken = admitted.accessToken();

        assertThat(accessToken).isNotBlank();

        assertThat(fakeQueueStore.findByAccessToken(accessToken))
                .isPresent()
                .get()
                .satisfies(tokenMapping -> {
                    assertThat(tokenMapping.concertId()).isEqualTo(concertId);
                    assertThat(tokenMapping.userId()).isEqualTo(userId);
                });

    }
}