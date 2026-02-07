package kr.hhplus.be.server.payment.application.port.out;

public interface DeductUserBalancePort {
    void deductBalance(Long userId, long amount);
}
