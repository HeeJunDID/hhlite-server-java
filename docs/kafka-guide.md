# Kafka 기초 가이드

> Kafka를 처음 접하는 팀원들을 위해 작성한 입문 문서입니다.

---

## 1. Kafka란 무엇인가?

Apache Kafka는 **분산 이벤트 스트리밍 플랫폼**입니다.
쉽게 말하면 **시스템 간에 메시지를 빠르고 안정적으로 주고받기 위한 중간 다리** 역할을 합니다.

LinkedIn에서 처음 개발해 Apache 재단에 기부된 오픈소스로, 현재 대부분의 대형 서비스에서 사용됩니다.

---

## 2. 왜 Kafka가 필요한가? — 이벤트 기반 시스템 관점

### 기존 방식의 문제

```
결제 서비스 → DataPlatform HTTP 호출
```

- 결제 서비스가 DataPlatform의 장애에 직접 영향을 받음
- 결제 응답 속도가 DataPlatform 응답 속도에 종속됨
- 시스템이 늘어날수록 직접 연결(Point-to-Point)이 폭발적으로 증가

### Kafka 도입 후

```
결제 서비스 → Kafka Topic → DataPlatform Consumer
             ↘ Analytics Consumer
             ↘ Notification Consumer
```

- 결제 서비스는 메시지만 발행하고 끝 (발행자는 소비자를 알 필요 없음)
- 소비자가 장애가 나도 결제 서비스에 영향 없음
- 새로운 소비자 추가 시 결제 서비스 코드 변경 불필요

---

## 3. 핵심 개념

### Producer (프로듀서)
- 메시지를 **발행**하는 쪽
- Topic에 메시지를 씀
- 우리 서비스에서는 `ReservationEventListener`가 Producer 역할

### Consumer (컨슈머)
- 메시지를 **소비**하는 쪽
- Topic에서 메시지를 읽음
- 우리 서비스에서는 `DataPlatformConsumer`가 Consumer 역할

### Topic (토픽)
- 메시지가 저장되는 **카테고리/채널**
- 이메일 수신함처럼, 특정 주제의 메시지들이 모이는 공간
- 예: `reservation-confirmed`, `payment-failed`, `user-registered`

### Partition (파티션)
- Topic을 물리적으로 나눈 단위
- 파티션이 많을수록 **병렬 처리** 가능 → 처리량 증가
- 같은 Key를 가진 메시지는 항상 같은 파티션으로 → **순서 보장**

```
Topic: reservation-confirmed
├── Partition 0: [msg1, msg4, msg7, ...]
├── Partition 1: [msg2, msg5, msg8, ...]
└── Partition 2: [msg3, msg6, msg9, ...]
```

### Broker (브로커)
- Kafka 서버 한 대
- 실제로 메시지를 저장하고 전달하는 역할
- 여러 Broker가 모여 **Kafka Cluster** 구성

### Consumer Group (컨슈머 그룹)
- 같은 그룹 ID를 가진 Consumer들의 묶음
- **하나의 파티션은 같은 그룹 내 하나의 Consumer만 처리**
- 그룹이 다르면 동일한 메시지를 각자 독립적으로 읽음

```
Topic (3 Partitions)
├── Partition 0 → Consumer A (group: data-platform)
├── Partition 1 → Consumer B (group: data-platform)
└── Partition 2 → Consumer C (group: data-platform)

→ 같은 그룹이라도 파티션 수만큼 병렬 처리 가능
```

### Offset (오프셋)
- 파티션 내 메시지의 **위치(인덱스)**
- Consumer는 어디까지 읽었는지(offset)를 Kafka에 저장(commit)
- 장애 후 재시작해도 마지막으로 읽은 위치부터 이어서 처리 가능

### Zookeeper
- Kafka 클러스터의 **메타데이터 관리** (브로커 목록, 리더 선출 등)
- Kafka 3.x부터 KRaft 모드로 Zookeeper 없이도 동작 가능
- 우리 실습 환경에서는 Zookeeper를 함께 사용

---

## 4. Kafka의 주요 특징

### 높은 처리량 (High Throughput)
- 디스크 순차 쓰기 방식으로 매우 빠름
- 초당 수백만 건 메시지 처리 가능

### 내구성 (Durability)
- 메시지를 디스크에 저장 (기본 7일)
- Consumer가 처리 실패해도 메시지 재소비 가능
- Replication으로 브로커 장애 시에도 데이터 유실 없음

### 확장성 (Scalability)
- Broker, Partition, Consumer를 독립적으로 수평 확장 가능

### 순서 보장
- **같은 파티션 내에서는** 순서가 보장됨
- Key를 기준으로 같은 파티션에 메시지를 보내면 순서 보장 가능

---

## 5. 장단점

### 장점

| 항목 | 설명 |
|------|------|
| 서비스 간 결합도 감소 | Producer와 Consumer가 서로를 모름 |
| 장애 격리 | 소비자 장애가 발행자에 영향 없음 |
| 재처리 가능 | 오프셋을 되돌려 메시지 재소비 가능 |
| 높은 처리량 | 초당 수백만 건 처리 |
| 확장 용이 | 파티션/브로커 추가로 선형 확장 |
| 다중 소비자 | 같은 메시지를 여러 서비스가 독립 소비 |

### 단점

| 항목 | 설명 |
|------|------|
| 운영 복잡도 | Zookeeper, 브로커 모니터링 필요 |
| 학습 곡선 | 파티션, 오프셋, 컨슈머 그룹 개념 이해 필요 |
| 메시지 순서 | 파티션 간 순서는 보장되지 않음 |
| 작은 메시지 비효율 | 매우 작은 메시지가 많을 경우 오버헤드 |
| 즉각적 일관성 X | 메시지 발행 후 처리까지 시간 차 발생 |

---

## 6. 이 프로젝트에서의 적용

### 변경 전 흐름

```
결제 완료
    → Spring ApplicationEvent 발행
        → ReservationEventListener (AFTER_COMMIT, @Async)
            → DataPlatformClient.send() 직접 호출
```

### 변경 후 흐름

```
결제 완료
    → Spring ApplicationEvent 발행
        → ReservationEventListener (AFTER_COMMIT, @Async)
            → KafkaTemplate.send("reservation-confirmed", event)  ← Producer
                → Kafka Broker (Topic: reservation-confirmed)
                    → DataPlatformConsumer.consume()              ← Consumer
                        → DataPlatformClient.send()
```

### 핵심 파일

| 파일 | 역할 |
|------|------|
| `KafkaTopicConfig.java` | 토픽 정의 (reservation-confirmed, 파티션 3) |
| `ReservationEventListener.java` | Spring 이벤트 → Kafka 발행 |
| `DataPlatformConsumer.java` | Kafka 메시지 소비 → DataPlatform 전송 |
| `application.yml` | Kafka 브로커 연결 설정 |

---

## 7. 로컬 실행 방법

### Kafka 클러스터 시작

```bash
docker-compose -f docker-compose.kafka.yaml up -d
```

### 토픽 확인

```bash
# broker1 컨테이너 접속
docker exec -it broker1 kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# 토픽 상세 정보
docker exec -it broker1 kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic reservation-confirmed
```

### 메시지 확인 (Consumer 수동 테스트)

```bash
docker exec -it broker1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic reservation-confirmed \
  --from-beginning
```

### 메시지 발행 (Producer 수동 테스트)

```bash
docker exec -it broker1 kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic reservation-confirmed
```

---

## 8. 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [Confluent Kafka 튜토리얼](https://developer.confluent.io/get-started/)
