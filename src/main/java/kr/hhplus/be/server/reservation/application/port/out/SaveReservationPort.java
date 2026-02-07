package kr.hhplus.be.server.reservation.application.port.out;

import kr.hhplus.be.server.reservation.domain.Reservation;

public interface SaveReservationPort {
    Reservation save(Reservation reservation);
}
