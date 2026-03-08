package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = userRepository.save(User.builder()
                .password("test123")
                .balance(50_000L)
                .build());
    }

    @Test
    @DisplayName("포인트 충전 후 잔액이 정상적으로 증가한다")
    void 포인트_충전_성공() {
        // when
        userService.chargePoint(testUser.getUserId(), 30_000L);

        // then
        long balance = userService.getBalance(testUser.getUserId());
        assertThat(balance).isEqualTo(80_000L);
    }

    @Test
    @DisplayName("잔액 조회가 정상적으로 동작한다")
    void 잔액_조회_성공() {
        // when
        long balance = userService.getBalance(testUser.getUserId());

        // then
        assertThat(balance).isEqualTo(50_000L);
    }

    @Test
    @DisplayName("존재하지 않는 유저 조회 시 USER_NOT_FOUND 예외가 발생한다")
    void 존재하지않는_유저_조회_예외() {
        // when & then
        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("0 이하의 금액 충전 시 INVALID_CHARGE_AMOUNT 예외가 발생한다")
    void 잘못된_금액_충전_예외() {
        // when & then
        assertThatThrownBy(() -> userService.chargePoint(testUser.getUserId(), 0L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CHARGE_AMOUNT);
    }

    @Test
    @DisplayName("포인트 충전 후 DB에 반영된 잔액을 재조회해도 동일하다")
    void 포인트_충전_DB_반영_확인() {
        // when
        userService.chargePoint(testUser.getUserId(), 20_000L);

        // then: DB에서 직접 조회
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualTo(70_000L);
    }
}
