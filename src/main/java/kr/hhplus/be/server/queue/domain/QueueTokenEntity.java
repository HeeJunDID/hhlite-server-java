package kr.hhplus.be.server.queue.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QueueTokenEntity {

    @Id
    @Column(name = "token_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "concert_id")
    private Long concertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public static QueueTokenEntity create(Long userId, Long concertId, int ttlMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return QueueTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .concertId(concertId)
                .status(TokenStatus.WAITING)
                .createdAt(now)
                .expiredAt(now.plusMinutes(ttlMinutes))
                .build();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt) || status == TokenStatus.EXPIRED;
    }

    public boolean isActive() {
        return status == TokenStatus.ACTIVE && !isExpired();
    }

    public void activate() {
        if (!isExpired()) {
            this.status = TokenStatus.ACTIVE;
        }
    }

    public void expire() {
        this.status = TokenStatus.EXPIRED;
    }
}
