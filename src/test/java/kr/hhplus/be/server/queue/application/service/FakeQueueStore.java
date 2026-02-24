package kr.hhplus.be.server.queue.application.service;

import kr.hhplus.be.server.queue.application.port.out.QueueStore;
import kr.hhplus.be.server.queue.domain.QueueToken;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class FakeQueueStore implements QueueStore {

    /**
     * 대기열 구현(concert 별로 줄을 세움)
     */
    private final Map<Long, List<Entry>> queues = new ConcurrentHashMap<>();

    /**
     * queueToken 저장소 구현
     */
    private final Map<QueueToken, TokenMapping> queueTokenStore = new ConcurrentHashMap<>();

    /**
     * accessToken 저장소 구현
     */
    private final Map<String, TokenMapping> accessTokenStore = new ConcurrentHashMap<>();

    /**
     * gate 구현
     */
    private final Map<Long, Long> gates = new ConcurrentHashMap<>();

    @Override
    public void enqueue(Long concertId, Long userId, double score) {
        // queue에 존재하면 해당 콘서트ID 기준으로 Entry(userId,score) 리스트에 추가 없다면 concertId 기준 리스트 생성 후 추가
        queues.computeIfAbsent(concertId, id -> new ArrayList<>())
                .add(new Entry(userId, score));
        // score 기준으로 정렬
        queues.get(concertId).sort(Comparator.comparingDouble(e -> e.score));
    }

    @Override
    public Optional<Long> findRank(Long concertId, Long userId) {
        List<Entry> queue = queues.get(concertId);
        if (queue == null) {
            return Optional.empty();
        }
        for (int i = 0; i < queue.size(); i++) {
            if (Objects.equals(queue.get(i).userId, userId)) {
                return Optional.of((long) i);
            }
        }
        return Optional.empty();
    }
    @Override
    public long getGate(Long concertId) {
        return gates.getOrDefault(concertId, -1L);
    }

    // 테스트에서 gate 설정하려고 추가 (Fake 전용)
    public void setGate(Long concertId, long gate) {
        gates.put(concertId, gate);
    }


    @Override
    public void saveQueueToken(QueueToken queueToken, Long concertId, Long userId, long ttlSeconds) {
        queueTokenStore.put(queueToken, new TokenMapping(concertId, userId));
    }

    @Override
    public Optional<TokenMapping> findByQueueToken(QueueToken queueToken) {
        return Optional.ofNullable(queueTokenStore.get(queueToken));
    }

    @Override
    public void saveAccessToken(String accessToken, Long concertId, Long userId, long ttlSeconds) {
        accessTokenStore.put(accessToken, new TokenMapping(concertId, userId));
    }

    @Override
    public Optional<TokenMapping> findByAccessToken(String accessToken) {
        return Optional.ofNullable(accessTokenStore.get(accessToken));
    }

    private static class Entry{
        final Long userId;
        final double score;

        Entry(Long userId, double score) {
            this.userId = userId;
            this.score = score;
        }

    }
}