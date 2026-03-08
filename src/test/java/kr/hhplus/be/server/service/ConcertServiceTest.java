package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.Concert;
import kr.hhplus.be.server.repository.ConcertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepository;

    @InjectMocks
    private ConcertService concertService;

    @Test
    @DisplayName("Green : 콘서트 ID로 콘서트를 조회한다")
    void getConcert_success() {
        // given
        Long concertId = 1L;
        Concert concert = Concert.builder()
                .concertId(concertId)
                .title("테스트 콘서트")
                .description("테스트 설명")
                .build();

        given(concertRepository.findById(concertId)).willReturn(Optional.of(concert));

        // when
        Concert result = concertService.getConcert(concertId);

        // then
        assertThat(result.getConcertId()).isEqualTo(concertId);
        assertThat(result.getTitle()).isEqualTo("테스트 콘서트");
        assertThat(result.getDescription()).isEqualTo("테스트 설명");
    }

    @Test
    @DisplayName("Red: 존재하지 않는 콘서트 ID로 조회하면 예외가 발생한다")
    void getConcert_notFound() {
        // given
        Long concertId = 999L;
        given(concertRepository.findById(concertId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> concertService.getConcert(concertId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONCERT_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("전체 콘서트 목록을 조회한다")
    void getAllConcerts_success() {
        // given
        List<Concert> concerts = List.of(
                Concert.builder().concertId(1L).title("콘서트1").description("설명1").build(),
                Concert.builder().concertId(2L).title("콘서트2").description("설명2").build()
        );

        given(concertRepository.findAll()).willReturn(concerts);

        // when
        List<Concert> result = concertService.getAllConcerts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("콘서트1");
        assertThat(result.get(1).getTitle()).isEqualTo("콘서트2");
    }

    @Test
    @DisplayName("Green: 콘서트가 없으면 빈 목록을 반환한다")
    void getAllConcerts_empty() {
        // given
        given(concertRepository.findAll()).willReturn(List.of());

        // when
        List<Concert> result = concertService.getAllConcerts();

        // then
        assertThat(result).isEmpty();
    }
}
