package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String password;

    private long balance;

    public void chargePoint(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }
        this.balance += amount;
    }

    public void usePoint(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_USE_AMOUNT);
        }
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                    "잔액이 부족합니다. 현재 잔액: " + this.balance);
        }
        this.balance -= amount;
    }
}
