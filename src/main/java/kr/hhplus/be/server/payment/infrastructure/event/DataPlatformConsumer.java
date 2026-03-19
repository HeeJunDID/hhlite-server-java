package kr.hhplus.be.server.payment.infrastructure.event;

import kr.hhplus.be.server.config.KafkaTopicConfig;
import kr.hhplus.be.server.payment.application.event.ReservationConfirmedEvent;
import kr.hhplus.be.server.payment.infrastructure.external.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformConsumer {

    private final DataPlatformClient dataPlatformClient;

    @KafkaListener(topics = KafkaTopicConfig.RESERVATION_CONFIRMED_TOPIC,
            groupId = "data-platform-consumer")
    public void consume(ReservationConfirmedEvent event) {
        try {
            dataPlatformClient.send(event);
        } catch (Exception e) {
            log.error("[DataPlatform] 예약 정보 전송 실패 - reservationId={}, 원인: {}",
                    event.reservationId(), e.getMessage());
        }
    }
}
