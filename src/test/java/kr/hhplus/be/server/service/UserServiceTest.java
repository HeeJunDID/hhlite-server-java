package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Green: 유저 ID로 유저를 조회한다")
    void getUser_success() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .userId(userId)
                .balance(1000L)
                .password("password")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.getUser(userId);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저 ID로 조회하면 예외가 발생한다")
    void getUser_notFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Green: 포인트를 충전한다")
    void chargePoint_success() {
        // given
        Long userId = 1L;
        long initialBalance = 1000L;
        long chargeAmount = 500L;

        User user = User.builder()
                .userId(userId)
                .balance(initialBalance)
                .password("password")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result.getBalance()).isEqualTo(initialBalance + chargeAmount);
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저에게 포인트 충전 시 예외가 발생한다")
    void chargePoint_userNotFound() {
        // given
        Long userId = 999L;
        long chargeAmount = 500L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(userId, chargeAmount))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Red: 0 이하의 금액 충전 시 예외가 발생한다")
    void chargePoint_invalidAmount() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .userId(userId)
                .balance(1000L)
                .password("password")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.chargePoint(userId, 0L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT);
                });
    }

    @Test
    @DisplayName("Green: 유저 잔액을 조회한다")
    void getBalance_success() {
        // given
        Long userId = 1L;
        long balance = 5000L;

        User user = User.builder()
                .userId(userId)
                .balance(balance)
                .password("password")
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        long result = userService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(balance);
    }
}
