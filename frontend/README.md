# KOLOG frontend

화주(Shipper)용 웹 프론트엔드. React + Vite + TypeScript + Tailwind CSS.
[제공된 Figma 화면 14개](../docs/planning/functional-spec.md#10-프론트엔드-화면-구현-현황)를 기준으로 구현했다.

## 실행

```bash
npm install
cp .env.example .env   # 필요하면 VITE_API_BASE_URL 수정
npm run dev
```

기본적으로 `http://localhost:8080`(docker-compose로 띄운 백엔드)을 바라본다. 백엔드를 먼저 띄워야 한다:

```bash
cd ../infra && cp .env.example .env && docker compose --env-file .env -f docker-compose.yml up --build
```

## 구조

```
src/
├── api/         # 백엔드 REST 호출 (도메인별 함수, fetch 래퍼)
├── context/     # Auth(로그인/토큰), Notification(서버 알림 API 폴링)
├── components/  # layout(헤더/하단탭/위저드), ui(버튼/카드/뱃지 등 공용 컴포넌트)
├── pages/       # 화면별 컴포넌트 (cargo/ 하위는 5단계 위저드)
└── types/       # 백엔드 DTO와 1:1로 맞춘 TS 타입
```

## 디자인 대비 단순화한 부분

디자인에는 있었지만 아래 4가지는 이번 작업에서 백엔드까지 실제로 구현해 반영했다
(계산 로직/근거는 [functional-spec.md](../docs/planning/functional-spec.md), 요율 등 mock 값의 근거는
`backend`의 `PricingPolicy`/`HazardGrade`/`TrackingSimulationService` 주석 참고):

- 위험물 A~D 등급 + 컨테이너 할증: AI가 등급을 추정하고(또는 화주가 직접 수정), 등급별 할증률(8~20%, 임의 가정치)이 운임에 실제로 반영된다.
- 적재보험료/플랫폼 수수료: 화주가 등록 시 신고한 화물가액 기준 보험료(0.3%, 임의 가정치)와 운임 기준 플랫폼 수수료(5%, 임의 가정치)를 실제로 계산해 결제금액에 합산한다.
- 운송현황의 실시간 위치: 실제 GPS 단말 연동은 없지만, 열차 시간표(출발/도착 시각) 기준 경과 비율로 출발역-도착역 사이를 선형보간한 좌표를 서버가 계산해 보여준다 (역 좌표는 근사값).
- 알림: 서버에 알림 테이블/API가 생겨, 화물 등록·AI 분석·매칭·코레일 승인·공동화 실패 처리 등 실제 상태 변화가 있을 때마다 서버가 알림을 저장한다. 프론트는 8초 간격으로 폴링해서 보여준다.

여전히 남아있는 단순화/미구현 항목:

- 법인 SSO 로그인, 비밀번호 재설정 → 버튼은 있지만 준비 중 안내만 표시 (백엔드 미구현)
- 위험물 등급 기준, 보험료율, 플랫폼 수수료율은 공식 요율표가 아니라 서비스 자체에서 가정한 mock 값이다 (실제 비즈니스 적용 전 재검토 필요).
- 위치추적은 시뮬레이션이며, 실제 화차 GPS 단말과 연동된 것이 아니다.
