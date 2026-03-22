package kr.hhplus.be.server.payment.infrastructure.event;

import kr.hhplus.be.server.config.KafkaTopicConfig;
import kr.hhplus.be.server.payment.application.event.ReservationConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventListener {

    private final KafkaTemplate<String, ReservationConfirmedEvent> kafkaTemplate;

    /**
     * 결제 트랜잭션 커밋 완료 후 Kafka 토픽으로 예약 확정 이벤트를 발행한다.
     * - AFTER_COMMIT: DB 커밋이 성공한 경우에만 실행되어 불필요한 메시지 발행을 방지한다.
     * - @Async: Kafka 발행이 메인 응답을 지연시키지 않도록 별도 스레드에서 처리한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.RESERVATION_CONFIRMED_TOPIC,
                    String.valueOf(event.reservationId()), event);
            log.info("[Kafka] 예약 확정 이벤트 발행 - reservationId={}, topic={}",
                    event.reservationId(), KafkaTopicConfig.RESERVATION_CONFIRMED_TOPIC);
        } catch (Exception e) {
            log.error("[Kafka] 예약 확정 이벤트 발행 실패 - reservationId={}, 원인: {}",
                    event.reservationId(), e.getMessage());
        }
    }
}
