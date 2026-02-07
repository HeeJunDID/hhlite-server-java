package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.payment.application.port.out.DeductUserBalancePort;
import kr.hhplus.be.server.payment.application.port.out.LoadUserPort;
import kr.hhplus.be.server.payment.application.port.out.UserInfo;
import kr.hhplus.be.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPortAdapter implements LoadUserPort, DeductUserBalancePort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserInfo> loadById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserInfo(user.getUserId(), user.getBalance()));
    }

    @Override
    public void deductBalance(Long userId, long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.usePoint(amount);
    }
}
