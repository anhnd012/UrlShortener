# Project: URL Shortener + Analytics Event

## 1. Mục tiêu chính

Xây một backend service giống bản đơn giản của Bitly:

```
Long URL
↓
Short URL
↓
Redirect
↓
Track click
↓
Analytics report
```

Project này dùng để ôn:

```
Spring Boot
PostgreSQL
Redis
Kafka
Java Date Time
Virtual Thread
Docker
Testing
CI/CD
```

---

# 2. Scope tổng thể

## Core Features

| Feature | Mục tiêu học |
| --- | --- |
| Create Short URL | REST API, validation, DB design |
| Redirect URL | HTTP 302, DB index, Redis cache |
| Track Click Event | Kafka producer, async event |
| Analytics Worker | Kafka consumer, background processing |
| Analytics API | aggregation query, reporting |
| Expiration Time | Java Date Time, timezone, Instant |
| Scheduled Cleanup | background job, batch update |
| Virtual Thread Processing | concurrent I/O task |
| Tests | unit/integration test |
| Docker Compose | local reproducible environment |
| GitHub Actions | CI pipeline |

---

# 3. Tech Stack Recommend

## Backend

```
Java 21
Spring Boot 3.x
Spring Web
Spring Data JPA
Spring Validation
Spring Kafka
Spring Scheduler
```

## Database / Infra

```
PostgreSQL
Redis
Kafka
Docker Compose
```

## Testing

```
JUnit 5
Mockito
Testcontainers
Spring Boot Test
```

## DevOps
GitHub Actions
Dockerfile
Docker Compose


# Architecture

```
Client
  ↓
Spring Boot API
  ↓
PostgreSQL

Redirect flow:
Client
  ↓
API
  ↓
Redis cache
  ↓ miss
PostgreSQL
  ↓
Redis set
  ↓
Kafka publish click event
  ↓
302 Redirect

Analytics flow:
Kafka topic: click-events
  ↓
Analytics Worker
  ↓
PostgreSQL click_events
```

---

# 7. Feature Plan + Estimate

## Phase 1 — Core URL Shortener

### Feature 1: Project Setup

Scope:

```
Spring Boot project
PostgreSQL
Docker Compose
BaseEntity
Health check
```

Estimate:

```
0.5 - 1 ngày
```

---

### Feature 2: Create Short URL

Scope:

```
POST /api/v1/urls
Validate longUrl
Generate shortCode
Save DB
Return shortUrl
```

Estimate:

```
1 ngày
```

Bạn cần học:

```
Validation
Entity design
Unique constraint
Exception handling
```

---

### Feature 3: Redirect URL

Scope:

```
GET /{shortCode}
Find by shortCode
Return 302 Redirect
Handle 404
```

Estimate:

```
0.5 - 1 ngày
```

Bạn cần học:

```
HTTP 302
ResponseEntity
DB index
```

---

## Phase 2 — Redis Cache

### Feature 4: Redis Cache for Redirect

Scope:

```
Check Redis first
If miss, query Postgres
Set Redis cache
Add TTL
```

Estimate:

```
1 ngày
```

Bạn cần học:

```
Cache Aside Pattern
TTL
Cache key design
Cache invalidation
```

Cache key:

```
short-url:{shortCode}
```

---

## Phase 3 — Kafka Analytics

### Feature 5: Publish Click Event

Scope:

```
When redirect happens
Publish event to Kafka
Do not block redirect flow
```

Estimate:

```
1 ngày
```

Event:

```json
{
  "shortCode": "abc123",
  "clickedAt": "2026-06-13T10:00:00Z",
  "ipAddress": "...",
  "userAgent": "...",
  "referrer": "..."
}
```

---

### Feature 6: Analytics Worker

Scope:

```
Consume click-events topic
Save click event to DB
Handle consumer error
```

Estimate:

```
1 - 1.5 ngày
```

Bạn cần học:

```
Kafka topic
Producer
Consumer
Consumer group
Offset
Retry
```

---

### Feature 7: Analytics API

Scope:

```
Total clicks
Daily clicks
Query by shortCode
Query by date range
```

Estimate:

```
1 ngày
```

Bạn cần học:

```
GROUP BY
Aggregation query
Date range query
Index usage
```

---

## Phase 4 — Java Date Time

### Feature 8: Expiring Short URL

Scope:

```
User can set expiresAt
Store time in UTC
Check expiration when redirect
Return 410 if expired
```

Estimate:

```
1 - 1.5 ngày
```

Bạn cần học:

```
Instant
LocalDateTime
ZonedDateTime
ZoneId
Clock
UTC storage
```

Rule:

```
API nhận timezone của user
Backend convert về Instant
DB lưu UTC
Business logic dùng Instant
```

---

### Feature 9: Timezone-aware Analytics

Scope:

```
GET analytics by date range
Group daily clicks based on user's timezone
```

Example:

```
GET /api/v1/analytics/abc123?from=2026-06-01&to=2026-06-13&timezone=Asia/Ho_Chi_Minh
```

Estimate:

```
1 - 2 ngày
```

Đây là feature rất tốt để luyện Date Time.

---

## Phase 5 — Background Job + Virtual Thread

### Feature 10: Scheduled Cleanup Job

Scope:

```
Every 5 minutes
Find expired active links
Mark as EXPIRED
Delete Redis cache
```

Estimate:

```
0.5 - 1 ngày
```

---

### Feature 11: Virtual Thread Batch Processing

Scope:

```
Process expired links concurrently
Each task:
- update status
- delete Redis cache
```

Estimate:

```
1 ngày
```

Bạn cần học:

```
Virtual thread
I/O-bound task
ExecutorService
Concurrency limit
Error handling
```

Không dùng virtual thread cho mọi thứ. Dùng ở cleanup job là hợp lý vì có nhiều I/O nhỏ với DB/Redis.

---

## Phase 6 — Quality

### Feature 12: Unit Test

Scope:

```
ShortCodeGeneratorTest
UrlExpirationServiceTest
AnalyticsServiceTest
```

Estimate:

```
1 ngày
```

---

### Feature 13: Integration Test

Scope:

```
Postgres Testcontainers
Redis Testcontainers
Kafka Testcontainers nếu còn thời gian
```

Estimate:

```
2 - 3 ngày
```

---

### Feature 14: GitHub Actions

Scope:

```
Run test
Build jar
Build Docker image
```

Estimate:

```
0.5 - 1 ngày
```

---

# 8. Tổng Estimate

## Nếu chỉ code cho chạy

```
7 - 10 ngày
```

## Nếu làm để cải thiện bản thân thật sự

```
3 - 4 tuần
```

Mình recommend bạn chọn:

```
3 tuần
```

---

# 9. Plan 3 tuần recommend

## Tuần 1 — Core Backend + Redis

```
Day 1: Setup project + Docker Compose + Postgres
Day 2: Create Short URL
Day 3: Redirect URL
Day 4: Redis cache
Day 5: Refactor + unit test cơ bản
Day 6-7: Review, write docs, fix bugs
```

Output cuối tuần 1:

```
App tạo short URL được
Redirect được
Redis cache hoạt động
Có README basic
```

---

## Tuần 2 — Kafka + Analytics

```
Day 1: Setup Kafka
Day 2: Publish click event
Day 3: Analytics worker consume event
Day 4: Save click events to DB
Day 5: Analytics API
Day 6-7: Test + refactor
```

Output cuối tuần 2:

```
Redirect sinh click event
Worker xử lý event
Analytics API trả được số click
```

---

## Tuần 3 — Date Time + Virtual Thread + CI/CD

```
Day 1: Expiration logic
Day 2: Timezone-aware analytics
Day 3: Scheduled cleanup job
Day 4: Virtual thread processing
Day 5: Integration tests
Day 6: GitHub Actions
Day 7: README + CV bullet + architecture diagram
```

Output cuối tuần 3:

```
Project đủ đẹp để đưa CV
Bạn giải thích được Redis, Kafka, Date Time, Scheduler, Virtual Thread
```

---

# 10. Rule làm project để tiến bộ nhanh

## Rule 1: Không code ngay

Mỗi feature phải viết trước:

```
Requirement
API
DB change
Sequence flow
Failure cases
Test cases
```

---

## Rule 2: Mỗi feature phải có ít nhất 5 failure cases

Ví dụ Create Short URL:

```
URL null
URL invalid
URL quá dài
shortCode duplicate
DB error
```

---

## Rule 3: Mỗi feature phải có test trước hoặc test case list trước

Không nhất thiết TDD hoàn toàn, nhưng phải có test case list.

---

## Rule 4: Không để AI viết toàn bộ feature

Quy trình tốt hơn:

```
Bạn tự design
Bạn tự code
Bạn tự chạy
Sau đó nhờ AI review
Sau đó refactor
```

---

## Rule 5: Mỗi feature phải có một đoạn “Interview Explanation”

Ví dụ Redis: