package kr.hhplus.be.server.reservation.application.port.in;

public interface ReserveSeatUseCase {
    ReservationResult execute(ReserveSeatCommand command);
}
