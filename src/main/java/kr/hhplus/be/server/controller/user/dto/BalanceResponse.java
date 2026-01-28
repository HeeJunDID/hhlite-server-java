package kr.hhplus.be.server.controller.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceResponse {

    private Long userId;
    private long balance;

    public static BalanceResponse of(Long userId, long balance) {
        return BalanceResponse.builder()
                .userId(userId)
                .balance(balance)
                .build();
    }
}
