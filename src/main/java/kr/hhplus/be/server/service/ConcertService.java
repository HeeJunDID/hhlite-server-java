package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.Concert;
import kr.hhplus.be.server.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 단건 콘서트 조회.
     * 콘서트 정보는 자주 변경되지 않으므로 1시간 TTL로 캐싱한다.
     */
    @Cacheable(value = "concert", key = "#concertId")
    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCERT_NOT_FOUND,
                        "콘서트를 찾을 수 없습니다. id: " + concertId));
    }

    /**
     * 전체 콘서트 목록 조회.
     * 콘서트 목록은 자주 변경되지 않으므로 1시간 TTL로 캐싱한다.
     */
    @Cacheable("concerts")
    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }
}
