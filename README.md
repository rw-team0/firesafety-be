# ArcGuard (아크가드)

> 전기화재 예방 스마트 진단/모니터링 시스템 — 백엔드 (Modular Monolith)

---

## 소개

씨에스텍 하드웨어(분전반 센서)가 보내는 데이터를 수신해 하드웨어 판정 + AI 아크 판정을 종합하고, 실시간 관제화면(WebSocket)과 경보(FCM 푸시 포함)로 이어주는 백엔드입니다. 단일 Spring Boot 애플리케이션 안에서 도메인 패키지로 분리된 Modular Monolith 구조를 사용합니다.

---

## 실행/문서 링크

| 항목 | 값 |
|---|---|
| 로컬 서버 | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |


### 테스트 계정 (local, `.env` 기준)

| 이메일 | 비밀번호 | 등급 |
|---|---|---|
| master@safety.com | master1234! | SUPER_ADMIN |
| (bootstrap 계정, `BOOTSTRAP_SUPER_ADMIN_ENABLED=true`일 때 자동 생성) | | |

> 그 외 ADMIN/GENERAL 계정은 로그인 후 SUPER_ADMIN → ADMIN → GENERAL 순으로 직접 등록해서 사용합니다.

---

## 팀원 및 담당 도메인

| 이름 | 담당 |
|---|---|
| 박소영 | 백엔드 전체 (auth, facility, sensor, diagnosis, monitoring, alert, statistics) |
| 이현주 | 프론트엔드 |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.0.7 |
| 인증 | Spring Security + JWT(HttpOnly Cookie, `jjwt` 0.12.6) |
| DB 접근 | MyBatis  |
| DB | MySQL |
| 마이그레이션 | Flyway |
| 실시간 통신 | WebSocket (STOMP) |
| API 문서 | springdoc-openapi 2.8.9 |
| 엑셀 | Apache POI 5.3.0 (경보 이력 다운로드) |
| 푸시 알림 | Firebase Admin SDK 9.4.3 (FCM) |
| 외부 API 연동 | Spring Cloud OpenFeign (AI 예측 서버) |
| 메일 발송 | Spring Mail (SMTP, Gmail/SES 등 교체 가능) |
| 환경변수 로드 | dotenv-java 3.0.0 (`.env`를 UTF-8로 직접 읽어 인코딩 깨짐 방지) |
| 빌드 | Gradle |

---

## 도메인 구성 (Modular Monolith)

| 패키지 | 역할 |
|---|---|
| `common` | 공통 응답(`ResultResponse`), 에러코드, JWT, 인증 컨텍스트 |
| `auth` | 로그인/로그아웃/재발급, 계정 CRUD·소프트삭제·복구, 비밀번호 재설정, 감사 로그 |
| `facility` | 현장/분전반/회로 CRUD, 담당현장배정, 설비 감사 로그 |
| `sensor` | 하드웨어 센서 데이터 수신(`GET /m_noUpload.php`), Mock 센서 스케줄러(발표/데모용) |
| `diagnosis` | AI 서버(`POST /predict`) 연동, 회로별 진단결과 |
| `monitoring` | 대시보드 요약, WebSocket(STOMP) 실시간 갱신, 분전반 상태 집계, 통신두절 감지 |
| `alert` | 경보 생성/조회, 상태 전이(미확인→확인→조치), FCM 발송, 엑셀 다운로드 |
| `statistics` | 기간별 통계 조회 |

### 특이사항

- `GET /m_noUpload.php`는 표준 REST 컨벤션(`/api/...`)을 따르지 않는 예외 경로입니다. 씨에스텍 하드웨어 고정 프로토콜이라 임의로 바꾸지 않습니다.
- 회로/장비 상태(정상/주의/위험)는 DB 저장값이 아니라 **하드웨어 판정 + 최신 AI 판정을 조합해 조회 시점에 계산**하는 값입니다.
- `user`, `site`, `panel`, `circuit`, `user_site`는 소프트 삭제만 합니다 (물리 삭제 없음).

---

## 실행 방법

### 사전 요구사항

- Java 21
- MySQL (로컬 실행 기준, 원격 접속 정보도 가능)

### 1. 환경변수 설정

```bash
cp .env.example .env
# .env를 열어 DB_URL/DB_USERNAME/DB_PASSWORD, JWT_SECRET_KEY, MAIL_* 등 실제 값으로 채운다
```

`.env`는 Java Properties가 아니라 `dotenv-java`로 직접 읽으므로 한글 값을 그대로 써도 됩니다.

### 2. 서버 실행

```bash
./gradlew bootRun
```

기동 시 Flyway가 마이그레이션을 자동 적용하고, `BOOTSTRAP_SUPER_ADMIN_ENABLED=true`면 마스터 계정을 자동 생성합니다.

### 3. 하드웨어 없이 데모하기 (Mock 센서)

```bash
SENSOR_MOCK_ENABLED=true   # .env에서 설정
SENSOR_MOCK_DELAY_MS=5000  # 생성 주기(ms)
```

활성 분전반이 있으면 실제 장비 없이도 센서 프레임 → DEVICE 경보 → 대시보드/WebSocket 갱신까지 기존 수신 흐름 그대로 태웁니다. 운영에서는 반드시 `false`.

### 4. 검증

```bash
./gradlew test
./gradlew clean build
```
