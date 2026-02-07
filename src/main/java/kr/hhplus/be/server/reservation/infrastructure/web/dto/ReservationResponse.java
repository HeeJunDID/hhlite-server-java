package kr.hhplus.be.server.reservation.infrastructure.web.dto;

import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponse {

    private Long reservationId;
    private Long userId;
    private Long seatId;
    private int seatNum;
    private long amount;
    private String status;
    private LocalDateTime expiredAt;

    public static ReservationResponse from(ReservationResult result) {
        return ReservationResponse.builder()
                .reservationId(result.reservationId())
                .userId(result.userId())
                .seatId(result.seatId())
                .seatNum(result.seatNum())
                .amount(result.amount())
                .status(result.status().name())
                .expiredAt(result.expiredAt())
                .build();
    }
}
