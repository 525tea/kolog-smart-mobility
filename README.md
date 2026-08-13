# KOLOG Smart Mobility

AI 철도 공동화물 거래소(KOLOG) — 여러 화주의 화물을 AI로 공동화하고 화물열차 잔여 적재용량과 매칭하는
화주 ↔ 코레일 양면 플랫폼.

- 아키텍처/도메인 모델(ERD): [docs/architecture/overview.md](docs/architecture/overview.md)
- E2E 검증 현황: [docs/qa/e2e-status.md](docs/qa/e2e-status.md)
- 프론트엔드 안내: [frontend/README.md](frontend/README.md)
- 최신 KOLOG Figma의 A01~A20 화면 흐름과 코레일 운영자 화면을 구현했다.

## 모노레포 구조

```
kolog-smart-mobility/
├── backend/    # Spring Boot 4 (Java 17) API 서버
├── frontend/   # React + Vite + TS + Tailwind (화주용 웹앱)
├── infra/      # 로컬 개발용 Docker Compose
├── docs/       # 기획/기능명세, 아키텍처 문서
└── .github/    # CI
```

## 로컬 실행 (백엔드 + 프론트엔드)

```bash
# 1) 백엔드 + DB
cp infra/.env.example infra/.env
docker compose --env-file infra/.env -f infra/docker-compose.yml up --build

# 2) 프론트엔드 (다른 터미널)
cd frontend
npm install
npm run dev
```

- 프론트엔드: http://localhost:5173
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### 공개 시연 계정

배포 환경에서 `DEMO_DATA_ENABLED=true`이면 서버 시작 시 아래 공개 계정과 시연용 사업자·운송·예약·알림 데이터를
자동으로 준비한다. 이 계정은 팀 시연과 심사위원 탐색 전용이며 실제 화주 정보를 저장하면 안 된다.

- 이메일: `demo@kolog.kr`
- 비밀번호: `demo1234`

PDF·이미지 OCR은 Google Document AI 설정 전까지 미해결이며, 현재 E2E 범위와 확인 순서는
[docs/qa/e2e-status.md](docs/qa/e2e-status.md)에 기록한다. 엑셀(XLS/XLSX) 추출은 서버 내부에서 동작한다.

### 실제 LLM(Gemini)으로 화물 정보 구조화 켜기 (선택, 무료)

기본값은 키 없이 동작하는 규칙기반(rule-based) 추출이다. 실제 LLM을 쓰려면:

1. https://aistudio.google.com/apikey 에서 무료 API 키 발급
2. `infra/.env`에 아래 값 채우기 (커밋 금지 — `.gitignore` 처리되어 있음)
   ```
   AI_PROVIDER=gemini
   GEMINI_API_KEY=발급받은키
   ```
3. `docker compose ... up --build` 재실행

키가 없거나 API 호출이 실패하면 자동으로 규칙기반으로 폴백하므로 서비스가 죽지 않는다.

## 백엔드만 로컬에서 실행 (MySQL은 Docker로만 띄우는 경우)

```bash
docker compose -f infra/docker-compose.yml up mysql -d
cd backend
./gradlew bootRun
```

## 테스트

**백엔드**: Docker가 필요합니다 (Testcontainers로 실제 MySQL을 띄워 Flyway 마이그레이션까지 검증합니다).

```bash
cd backend
./gradlew test
```

**프론트엔드**: 타입체크·프로덕션 빌드·린트를 실행합니다.

```bash
cd frontend
npm run build
npm run lint
```

## 기술 스택

**백엔드**: Java 17 · Spring Boot 4 · Spring Security(JWT) · Spring Data JPA · MySQL 8 · Flyway · springdoc-openapi ·
Google Gemini API(선택) · Google OR-Tools(CP-SAT) · JUnit5/Mockito/AssertJ/Testcontainers.

**프론트엔드**: React 19 · Vite · TypeScript · Tailwind CSS 4 · React Router.

자세한 구현 방식과 검증 범위는 [아키텍처 문서](docs/architecture/overview.md)와
[E2E 검증 현황](docs/qa/e2e-status.md)을 참고한다.
