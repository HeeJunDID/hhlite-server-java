package kr.hhplus.be.server.queue.application.port.out;

import kr.hhplus.be.server.queue.domain.QueueToken;

import java.util.Optional;

public interface QueueStore {
    /**
     * 대기열에 사용자 등록(ZADD)
     * @param concertId
     * @param userId
     * @param score
     */
    void enqueue(Long concertId, Long userId, double score);

    /**
     * 내 순번 찾기 (ZRANK)
     * @param concertId
     * @param userId
     * @return
     */
    Optional<Long> findRank(Long concertId, Long userId);

    /**
     * gate 값 조회 (몇명이 통과 가능한지 여부 조회) 없으면 -1 반
     */
    long getGate(Long concertId);

    /**
     * queueToken 저장
     */
    void saveQueueToken(QueueToken queueToken, Long concertId, Long userId, long ttlSeconds);

    /**
     * 사용자 조회(토큰값 기준으로 찾기)
     */
    Optional<TokenMapping> findByQueueToken(QueueToken queueToken);

    /**
     * accessToken 저장
     */
    void saveAccessToken(String accessToken, Long concertId, Long userId, long ttlSeconds);

    /**
     * accessToken 기준 사용자 조회
     */
    Optional<TokenMapping> findByAccessToken(String accessToken);

    /**
     * 토큰이 가리키는 정보 조회
     */
    record TokenMapping(Long concertId, Long userId) {

    }


}
