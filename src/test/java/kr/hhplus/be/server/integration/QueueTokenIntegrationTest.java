package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.queue.application.port.in.IssueTokenCommand;
import kr.hhplus.be.server.queue.application.port.in.IssueTokenUseCase;
import kr.hhplus.be.server.queue.application.port.in.TokenResult;
import kr.hhplus.be.server.queue.application.port.in.ValidateTokenUseCase;
import kr.hhplus.be.server.queue.domain.TokenStatus;
import kr.hhplus.be.server.queue.infrastructure.persistence.QueueTokenJpaRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueTokenIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTokenUseCase issueTokenUseCase;

    @Autowired
    private ValidateTokenUseCase validateTokenUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueTokenJpaRepository queueTokenJpaRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        queueTokenJpaRepository.deleteAll();
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .password("test123")
                .balance(100_000L)
                .build());
    }

    @Test
    @DisplayName("큐 토큰 발급 시 WAITING 상태의 토큰이 발급된다")
    void 큐토큰_발급_성공() {
        // when
        TokenResult result = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.token()).isNotNull();
        assertThat(result.userId()).isEqualTo(testUser.getUserId());
        assertThat(result.concertId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(TokenStatus.WAITING);
        assertThat(result.expiredAt()).isNotNull();
        assertThat(result.position()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동일한 유저가 동일한 콘서트로 토큰을 재요청하면 기존 토큰을 반환한다")
    void 기존_유효토큰_재사용() {
        // given: 첫 번째 토큰 발급
        TokenResult firstToken = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );

        // when: 동일 조건으로 재요청
        TokenResult secondToken = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );

        // then: 동일한 토큰 반환
        assertThat(secondToken.token()).isEqualTo(firstToken.token());
        assertThat(secondToken.tokenId()).isEqualTo(firstToken.tokenId());
    }

    @Test
    @DisplayName("발급된 토큰을 검증하면 동일한 정보가 반환된다")
    void 토큰_검증_성공() {
        // given
        TokenResult issued = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );

        // when
        TokenResult validated = validateTokenUseCase.validate(issued.token());

        // then
        assertThat(validated.token()).isEqualTo(issued.token());
        assertThat(validated.userId()).isEqualTo(testUser.getUserId());
        assertThat(validated.concertId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 토큰 검증 시 예외가 발생한다")
    void 존재하지않는_토큰_검증_예외() {
        // when & then
        assertThatThrownBy(() -> validateTokenUseCase.validate("non-existent-token-value"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("여러 유저가 순서대로 토큰 발급 시 위치(position)가 순서대로 부여된다")
    void 여러_유저_토큰_발급_순서() {
        // given: 두 번째 유저
        User secondUser = userRepository.save(User.builder()
                .password("second123")
                .balance(100_000L)
                .build());

        // when
        TokenResult firstToken = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );
        TokenResult secondToken = issueTokenUseCase.issue(
                new IssueTokenCommand(secondUser.getUserId(), 1L)
        );

        // then: 첫 번째가 1번, 두 번째가 2번
        assertThat(firstToken.position()).isEqualTo(1L);
        assertThat(secondToken.position()).isEqualTo(2L);
    }

    @Test
    @DisplayName("서로 다른 콘서트에 토큰을 발급하면 각각 독립적인 토큰이 발급된다")
    void 다른_콘서트_토큰_독립_발급() {
        // when
        TokenResult concertAToken = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 1L)
        );
        TokenResult concertBToken = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), 2L)
        );

        // then: 서로 다른 토큰
        assertThat(concertAToken.token()).isNotEqualTo(concertBToken.token());
        assertThat(concertAToken.concertId()).isEqualTo(1L);
        assertThat(concertBToken.concertId()).isEqualTo(2L);
    }
}
