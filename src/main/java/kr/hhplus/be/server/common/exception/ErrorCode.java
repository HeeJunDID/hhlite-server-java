package kr.hhplus.be.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저를 찾을 수 없습니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "U002", "충전 금액은 0보다 커야 합니다."),
    INVALID_USE_AMOUNT(HttpStatus.BAD_REQUEST, "U003", "사용 금액은 0보다 커야 합니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "U004", "잔액이 부족합니다."),

    // Concert
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "콘서트를 찾을 수 없습니다."),
    CONCERT_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "CO002", "콘서트 일정을 찾을 수 없습니다."),

    // Seat
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "좌석을 찾을 수 없습니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "S002", "이미 예약된 좌석입니다."),

    // Reservation
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "예약을 찾을 수 없습니다."),
    RESERVATION_ALREADY_PAID(HttpStatus.CONFLICT, "R002", "이미 결제된 예약입니다."),
    RESERVATION_EXPIRED(HttpStatus.BAD_REQUEST, "R003", "예약이 만료되었습니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "P002", "결제에 실패했습니다."),

    // Queue
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "Q001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Q002", "만료된 토큰입니다."),
    TOKEN_NOT_ACTIVE(HttpStatus.FORBIDDEN, "Q003", "아직 입장할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
