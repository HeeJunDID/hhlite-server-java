package kr.hhplus.be.server.reservation.application.port.out;

public record SeatInfo(
        Long seatId,
        Long scheduleId,
        int seatNum,
        long price,
        String status,
        String grade
) {
    private static final String STATUS_AVAILABLE = "AVAILABLE";

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }
}
