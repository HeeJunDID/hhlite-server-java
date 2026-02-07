package kr.hhplus.be.server.payment.application.port.out;

import java.util.Optional;

public interface LoadUserPort {
    Optional<UserInfo> loadById(Long userId);
}
