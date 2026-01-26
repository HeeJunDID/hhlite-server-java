package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Concert;
import kr.hhplus.be.server.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "콘서트를 찾을 수 없습니다. id: " + concertId));
    }

    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }


}
