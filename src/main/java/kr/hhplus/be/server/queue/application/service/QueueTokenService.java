package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.queue.application.port.in.*;
import kr.hhplus.be.server.queue.application.port.out.QueueTokenPort;
import kr.hhplus.be.server.queue.domain.QueueTokenEntity;
import kr.hhplus.be.server.queue.domain.TokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueTokenService implements IssueTokenUseCase, ValidateTokenUseCase, ExpireTokenUseCase {

    private static final int TOKEN_TTL_MINUTES = 10;

    private final QueueTokenPort queueTokenPort;

    @Override
    public TokenResult issue(IssueTokenCommand command) {
        // 이미 유효한 토큰이 있는지 확인
        List<TokenStatus> validStatuses = List.of(TokenStatus.WAITING, TokenStatus.ACTIVE);
        var existingToken = queueTokenPort.findByUserIdAndConcertIdAndStatusIn(
                command.userId(), command.concertId(), validStatuses);

        if (existingToken.isPresent()) {
            QueueTokenEntity token = existingToken.get();
            if (!token.isExpired()) {
                return toResult(token, calculatePosition(token));
            }
        }

        // 새 토큰 발급
        QueueTokenEntity newToken = QueueTokenEntity.create(
                command.userId(),
                command.concertId(),
                TOKEN_TTL_MINUTES
        );

        QueueTokenEntity savedToken = queueTokenPort.save(newToken);
        Long position = calculatePosition(savedToken);

        return toResult(savedToken, position);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResult validate(String token) {
        QueueTokenEntity tokenEntity = queueTokenPort.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 토큰입니다."));

        if (tokenEntity.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "만료된 토큰입니다.");
        }

        Long position = calculatePosition(tokenEntity);
        return toResult(tokenEntity, position);
    }

    @Override
    public void expireToken(String token) {
        QueueTokenEntity tokenEntity = queueTokenPort.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 토큰입니다."));
        tokenEntity.expire();
        queueTokenPort.save(tokenEntity);
    }

    @Override
    public int expireAllExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<QueueTokenEntity> expiredTokens = queueTokenPort.findByStatusAndExpiredAtBefore(
                TokenStatus.WAITING, now);

        expiredTokens.addAll(queueTokenPort.findByStatusAndExpiredAtBefore(TokenStatus.ACTIVE, now));

        for (QueueTokenEntity token : expiredTokens) {
            token.expire();
        }

        queueTokenPort.saveAll(expiredTokens);
        return expiredTokens.size();
    }

    private Long calculatePosition(QueueTokenEntity token) {
        if (token.getStatus() == TokenStatus.ACTIVE) {
            return 0L;
        }
        return queueTokenPort.countByStatusAndCreatedAtBefore(
                TokenStatus.WAITING, token.getCreatedAt()) + 1;
    }

    private TokenResult toResult(QueueTokenEntity token, Long position) {
        return new TokenResult(
                token.getTokenId(),
                token.getToken(),
                token.getUserId(),
                token.getConcertId(),
                token.getStatus(),
                position,
                token.getExpiredAt()
        );
    }
}
