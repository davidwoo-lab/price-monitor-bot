# 🔔 Price Monitor Bot

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![Slack](https://img.shields.io/badge/Slack-Webhook-4A154B?style=for-the-badge&logo=slack)
![Telegram](https://img.shields.io/badge/Telegram-Bot-26A5E4?style=for-the-badge&logo=telegram)
![KakaoTalk](https://img.shields.io/badge/KakaoTalk-Notify-FFCD00?style=for-the-badge&logo=kakaotalk)

네이버 쇼핑 / 쿠팡 상품 가격을 주기적으로 크롤링하여, 목표 가격 이하로 떨어지면 Slack / Telegram / KakaoTalk으로 알림을 발송하는 자동화 봇입니다.

---

## 📌 주요 기능

| 기능 | 설명 |
|---|---|
| 가격 크롤링 | 네이버 쇼핑 / 쿠팡 상품 URL 기반 현재 최저가 수집 |
| 플랫폼 자동 감지 | 등록된 URL로 네이버 / 쿠팡 크롤러 자동 선택 |
| 목표 가격 설정 | 상품별 알림 발송 기준 가격 설정 |
| 상품별 알림 채널 | 상품마다 Slack / Telegram / KakaoTalk 중 하나를 지정해 발송 (관리자 알림은 전체 채널) |
| 주기적 자동 실행 | Spring Scheduler 기반 크론 표현식으로 실행 주기 설정 |
| 가격 이력 저장 | 수집된 가격 데이터 DB 저장 및 이력 조회 |
| 중복 알림 방지 | 동일 조건 알림은 지정 시간 내 재발송 차단 |

---

## 🏗️ 아키텍처

```
Scheduler (Cron)
  └── CrawlerService
        ├── NaverShoppingCrawler (Jsoup)
        ├── CoupangCrawler (Jsoup)
        └── PriceHistoryRepository (JPA)
              └── MySQL

PriceAlertService
  └── 목표 가격 비교
        └── NotificationDispatcher
              ├── SlackNotifier (Webhook)
              ├── TelegramNotifier (Bot API)
              └── KakaoNotifier (KakaoTalk Notify API)
```

### 패키지 구조

```
src/main/java/com/davidlab/pricemonitor/
├── common/
│   ├── config/          # Scheduler, JPA 설정
│   └── exception/       # 전역 예외 처리
├── crawler/
│   ├── PriceCrawler.java          # 크롤러 인터페이스
│   ├── NaverShoppingCrawler.java  # 네이버 쇼핑 구현체
│   ├── CoupangCrawler.java        # 쿠팡 구현체
│   ├── CrawlerFactory.java        # URL 기반 크롤러 자동 선택
│   └── dto/                       # 크롤링 결과 DTO
├── product/
│   ├── domain/          # 모니터링 상품 Entity
│   ├── repository/
│   └── service/
├── price/
│   ├── domain/          # 가격 이력 Entity
│   ├── repository/
│   └── service/
├── alert/
│   ├── domain/          # 알림 설정 Entity
│   ├── service/         # 가격 비교 및 알림 트리거
│   └── scheduler/       # Cron 스케줄러
└── notification/
    ├── NotificationDispatcher.java
    ├── NotificationChannel.java  (interface)
    ├── slack/
    ├── telegram/
    └── kakao/
```

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| 크롤링 | Jsoup |
| ORM | Spring Data JPA (Hibernate) |
| DB | MySQL 8.0 |
| 스케줄러 | Spring Scheduler (@Scheduled) |
| 알림 | Slack Incoming Webhook / Telegram Bot API / KakaoTalk Notify API |
| 빌드 | Gradle |

---

## 🚀 실행 방법

### 사전 요구사항

- Java 17+
- MySQL 8.0+
- 사용할 알림 채널 API 키 (하나 이상)

### 알림 채널 설정

```yaml
# src/main/resources/application.yml
notification:
  slack:
    enabled: true
    webhook-url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
  telegram:
    enabled: false
    bot-token: YOUR_BOT_TOKEN
    chat-id: YOUR_CHAT_ID
  kakao:
    enabled: false
    access-token: YOUR_ACCESS_TOKEN

scheduler:
  cron: "0 0 * * * *"   # 매 시간 정각 실행 (기본값)
```

> `notification.<채널>.enabled`는 **채널의 사용 가능 여부**(봇이 해당 채널로 발송할 수 있는지)를 제어합니다.
> **어느 상품을 어느 채널로 받을지**는 아래 `product_notification` 테이블에서 상품별로 지정합니다.
> 현재는 **상품당 단일 채널**만 지원합니다(스키마는 다중 채널 확장 대비).

### DB 초기화 (최초 1회)

JPA 설정이 `ddl-auto: validate`이므로 애플리케이션이 테이블을 자동 생성하지 않습니다.
MySQL 같은 비임베디드 DB는 `schema.sql`을 자동 실행하지 않으므로 **최초 1회 수동 실행**이 필요합니다.

```bash
# 스키마 생성 (테이블이 없으면 기동 시 validate 실패함)
mysql -u root -p < src/main/resources/schema.sql
```

> 자동 실행을 원하면 `application.yml`에 `spring.sql.init.mode: always`를 추가할 수 있으나,
> 운영 환경에서는 명시적 수동 마이그레이션을 권장합니다.

### 실행

```bash
git clone https://github.com/david-lab/price-monitor-bot.git
cd price-monitor-bot
./gradlew bootRun
```

---

## 📡 모니터링 상품 등록 예시

상품 URL만 등록하면 네이버 / 쿠팡 크롤러가 자동으로 선택됩니다.

```sql
-- 네이버 쇼핑 (슬랙으로 알림 수신)
INSERT INTO product (name, url, target_price, active, below_target, created_at, updated_at)
VALUES ('애플 에어팟 프로 2세대',
        'https://search.shopping.naver.com/...',
        280000, 1, 0, NOW(), NOW());
INSERT INTO product_notification (product_id, channel)
VALUES (LAST_INSERT_ID(), 'SLACK');

-- 쿠팡 (텔레그램으로 알림 수신)
INSERT INTO product (name, url, target_price, active, below_target, created_at, updated_at)
VALUES ('삼성 갤럭시 버즈3',
        'https://www.coupang.com/vp/products/...',
        150000, 1, 0, NOW(), NOW());
INSERT INTO product_notification (product_id, channel)
VALUES (LAST_INSERT_ID(), 'TELEGRAM');
```

> `channel` 값은 `SLACK` / `TELEGRAM` / `KAKAO` 중 하나이며, 해당 채널이 `application.yml`에서
> `enabled: true`로 활성화되어 있어야 실제 발송됩니다. 상품에 채널이 지정되지 않으면 알림이 발송되지 않습니다.

---

## 🔔 알림 메시지 예시

```
[가격 알림] 애플 에어팟 프로 2세대
플랫폼: 네이버 쇼핑
현재가: 275,000원 (목표가: 280,000원)
📉 목표 가격 달성!
🔗 https://search.shopping.naver.com/...

[가격 알림] 삼성 갤럭시 버즈3
플랫폼: 쿠팡
현재가: 148,000원 (목표가: 150,000원)
📉 목표 가격 달성!
🔗 https://www.coupang.com/vp/products/...
```

---

## ⏱️ 스케줄러 크론 표현식 예시

| 표현식 | 실행 주기 |
|---|---|
| `0 0 * * * *` | 매 시간 정각 |
| `0 */30 * * * *` | 30분마다 |
| `0 0 9,18 * * *` | 매일 오전 9시, 오후 6시 |
| `0 0 9 * * MON-FRI` | 평일 오전 9시 |

---

## 🛡️ 안정화 / 데이터 품질

| 구분 | 항목 | 설명 |
|---|---|---|
| 기능 | 할인 정보 강화 | 알림 메시지에 직전 가격 대비 하락폭·할인율 표시 |
| 기능 | 역대 최저가 알림 | 역대 최저가 갱신 시 🏆 표시 |
| 기능 | 가격 재하락 재알림 | 목표가 위로 올랐다가 다시 내려오면 쿨다운 무시하고 즉시 알림 |
| 기능 | 상품별 알림 채널 | 상품마다 채널 지정 (`product_notification`) |
| 데이터 | 비정상 가격 변동 필터 | 직전 대비 ±70% 초과 시 이상치로 보고 알림 보류 (오알림 방지) |
| 데이터 | 이력 집계/보관 | 일 단위 집계(`price_daily_summary`) 후 원본 이력 30일 보관·정리 |
| 안정성 | 일시적 실패 재시도 | 네트워크 타임아웃 등 일시 오류 1~2회 재시도 |
| 안정성 | HTTP 타임아웃 | 알림 API 연결/응답 타임아웃으로 스케줄러 블로킹 방지 |
| 운영 | 다중 인스턴스 락(ShedLock) | 여러 인스턴스 배포 시 스케줄러 중복 실행 방지 |
| 운영 | Actuator health | 봇 생존 여부 모니터링 |

> 상세 작업 계획은 [docs/work-plan.md](docs/work-plan.md) 참고

---

## 📄 License

MIT
