package kr.hhplus.be.server.reservation.infrastructure.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationRequest {

    private Long userId;
    private Long scheduleId;
    private Long seatId;
}
