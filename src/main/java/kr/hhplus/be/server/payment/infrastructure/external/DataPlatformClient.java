package kr.hhplus.be.server.payment.infrastructure.external;

import kr.hhplus.be.server.payment.application.event.ReservationConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataPlatformClient {

    public void send(ReservationConfirmedEvent event) {
        // Mock: 실제 환경에서는 외부 데이터 플랫폼 HTTP API 호출
        log.info("[DataPlatform] 예약 정보 전송 - paymentId={}, reservationId={}, userId={}, concertId={}, amount={}, paidAt={}",
                event.paymentId(),
                event.reservationId(),
                event.userId(),
                event.concertId(),
                event.amount(),
                event.paidAt()
        );
    }
}
