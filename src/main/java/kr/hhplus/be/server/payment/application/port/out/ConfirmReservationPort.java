package kr.hhplus.be.server.payment.application.port.out;

public interface ConfirmReservationPort {
    void confirm(Long reservationId, Long paymentId);
}
