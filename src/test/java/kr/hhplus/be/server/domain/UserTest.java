package kr.hhplus.be.server.domain;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("Green: 포인트를 충전한다")
    void chargePoint_success() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when
        user.chargePoint(500L);

        // then
        assertThat(user.getBalance()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Green: 여러 번 포인트를 충전한다")
    void chargePoint_multiple() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(0L)
                .password("password")
                .build();

        // when
        user.chargePoint(1000L);
        user.chargePoint(2000L);
        user.chargePoint(500L);

        // then
        assertThat(user.getBalance()).isEqualTo(3500L);
    }

    @Test
    @DisplayName("Red: 0 이하의 금액 충전 시 예외가 발생한다")
    void chargePoint_zeroOrNegative() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when & then
        assertThatThrownBy(() -> user.chargePoint(0L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
                });

        assertThatThrownBy(() -> user.chargePoint(-500L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
                });
    }

    @Test
    @DisplayName("Green: 포인트를 사용한다")
    void usePoint_success() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when
        user.usePoint(300L);

        // then
        assertThat(user.getBalance()).isEqualTo(700L);
    }

    @Test
    @DisplayName("Green: 전액 포인트를 사용한다")
    void usePoint_fullBalance() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when
        user.usePoint(1000L);

        // then
        assertThat(user.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Red: 잔액보다 많은 포인트 사용 시 예외가 발생한다")
    void usePoint_insufficientBalance() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when & then
        assertThatThrownBy(() -> user.usePoint(1500L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                });
    }

    @Test
    @DisplayName("Red: 0 이하의 금액 사용 시 예외가 발생한다")
    void usePoint_zeroOrNegative() {
        // given
        User user = User.builder()
                .userId(1L)
                .balance(1000L)
                .password("password")
                .build();

        // when & then
        assertThatThrownBy(() -> user.usePoint(0L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_USE_AMOUNT);
                });

        assertThatThrownBy(() -> user.usePoint(-100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_USE_AMOUNT);
                });
    }
}
