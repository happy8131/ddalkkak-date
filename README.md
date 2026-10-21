# 데이트 딸깍

AI가 추천하는 데이트 코스 서비스입니다. 프론트엔드(Next.js), 백엔드(Spring Boot), 인프라(Terraform)를 하나의 리포지토리에서 관리합니다. 요구사항은 [prd.md](./prd.md)를 참고하세요.

| 디렉터리 | 내용 |
| --- | --- |
| `frontend/` | Next.js (App Router) + TypeScript + Tailwind |
| `backend/` | Spring Boot (Java 17, Gradle) + PostgreSQL |
| `infra/` | Terraform (prod 단일 환경, 리전 us-east-1). 사용법은 [infra/README.md](./infra/README.md) |
| `docker-compose.yml` | 로컬 개발 환경 (frontend + backend + postgres) |

## 로컬 실행 (Docker Compose)

### 준비물

- Docker Desktop (Compose v2.32 이상)
- 그 외 설치는 필요 없습니다. Node.js와 JDK는 컨테이너 안에서 실행됩니다.

### 실행

```bash
# 1. (선택) 환경 변수 파일 만들기. 기본값이 있어서 생략해도 실행됩니다.
cp .env.example .env

# 2. 전체 기동 (처음에는 이미지 빌드와 의존성 다운로드로 몇 분 걸립니다)
docker compose up --build

# 소스 변경 시 백엔드까지 자동 재시작하려면 아래 명령을 대신 사용합니다.
docker compose watch
```

| 서비스 | 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 |
| 연동 확인 페이지 (FE → BE → DB) | http://localhost:3000/health |
| 백엔드 헬스체크 | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5433` (DB/계정/비밀번호 모두 `ddalkkak`) |

`/health` 페이지에 "백엔드 상태: UP"이 보이면 프론트엔드 → 백엔드 → DB 연결이 모두 정상입니다. 백엔드 헬스체크는 DB 연결 상태를 포함하므로, DB가 내려가면 이 페이지에 "연결 실패: 헬스체크 실패: 503"이 표시됩니다.

### 종료와 초기화

```bash
docker compose down        # 컨테이너 종료 (DB 데이터는 유지)
docker compose down -v     # 컨테이너와 볼륨까지 삭제 (DB 데이터 초기화)
```

### 핫 리로드

- **프론트엔드:** `frontend/` 소스를 저장하면 약 2초 안에 반영됩니다. Docker Desktop은 파일 변경 이벤트를 컨테이너에 전달하지 못해서 `next.config.ts`의 `NEXT_WATCH_POLL_MS`(compose에서 1000ms)로 폴링합니다. Next.js 문서는 폴링을 최후의 수단으로 권장하므로, CPU 사용량이 신경 쓰이면 프론트엔드만 `docker compose up postgres backend`로 띄우고 호스트에서 `cd frontend && npm run dev`로 실행하세요.
- **백엔드:** `docker compose watch`로 실행했을 때 `backend/src` 또는 `backend/build.gradle`이 바뀌면 백엔드 컨테이너가 재시작되며 다시 컴파일합니다. 반영까지 30~40초 걸립니다. 에디터로 저장한 변경은 감지되지만, `sed -i` 같은 명령행 치환은 감지되지 않을 수 있습니다.

### 포트 충돌

호스트에 PostgreSQL이 이미 5432를 쓰는 경우가 많아서 컨테이너 DB는 기본으로 호스트 5433에 연결합니다. 다른 포트가 필요하면 `.env`에서 바꿉니다.

```
POSTGRES_HOST_PORT=5433
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=3000
```

`FRONTEND_HOST_PORT`를 바꾸면 백엔드 CORS 허용 주소(`backend/src/main/resources/application-local.yml`의 `app.cors.allowed-origins`, 기본 `http://localhost:3000`)도 함께 바꿔야 합니다.

## 개별 실행

Docker 없이 실행하려면 각 디렉터리의 설명을 참고하세요. 명령어와 구조는 [CLAUDE.md](./CLAUDE.md)에 정리되어 있습니다.

- 프론트엔드: `cd frontend && npm install && npm run dev`
- 백엔드: PostgreSQL을 띄운 뒤 `cd backend && ./gradlew bootRun` (기본 DB 주소는 `localhost:5432`이며 `DB_URL`로 바꿀 수 있습니다)

## 브랜치와 작업 방식

Jira 티켓(KAN-*) 단위로 브랜치를 만들어 작업하고, 티켓이 완전히 끝나면 `main`에 머지합니다. 자세한 규칙은 [CLAUDE.md](./CLAUDE.md)의 "Git 워크플로우"를 참고하세요.
