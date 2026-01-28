package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "유저를 찾을 수 없습니다. id: " + userId));
    }

    @Transactional
    public User chargePoint(Long userId, long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "유저를 찾을 수 없습니다. id: " + userId));

        user.chargePoint(amount);

        return user;
    }

    @Transactional(readOnly = true)
    public long getBalance(Long userId) {
        User user = getUser(userId);
        return user.getBalance();
    }
}
