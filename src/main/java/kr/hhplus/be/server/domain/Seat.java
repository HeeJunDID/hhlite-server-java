package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seat {

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
}
