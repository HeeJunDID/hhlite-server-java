package kr.hhplus.be.server.controller.user.dto;

import kr.hhplus.be.server.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private long balance;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .balance(user.getBalance())
                .build();
    }
}
