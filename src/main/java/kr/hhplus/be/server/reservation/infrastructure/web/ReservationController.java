package kr.hhplus.be.server.reservation.infrastructure.web;

import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.infrastructure.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.infrastructure.web.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReserveSeatUseCase reserveSeatUseCase;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(@RequestBody ReservationRequest request) {
        ReserveSeatCommand command = new ReserveSeatCommand(
                request.getUserId(),
                request.getScheduleId(),
                request.getSeatId()
        );

        ReservationResult result = reserveSeatUseCase.execute(command);

        return ResponseEntity.ok(ReservationResponse.from(result));
    }
}
