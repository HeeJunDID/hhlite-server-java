package kr.hhplus.be.server.payment.infrastructure.event;

import kr.hhplus.be.server.payment.application.event.ReservationConfirmedEvent;
import kr.hhplus.be.server.payment.infrastructure.external.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventListener {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 결제 트랜잭션 커밋 완료 후 비동기로 데이터 플랫폼에 예약 정보를 전송한다.
     * - AFTER_COMMIT: DB 커밋이 성공한 경우에만 실행되어 불필요한 외부 전송을 방지한다.
     * - @Async: 외부 API 호출이 메인 응답을 지연시키지 않도록 별도 스레드에서 처리한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        try {
            dataPlatformClient.send(event);
        } catch (Exception e) {
            log.error("[DataPlatform] 예약 정보 전송 실패 - reservationId={}, 원인: {}",
                    event.reservationId(), e.getMessage());
        }
    }
}
