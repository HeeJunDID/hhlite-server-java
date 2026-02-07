package kr.hhplus.be.server.payment.application.port.out;

public record UserInfo(
        Long userId,
        long balance
) {
}
