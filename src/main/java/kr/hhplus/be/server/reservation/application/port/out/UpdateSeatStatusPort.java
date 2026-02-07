package kr.hhplus.be.server.reservation.application.port.out;

public interface UpdateSeatStatusPort {
    void markAsReserved(Long seatId);
    void release(Long seatId);
}
