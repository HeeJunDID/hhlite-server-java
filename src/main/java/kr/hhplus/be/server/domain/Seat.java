package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seat {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_SOLD = "SOLD";

    @Id
    @Column(name = "seat_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "seat_num")
    private int seatNum;

    private long price;

    private String status;

    private String grade;

    public void markAsSold() {
        this.status = STATUS_SOLD;
    }

    public void release() {
        this.status = STATUS_AVAILABLE;
    }
}
