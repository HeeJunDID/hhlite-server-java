package kr.hhplus.be.server.service;

import kr.hhplus.be.server.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private ReservationRepository reservationRepository;

}