# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

프로젝트 전체 개요와 목표 아키텍처는 상위 디렉터리의 `../CLAUDE.md`와 `../prd.md`를 참고하세요. 이 문서는 `frontend/`만 다룹니다.

## 명령어

```bash
npm run dev                # 개발 서버 (포트 지정: npm run dev -- -p 3100)
npm run build              # 프로덕션 빌드, .next/standalone 생성
npm run lint               # ESLint (flat config, eslint.config.mjs)
npx prettier --write .     # 포맷팅
```

- 테스트 프레임워크가 없어서 테스트 실행 명령도 아직 없습니다.
- 타입 검사는 별도 스크립트 없이 `npm run build` 안에서 수행됩니다.

## 구조와 규칙

- Next.js 16 (App Router), React 19, TypeScript, Tailwind v4. 경로 별칭은 `@/*`이고 `src/` 디렉터리는 쓰지 않습니다.
- 데이터 흐름은 두 갈래입니다.
  - 서버 상태: `lib/api.ts`의 fetch 함수를 TanStack Query 훅에서 호출합니다. `QueryClientProvider`는 `components/Providers.tsx`에 있고 `app/layout.tsx`가 전체를 감쌉니다.
  - 클라이언트 전역 상태: `store/`의 Zustand 스토어입니다. 지금은 로그인 모달 상태(`useUiStore`)만 있습니다.
- API 주소는 `lib/env.ts`의 `API_BASE_URL`로만 읽습니다. 값은 `NEXT_PUBLIC_API_BASE_URL`(기본 `http://localhost:8080`)이고 `.env.example`에 있습니다. `NEXT_PUBLIC_` 값은 빌드 시점에 번들에 박히므로, 컨테이너 이미지에서 주소를 바꾸려면 빌드 인자 처리가 필요합니다.
- 스타일 토큰은 `tailwind.config` 없이 `app/globals.css`의 `@theme inline`에서 정의합니다. 색상은 `text-primary`, `text-secondary` 같은 유틸리티로 씁니다.
- `next.config.ts`의 `output: "standalone"`은 ECS Fargate 컨테이너 배포용이라 유지해야 합니다. `cacheComponents`와 `partialPrefetching`도 켜져 있어서, 서버 컴포넌트에서 요청 시점 데이터를 읽으면 Suspense 경계가 필요할 수 있습니다. 지금은 데이터 호출을 모두 클라이언트 컴포넌트에서 하고 있습니다.
- `app/health/page.tsx`는 백엔드 연동 확인용 임시 페이지입니다. 호출 경로는 Spring Boot Actuator의 `/actuator/health`입니다.
- Prettier는 `prettier-plugin-tailwindcss`로 클래스를 자동 정렬하므로, 클래스 순서는 직접 맞추지 말고 포맷터에 맡기세요.

## Next.js 버전 주의

`AGENTS.md`가 이 버전에서 API와 파일 규칙이 달라졌을 수 있다고 안내합니다. 낯선 API를 쓰기 전에 `node_modules/next/dist/docs/`의 해당 문서를 확인하세요. `next dev`가 `AGENTS.md`의 해당 블록을 자동으로 다시 쓰므로 직접 고치지 않습니다.
