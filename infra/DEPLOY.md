# 배포 가이드 (오라클클라우드 Always Free VM 기준)

로컬 개발용 `docker-compose.yml`과 달리, 배포용 `docker-compose.prod.yml`은 DB/백엔드 포트를 외부에
열지 않고 **Nginx 하나만 80번 포트로 노출**한다. 브라우저는 이 Nginx 주소 하나만 알면 되고, 프론트
정적파일 서빙과 `/api/**` 백엔드 프록시를 Nginx가 알아서 나눠준다 (자세한 구조는 `docker-compose.prod.yml`,
`nginx/nginx.conf` 주석 참고).

## 0. 사전 준비물

- OCI Always Free VM (Ubuntu 22.04/24.04), 공인 IP
- SSH 접속 가능한 상태 (private key 또는 비밀번호)
- (선택) Gemini API 키 — 실제 LLM 분석을 쓰려면. 안 쓰면 `AI_PROVIDER=rule-based`로 규칙기반 사용
- Google Cloud 프로젝트와 Document AI Form Parser 프로세서 — PDF·이미지 OCR에 필수

## 1. VM에 Docker 설치

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
# 위 명령 후 한 번 재접속(SSH 재접속)해야 docker 그룹 권한이 적용된다
```

## 2. 저장소 클론

```bash
git clone https://github.com/525tea/kolog-smart-mobility.git
cd kolog-smart-mobility/infra
```

## 3. 환경변수 설정 (★ 기본값 그대로 배포 금지)

```bash
cp .env.prod.example .env
nano .env   # 또는 vim
```

최소한 아래 값은 반드시 실제 값으로 바꿔야 한다 (안 바꾸면 컨테이너가 아예 안 뜬다 — `docker-compose.prod.yml`에
`:?에러메시지` 필수값 체크를 걸어뒀다):

- `DB_PASSWORD`, `DB_ROOT_PASSWORD` — 아무 무작위 문자열로
- `JWT_SECRET` — 32자 이상 무작위 문자열로 (예: `openssl rand -hex 32`)
- `OPERATOR_EMAIL`, `OPERATOR_PASSWORD` — 최초 운영자 로그인 정보(비밀번호 12자 이상)
- `GOOGLE_CLOUD_PROJECT`, `DOCUMENT_AI_LOCATION`, `DOCUMENT_AI_PROCESSOR_ID` — Form Parser 생성 화면의 값

공개 회원가입으로는 운영자 계정을 만들 수 없다. 첫 부팅에서 `OPERATOR_BOOTSTRAP_ENABLED=true`로
계정을 만든 뒤에는 `false`로 바꿔도 기존 계정은 유지된다.

### Google Document AI 인증 설정 (PDF·이미지 OCR 필수)

1. Google Cloud에서 **Document AI API**를 활성화한다.
2. Processor Gallery에서 **Form Parser** 프로세서를 만들고 프로젝트 ID·리전(`us` 또는 `eu`)·프로세서 ID를 `.env`에 입력한다.
3. Document AI 호출 권한이 있는 전용 서비스 계정을 만들고 JSON 키를 내려받는다.
4. VM의 저장소에서 아래처럼 키를 배치한다. 이 디렉터리는 `.gitignore`에 포함되어 절대 Git에 올라가지 않는다.

```bash
mkdir -p secrets
cp /안전한/경로/service-account.json secrets/google-document-ai.json
chmod 600 secrets/google-document-ai.json
```

`.env`에는 다음 값을 설정한다.

```dotenv
DOCUMENT_AI_ENABLED=true
GOOGLE_CLOUD_PROJECT=실제_프로젝트_ID
DOCUMENT_AI_LOCATION=us
DOCUMENT_AI_PROCESSOR_ID=실제_Form_Parser_프로세서_ID
GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/google-document-ai.json
```

엑셀(XLS/XLSX)은 외부 전송 없이 백엔드에서 시트와 셀을 직접 읽는다. PDF·이미지는 Google Document AI로 전송되므로 실제 화주 문서를 다룰 때는 Google Cloud의 데이터 처리·보관 정책과 사내 개인정보 처리 기준을 함께 확인해야 한다.

## 4. 방화벽 열기

**OCI 콘솔**: 인스턴스 상세 → Subnet → Security List (또는 NSG 사용 시 그쪽) → Ingress Rules 추가
- SSH `22`번은 가능하면 관리자의 공인 IP로만 제한한다.
- Source: `0.0.0.0/0`, Protocol: TCP, Destination Port: `80` (나중에 HTTPS 쓰면 `443`도 추가)
- `3306`(MySQL), `8080`(백엔드)은 OCI 보안 목록과 VM 방화벽 모두에서 열지 않는다.

**VM 자체 방화벽** (Ubuntu는 기본적으로 iptables가 막혀있는 경우가 많음):

```bash
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
sudo netfilter-persistent save   # 없으면: sudo apt install -y iptables-persistent
```

## 5. 빌드 + 실행

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker compose -f docker-compose.prod.yml ps   # mysql/backend/nginx 모두 Up인지 확인
curl -fsS http://127.0.0.1/health             # {"status":"UP"} 확인
```

첫 빌드는 프론트(npm ci + build)와 백엔드(gradle build)를 둘 다 하기 때문에 몇 분 걸릴 수 있다.

## 6. 접속 확인

```
http://<VM 공인 IP>
```

이 주소 하나로 프론트 화면이 뜨고, 프론트가 호출하는 `/api/v1/**` 요청은 Nginx가 내부적으로 백엔드로
전달한다. 로그인/화물등록 등 실제 플로우가 되는지 확인한다.

운영자 계정으로 로그인하면 `/operator`로 이동하며, 화차 배정 재실행과 승인/반려를 처리할 수 있다.

## 7. 로그 확인 / 문제 생겼을 때

```bash
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f nginx
docker compose -f docker-compose.prod.yml logs -f mysql
```

## 8. HTTPS 적용 (필수)

로그인 비밀번호와 JWT를 다루므로 운영 서버에서 HTTP만 사용하는 것은 허용하지 않는다. 도메인이 없어도
Let's Encrypt의 단기 IP 인증서를 사용할 수 있다. IP 인증서는 약 6일만 유효하므로 자동 갱신이 필수다.

OCI 보안 목록/NSG와 VM 방화벽에서 TCP 443을 먼저 연다. `.env`에는 아래 값을 추가한다.

```dotenv
TLS_SERVER_NAME=158.179.178.77
```

HTTP 구성을 먼저 실행해 ACME webroot를 연 뒤 Certbot 5.4 이상으로 인증서를 발급한다.

```bash
mkdir -p certbot/conf certbot/www
docker compose -f docker-compose.prod.yml --env-file .env up -d --build

docker run --rm \
  -v "$PWD/certbot/conf:/etc/letsencrypt" \
  -v "$PWD/certbot/www:/var/www/certbot" \
  certbot/certbot:latest certonly \
  --preferred-profile shortlived \
  --webroot --webroot-path /var/www/certbot \
  --ip-address 158.179.178.77 \
  --agree-tos --no-eff-email -m 실제_관리자_이메일
```

발급 후 TLS 오버레이를 함께 실행한다. HTTP 요청은 HTTPS로 자동 이동한다.

```bash
docker compose \
  -f docker-compose.prod.yml \
  -f docker-compose.tls.yml \
  --env-file .env up -d --build nginx

curl -I https://158.179.178.77/login
```

IP 인증서는 단기 인증서이므로 최소 하루 한 번 `certbot renew` 후 Nginx를 reload하도록 cron/systemd timer를
설정해야 한다. 갱신 명령의 webroot와 인증서 볼륨은 위 발급 명령과 동일하게 사용한다.

운영 구성에서는 Swagger와 `/v3/api-docs`를 비활성화하고, 로그인 요청을 IP당 분당 5회로 제한한다.
또한 CSP, 클릭재킹 방지, MIME 스니핑 방지, HSTS 등 기본 보안 헤더를 Nginx가 반환한다.

## 9. 갱신 배포 (코드 수정 후 다시 반영할 때)

```bash
cd kolog-smart-mobility
git pull
cd infra
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```
