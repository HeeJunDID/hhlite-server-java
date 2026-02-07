package kr.hhplus.be.server.controller.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChargePointRequest {

    private long amount;

    public ChargePointRequest(long amount) {
        this.amount = amount;
    }
}
