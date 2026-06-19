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
| 다중 알림 채널 | Slack / Telegram / KakaoTalk 중 선택 또는 복수 사용 |
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
-- 네이버 쇼핑
INSERT INTO product (name, url, target_price, created_at)
VALUES ('애플 에어팟 프로 2세대',
        'https://search.shopping.naver.com/...',
        280000, NOW());

-- 쿠팡
INSERT INTO product (name, url, target_price, created_at)
VALUES ('삼성 갤럭시 버즈3',
        'https://www.coupang.com/vp/products/...',
        150000, NOW());
```

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

## 📄 License

MIT
