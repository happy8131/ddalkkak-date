# 데이트 딸깍 MVP - Product Requirements Document (PRD)

**문서 버전:** v1.0

**작성일:** 2025년 10월 13일

**작성자:** Product Team

**검토자:** Engineering Lead, Design Lead

**승인 필요:** CEO, CTO

---

## 📋 Executive Summary (개요)

### 제품 개요

**데이트 딸깍**은 Z세대 연인들이 겪는 "데이트 계획의 고통"을 해결하는 AI 기반 데이트 코스 추천 서비스입니다. 사용자가 `지역`, `데이트 유형`, `예산` 3가지만 입력하면, Claude AI가 최적화된 데이트 코스 3가지를 60초 이내에 추천합니다.

### 핵심 가치 제안 (Value Proposition)

```
Before: 데이트 준비 3시간 + 불안감 + 예산 초과
After: 60초 완성 + 확신 + 예산 내 보장

- 리서치 시간: 3.5시간 → 60초 (210배 단축)
- 의사결정 스트레스: 高 → 低
- 예산 관리: 불확실 → 자동 계산

```

### MVP 목표

- **플랫폼:** 웹 우선 (Next.js + TypeScript, 모바일 브라우저 반응형 지원). 네이티브 앱은 PMF 검증 이후 검토
- **런칭 시점:** 2025년 11월 15일 (D-33일)
- **타겟 MAU:** 10,000명 (첫 3개월)
- **PMF 검증 기준:** D7 Retention 40%+, NPS 40+

---

## 🎯 Problem Statement (해결하려는 문제)

### Problem 1: 리서치 시간 폭탄

```
현상: 데이트 계획에 평균 3.5시간 소요
원인: 블로그/인스타 무한 스크롤 → 정보 과부하 → 선택 마비
영향: 다음날 업무 집중력 30% 하락, 관계 스트레스 증가

```

### Problem 2: 예산 관리 실패

```
현상: 예산 5만원 생각 → 실제 지출 10만원
원인: 각 장소 가격 합산 안 함, 숨은 비용 간과
영향: 월말 카드값 폭탄, 데이트 빈도 감소

```

### Problem 3: 동선 비효율

```
현상: 강남 식당 → 홍대 카페 → 왕복 2시간
원인: 각 장소를 개별적으로 선택, 통합 계획 부재
영향: 이동 시간에 데이트 시간 소진, 피로도 증가

```

**→ 해결책: 60초 만에 예산 내에서 동선 최적화된 코스 자동 생성**

---

## 📊 Goals & Success Metrics (목표와 성공 지표)

### North Star Metric

**"실제로 추천 코스로 데이트 다녀온 사용자 수"**

- 목표: 월 7,000명 (MAU 10,000명 × 70% 실행률)

### HEART Framework Metrics

| Category | Metric | Target | 측정 방법 |
| --- | --- | --- | --- |
| **Happiness** | NPS | 40+ | 데이트 후 48시간 내 설문 |
|  | Post-Date Satisfaction | 4.2/5.0 | 5점 척도 만족도 |
| **Engagement** | DAU/MAU | 25%+ | 일간/월간 활성 사용자 비율 |
|  | Course Save Rate | 40%+ | 저장 수 / 조회 수 |
| **Adoption** | Onboarding Completion | 50%+ | 최초 접속 → 첫 코스 생성 (비로그인 포함) |
|  | Login Conversion | 20%+ (가설, 베타 후 조정) | 코스를 1회 이상 생성한 사용자 중 로그인한 비율 |
|  | Feature Discovery (D7) | 60%+ | 7일 내 2개 이상 기능 사용 |
| **Retention** | D7 Retention | 40%+ | 최초 방문 후 7일차 재방문율 (user_id/guest_id 기준) |
|  | D30 Retention | 25%+ | 최초 방문 후 30일차 재방문율 (user_id/guest_id 기준) |
| **Task Success** | Time-to-First-Course | <60초 | 사이트 접속 → 첫 코스 완성 |
|  | Recommendation-to-Execution | 70%+ | 저장 → 실제 방문 |

---

## 👥 User Personas (타겟 사용자)

### Persona 1: "완벽주의 계획러" 김민준

```
나이: 27세
직업: IT 스타트업 PM
연봉: 5,500만원
연애: 6개월차

Pain Points:
- 데이트 계획에 매주 3.5시간 소요
- "이게 진짜 좋은 곳일까?" 불안감
- 동선/예산 통합 계획의 어려움

Value Delivered:
- 시간 210배 단축 (3.5시간 → 60초)
- AI 기반 확신 제공
- 예산/동선 자동 최적화

```

### Persona 2: "오늘뭐하지형" 박서연

```
나이: 24세
직업: 패션 브랜드 마케터
연봉: 3,800만원
연애: 소개팅 활발, 3개월 내 짧은 연애

Pain Points:
- "오늘 뭐해?" 메시지 공포
- 결정 장애 + FOMO
- 예산 관리 실패 (예상 5만원 → 실제 10만원)

Value Delivered:
- 5초 의사결정 (즉흥성 유지)
- "AI가 골라줬으니 후회 없어"
- 예산 내 보장 (입력값 절대 초과 안 함)

```

---

## 📱 User Stories & Use Cases

### Epic 1: 코스 추천 받기 (Core Flow)

### User Story 1.1: 빠른 코스 생성

```
As a 사용자
I want to 지역/유형/예산만 입력하면 코스가 자동 생성되길
So that 고민 없이 빠르게 데이트 계획을 세울 수 있다

Acceptance Criteria:
✅ 입력 필드 3개: 지역(지도 기반 비주얼 선택), 유형(카드 선택), 예산(슬라이더)
✅ 데스크톱/모바일 브라우저 모두 지원 (반응형, 모바일 우선 설계)
✅ 입력 완료 후 60초 이내 추천 완료
✅ 추천 코스 3개 제시 (각각 3~4곳 장소 포함)
✅ 총 예산이 입력값의 95~100% 범위 내

```

**Use Case:**

```
1. [사용자] 웹사이트 접속
2. [시스템] 온보딩 화면 표시 (최초 1회만)
3. [사용자] "코스 만들기" 버튼 클릭
4. [사용자] 지역 선택: 서울 지도 기반 UI에서 "홍대" 영역 클릭/터치 (비주얼 맵 선택)
   - 서울 주요 상권이 일러스트 지도 형태로 표시
   - 각 지역 영역을 클릭/터치하면 선택 (예: 홍대, 강남, 성수, 연남, 이태원 등)
5. [사용자] 유형 선택: "문화데이트" (카드 클릭/터치)
6. [사용자] 예산 입력: 100,000원 (슬라이더)
7. [사용자] "추천 받기" 버튼 클릭
8. [시스템] 로딩 인디케이터 표시 ("코스 생성 중... 20초")
9. [시스템] 3개 코스 카드 형태로 표시
10. [사용자] 마음에 드는 코스 선택 → 상세 보기

```

### User Story 1.2: 코스 상세 정보 확인

```
As a 사용자
I want to 추천받은 코스의 상세 정보를 보고 싶다
So that 실제로 갈지 판단할 수 있다

Acceptance Criteria:
✅ 각 장소별 정보: 이름, 주소, 예상 비용, 예상 소요 시간
✅ 전체 동선 지도 표시 (순서대로 연결)
✅ 총 예산 & 총 소요 시간 표시
✅ "카카오맵 열기" 버튼
✅ 각 장소별 "더보기" → 외부 링크 (카카오맵 장소 페이지)
✅ 각 장소별 블로그 후기 섹션: 최근 후기 최대 3개(제목 + 스니펫 + 원문 링크) + 후기 수/최근 작성일 + 신뢰도 배지
✅ 후기가 없는 장소는 "후기 수집 중"으로 표시 (후기 없이도 코스 노출 가능)

```

### User Story 1.3: 코스 저장 & 공유

```
As a 사용자
I want to 마음에 드는 코스를 저장하고 공유하고 싶다
So that 나중에 다시 보거나 친구/연인에게 보낼 수 있다

Acceptance Criteria:
✅ "저장" 버튼 → 내 코스 보관함에 저장
✅ "공유" 버튼 → 카카오톡 공유 / 링크 복사 / Web Share API (모바일)
✅ 코스별 공유 URL 제공 (Open Graph 메타태그로 미리보기 카드 노출)
✅ 인스타 스토리용 코스 요약 이미지 저장 기능
✅ 공유 이미지/링크: 코스 요약 + 서비스 바로가기 링크

```

---

### Epic 2: 피드백 & 개선

### User Story 2.1: 데이트 후 만족도 평가

```
As a 사용자
I want to 다녀온 데이트에 대해 피드백을 남기고 싶다
So that 더 나은 추천을 받을 수 있다

Acceptance Criteria:
✅ 저장 후 48시간 뒤 웹 푸시 알림(권한 허용 시) 또는 재방문 시 배너: "다녀오셨나요?"
✅ 간단한 평가: ⭐ 5점 척도
✅ 선택적 코멘트: "어떤 점이 좋았나요?" (텍스트)
✅ 각 장소별 개별 평가 가능

```

### User Story 2.2: 재추천 받기

```
As a 사용자
I want to 마음에 안 들면 다른 코스를 다시 추천받고 싶다
So that 만족할 때까지 여러 옵션을 볼 수 있다

Acceptance Criteria:
✅ "다른 코스 보기" 버튼 (무제한)
✅ 이전 코스와 중복 장소 최소화 (80% 이상 다른 장소)
✅ 재추천 속도: 10초 이내 (캐싱 활용)

```

---

### Epic 3: 마이페이지 & 히스토리

### User Story 3.1: 저장한 코스 관리

```
As a 사용자
I want to 저장한 코스 목록을 보고 싶다
So that 과거 계획을 다시 활용할 수 있다

Acceptance Criteria:
✅ "내 코스" 탭에 저장 목록 표시
✅ 최신순 정렬, 날짜별 필터
✅ 각 코스 카드: 썸네일, 제목, 예산, 날짜
✅ 삭제 버튼 (확인 후 삭제, 모바일은 스와이프 보조 지원)

```

---

## 🛠️ Functional Requirements (기능 요구사항)

### FR-1: 사용자 인증 (Authentication)

### FR-1.1: 소셜 로그인

```
요구사항:
- 카카오톡 간편 로그인 단일 지원 (유일한 인증 수단)
- 네이버/애플/구글 로그인, 자체 회원가입(이메일·비밀번호), 별도 본인인증(PASS/NICE) 없음
- 로그인은 선택: 메인 기능은 비로그인으로 모두 사용 가능 (FR-1.3 참조)
- 로그인 UI: 로그인이 필요한 순간에만 모달 팝업으로 노출 (별도 로그인 페이지로 이동하지 않음)

기술 스펙:
- OAuth 2.0 표준
- JWT 토큰 기반 세션 관리
- Refresh Token: 30일
- Access Token: 1시간

API Endpoint:
POST /api/v1/auth/kakao
Request:
{
  "authorization_code": "string"
}
Response:
{
  "access_token": "string",
  "refresh_token": "string",
  "user_id": "string",
  "is_new_user": boolean
}

게스트 데이터 이관:
- 비로그인 사용자는 서버가 발급한 guest_id (HttpOnly 쿠키)로 식별
- 로그인 요청 시 guest_id 쿠키가 함께 전달되면, 해당 guest_id의 코스 생성 이력을
  user_id로 귀속 (로그인 직전까지 만든 코스를 잃지 않음)

```

### FR-1.2: 만 19세 이상 확인 (주류 장소 노출)

```
요구사항:
- 별도 본인인증(PASS/NICE) 없이 청소년보호법 취지에 맞게 주류 중심 장소(술집/바) 노출을 제어
- places.is_alcohol = true 장소는 만 19세 이상 확인 후에만 추천 후보에 포함

플로우:
[비로그인]
1. 기본: 주류 장소 제외하고 코스 생성 (감성데이트의 루프탑 카페 등 비주류 장소 위주)
2. 코스 생성 화면에서 "만 19세 이상입니다" 자가 확인 체크 시 주류 장소 포함
   (확인 값은 guest_id 세션에 저장)
[로그인]
1. 카카오 동의항목(연령대 또는 출생연도)으로 만 19세 이상 여부 확인
2. 확인 결과를 users.is_adult에 저장 → 이후 자동으로 주류 장소 포함
3. 만 19세 미만으로 확인되면 주류 장소는 항상 제외 (가입 차단은 하지 않음)

사전 확인 필요:
- 카카오 연령대/출생연도 동의항목은 비즈 앱 전환 및 동의항목 검수가 필요할 수 있음
- 비로그인 자가 확인 방식의 적정성은 법무 검토 필요 (불충분하면 주류 장소는 로그인 사용자에게만 노출)

```

### FR-1.3: 비로그인 사용 정책 (Guest-first)

```
원칙:
- 메인 플로우(코스 생성 → 상세 → 재추천 → 지도 → 공유)는 비로그인으로 끝까지 체험 가능
- 로그인은 "내 데이터가 필요한 개인화 기능"을 쓰려는 순간에만 요청

비로그인 가능:
- 지역/유형/예산 입력 및 코스 3개 생성, "다른 코스 보기" (재추천)
- 코스 상세 (장소 정보, 동선 지도, 총 예산/시간, 카카오맵 열기, 블로그 후기)
- 코스 공유 (카카오톡 공유, 링크 복사, 요약 이미지 저장)
- 공유받은 코스 링크 열람
- 최근 본 코스 (브라우저 localStorage에 최대 5개, 서버 저장 아님)

로그인 필요 (개인화 기능, 해당 액션 시점에 로그인 모달 노출):
- 코스 저장 / 내 코스 보관함 (FR-3)
- 데이트 후 피드백·평점 작성 (FR-4)
- 방문 후 알림 (웹 푸시/배너) 수신
- 선호 지역·취향 기반 맞춤 추천 (Phase 2)

로그인 모달 UX:
1. 로그인 필요 액션(예: "저장" 클릭) → 현재 화면 위에 모달 표시 ("카카오로 3초 만에 시작하기")
2. 모달에는 로그인으로 얻는 혜택 한 줄 안내 (예: "이 코스를 저장하고 다녀온 뒤 후기를 남길 수 있어요")
3. 카카오 인증 완료 후 원래 화면으로 복귀하고, 하려던 액션(저장 등)을 자동 실행
4. 모달을 닫으면 아무 손실 없이 비로그인 상태로 계속 이용 (강제 로그인·반복 팝업 없음)
5. "내 코스" 탭은 비로그인 시 빈 상태 + 로그인 안내를 보여줌

API 접근 정책:
- 공개 (인증 불필요): POST /api/v1/courses/generate, GET /api/v1/courses/{course_id},
  GET /api/v1/places/{place_id}/reviews
- 인증 필요 (미인증 시 401 → 프론트엔드가 로그인 모달 노출):
  POST /api/v1/courses/save, GET /api/v1/courses/saved, POST /api/v1/feedback

비용·남용 방어 (비로그인 허용에 따른 필수 장치):
- 비로그인 코스 생성: guest_id + IP 기준 Rate Limit (초기값 시간당 10회), 로그인 사용자는 완화 (시간당 30회)
- 동일 조건은 Redis 캐시로 응답 → Claude API 호출 최소화
- 한도 초과 시: "잠시 후 다시 시도해주세요" 또는 로그인 시 한도 확대 안내 (한도 초과 상황에 한해 로그인 유도)
- 봇 방어: 필요 시 Cloudflare Turnstile 등 도입 검토

```

---

### FR-2: 코스 추천 엔진 (Recommendation Engine)

### FR-2.1: 사용자 입력 처리

```
Input Parameters:
1. region: string (예: "홍대", "강남", "성수")
   - 드롭다운 선택
   - 초기 지원 지역: 서울 5대 상권 (홍대/강남/성수/연남/이태원)

2. date_type: enum
   - "문화데이트" (전시, 뮤지엄, 카페)
   - "맛집투어" (식당, 디저트)
   - "액티비티" (방탈출, 볼링, 스크린골프)
   - "감성데이트" (루프탑, 와인바, 야경)

3. budget: integer (단위: 원)
   - 슬라이더 UI: 30,000원 ~ 300,000원
   - 기본값: 100,000원
   - 간격: 10,000원

Validation:
- region: NOT NULL, 지원 지역 리스트 내
- date_type: NOT NULL, enum 값만 허용
- budget: 30,000 <= budget <= 300,000

```

### FR-2.2: AI 추천 로직

```
Process Flow:
1. [Cache Check] Redis에서 동일 조건 캐시 확인
   - Cache Key: MD5(region + date_type + budget)
   - Cache Hit → 즉시 반환 (응답시간 0.5초)

2. [Cache Miss] DB에서 장소 후보 조회
   - SQL:
     SELECT * FROM places
     WHERE region = ?
     AND date_type LIKE %?%
     AND price_range <= ?
     AND is_active = true
     LIMIT 50

3. [AI Call] Claude Sonnet 4.5 API 호출
   - Prompt Template:
     """
     당신은 데이트 코스 큐레이터입니다.

     조건:
     - 지역: {region}
     - 데이트 유형: {date_type}
     - 예산: {budget}원

     사용 가능한 장소 목록:
     {places_json}

     요구사항:
     1. 3개의 코스를 추천하세요 (각 코스는 3~4곳)
     2. 총 예산은 {budget}원의 95~100% 범위
     3. 동선을 최적화하세요 (이동 시간 최소화)
     4. 각 코스는 최소 50% 이상 다른 장소로 구성

     응답 형식 (JSON):
     {
       "courses": [
         {
           "title": "코스 제목",
           "places": [
             {
               "place_id": "id",
               "name": "장소명",
               "estimated_cost": 35000,
               "estimated_time": "1시간"
             }
           ],
           "total_cost": 95000,
           "total_time": "4시간"
         }
       ]
     }
     """

4. [Validation] AI 응답 검증
   - JSON 파싱 가능한가?
   - courses 배열 길이 = 3
   - 각 코스의 total_cost <= budget
   - place_id가 DB에 존재하는가?

5. [Cache Save] Redis에 결과 저장 (TTL: 24시간)

6. [Response] 클라이언트에 반환

```

**API Endpoint:**

```
POST /api/v1/courses/generate
Request:
{
  "region": "홍대",
  "date_type": "문화데이트",
  "budget": 100000
}

Response:
{
  "request_id": "uuid",
  "generated_at": "2025-10-13T14:30:00Z",
  "courses": [
    {
      "course_id": "c1",
      "title": "홍대 감성 문화 코스",
      "places": [
        {
          "place_id": "p123",
          "name": "서울숲 갤러리",
          "category": "전시",
          "address": "서울 성동구...",
          "estimated_cost": 15000,
          "estimated_time": "1.5시간",
          "kakao_place_url": "https://place.map.kakao.com/...",
          "trust_score": 0.82,
          "review_summary": {
            "review_count_30d": 12,
            "last_reviewed_at": "2025-10-10",
            "reviews": [
              {
                "title": "서울숲 갤러리 후기",
                "snippet": "전시 분위기가 좋고...",
                "link": "https://blog.naver.com/...",
                "source": "naver_blog",
                "posted_at": "2025-10-10"
              }
            ]
          }
        },
        {
          "place_id": "p456",
          "name": "카페 온더그라운드",
          "category": "카페",
          "estimated_cost": 18000,
          "estimated_time": "1시간"
        },
        {
          "place_id": "p789",
          "name": "이태원 루프탑 바",
          "category": "바",
          "estimated_cost": 62000,
          "estimated_time": "2시간"
        }
      ],
      "total_cost": 95000,
      "total_time": "4.5시간",
      "route_map_url": "https://map.kakao.com/..."
    },
    // ... 2개 더
  ]
}

```

### FR-2.3: 성능 요구사항

```
SLA:
- P50 (중앙값): 3초 이내
- P95: 8초 이내
- P99: 15초 이내
- Timeout: 30초 (그 이후 Fallback)

Fallback Strategy:
1. Claude API 타임아웃 → 룰 베이스 추천
2. 룰 베이스 예시:
   - 홍대 + 문화데이트 + 10만원 → 미리 정의된 템플릿 3개 반환
   - DB에 저장된 "인기 코스 TOP 10" 활용

Circuit Breaker:
- Failure Rate > 50% → Circuit Open (30초간 Fallback만 사용)
- 30초 후 Half-Open (1개 요청 테스트) → 성공 시 Closed

```

---

### FR-3: 코스 저장 & 관리

### FR-3.1: 코스 저장

```
API Endpoint:
POST /api/v1/courses/save
Request:
{
  "course_id": "c1",
  "request_id": "uuid"  // 원본 생성 요청 ID
}

Response:
{
  "saved_course_id": "sc123",
  "saved_at": "2025-10-13T14:35:00Z"
}

인증: 필요 (카카오 로그인). 비로그인 상태에서 호출되면 401 반환 → 프론트엔드가 로그인 모달 후 요청 자동 재시도

Database Schema:
Table: saved_courses
- id (PK)
- user_id (FK)
- course_id
- saved_at
- is_deleted (soft delete)
- visited_at (nullable, 실제 방문 날짜)

```

### FR-3.2: 코스 목록 조회

```
API Endpoint:
GET /api/v1/courses/saved?page=1&size=20&sort=latest

Response:
{
  "courses": [
    {
      "saved_course_id": "sc123",
      "course_title": "홍대 감성 문화 코스",
      "saved_at": "2025-10-13T14:35:00Z",
      "total_cost": 95000,
      "region": "홍대",
      "thumbnail_url": "https://..."
    }
  ],
  "pagination": {
    "current_page": 1,
    "total_pages": 5,
    "total_count": 87
  }
}

```

---

### FR-4: 피드백 시스템

### FR-4.1: 만족도 평가

```
API Endpoint:
POST /api/v1/feedback
Request:
{
  "saved_course_id": "sc123",
  "overall_rating": 5,  // 1~5
  "visited": true,
  "place_feedbacks": [
    {
      "place_id": "p123",
      "still_open": true,
      "actual_cost": 17000,
      "rating": 5
    }
  ],
  "comment": "최고였어요!" (optional)
}

Response:
{
  "feedback_id": "f123",
  "created_at": "2025-10-15T20:00:00Z"
}

Trigger:
- 저장 후 48시간 뒤 푸시 알림 발송
- 웹 푸시(Web Push) 또는 재방문 시 인앱 배너로 노출
- 알림 제목: "데이트 다녀오셨나요? 😊"
- 알림 내용: "5초만 평가해주시면 더 좋은 추천을 드릴게요!"

```

### FR-4.2: 데이터 활용

```
피드백 → DB 자동 업데이트:
1. still_open = false (2건 이상) → places.is_active = false
2. actual_cost 평균 → places.price_range 업데이트
3. rating 평균 → places.user_rating 업데이트

AI 프롬프트 개선:
- 평점 4.5 이상 장소 → "강력 추천" 가중치 상승
- 평점 3.0 이하 장소 → 제외 처리

```

---

### FR-5: 장소 데이터 수집 & 후기 (Data Pipeline)

### FR-5.1: 장소 데이터 소스 (네이버 플레이스 대체)

```
1순위: 카카오 Local API (공식, 무료 쿼터 내 사용)
- 키워드/카테고리 검색으로 상권별 후보 장소 수집
- 확보 가능: 장소명, 주소, 좌표, 카테고리, 전화번호, 카카오맵 URL
- 확보 불가: 가격, 평점, 후기, 사진, 영업시간 상세
  → 가격/소요시간: 수작업 큐레이션 + 사용자 피드백(actual_cost)으로 보정

2순위(보조): 네이버 검색 API - 지역(Local) 검색 (공식, 무료)
- 카카오로 수집한 장소가 실제 존재하는지 교차 검증하는 용도 (이름·주소 매칭)
- 가격, 평점, 후기는 제공되지 않으므로 데이터 보강 목적은 아님

원칙: 공식 API만 사용
- 크롤링/스크래핑(카카오맵·네이버 플레이스·인스타그램 등)은 하지 않음
- Instagram Graph API는 본인 소유 계정만 접근 가능하여 장소 검색·후기 수집에 사용할 수 없음
- 공식 API로 얻을 수 없는 가격·소요시간·영업시간은 수작업 큐레이션 + 사용자 피드백으로 채움

수집 주기:
- 초기: 서울 5대 상권 × 100곳 (카카오 API 수집 + 큐레이션 보완)
- 이후: 주 1회 배치로 폐업/이전 여부 갱신 (Spring Batch 또는 ECS Scheduled Task)
- 피드백(still_open=false)과 함께 폐업 판단에 활용
```

### FR-5.2: 블로그 후기 연동 (공식 검색 API)

```
방식: 네이버 검색 API(블로그) + 카카오 다음 검색 API(블로그) - 둘 다 공식·무료
- 검색어: "{장소명} {지역}" (+ "후기")
- 응답에서 얻는 값: 글 제목, 요약 스니펫, 원문 링크, 작성일, 블로거명
- 노출 방식: 제목 + 짧은 스니펫 + "원문 보기" 링크 (원문은 새 탭에서 이동)
  → 본문 전체를 저장·재게시하지 않음, 출처(블로그명/플랫폼) 표기
- 스니펫의 HTML 태그(<b> 등)는 제거(sanitize) 후 표시

운영 방식:
- 장소별 최초 조회 시 API 호출 후 DB 캐싱 (TTL 7일), 주 1회 배치로 갱신
- 동일 장소명(체인점 등) 오매칭 방지: 제목/스니펫에 장소명 + 지역명이 모두 포함된 글만 채택
  (match_confidence로 필터링)
- 일일 호출 쿼터(네이버 검색 API 일 25,000회 수준, 카카오 쿼터 별도) 확인 후 배치 분산
- 신고 접수 시 is_hidden 처리

한계:
- 블로그 후기 위주이며 인스타그램·네이버 플레이스 리뷰는 제공하지 않음
- 후기가 없는 장소는 "후기 수집 중"으로 표시하고 코스 노출은 그대로 유지
- 후기 본문을 AI로 요약하는 기능은 MVP 제외 (스니펫 수준 표시만, 이용약관/저작권 검토 후 Phase 2)
```

### FR-5.3: 데이터 신뢰도 (Trust Score)

```
목적: 폐업/정보 오류 장소 추천을 줄이고, 사용자에게 신뢰 근거 제공

산출 입력:
- 최근 30일 블로그 후기 수, 가장 최근 후기 작성일 (영업 중 신호)
- 카카오·네이버 두 API에서 모두 확인되는 장소인지 (교차 검증)
- 사용자 피드백 평점/still_open 비율
- 카카오 API 최신 갱신 여부
- 수작업 큐레이션 검증 여부

활용:
- AI 프롬프트의 places_json에 trust_score 포함 → 높은 장소 우선 선택
- trust_score < 0.3 장소는 후보에서 제외
- UI에 "최근 후기 N건", "최근 후기 N일 전" 배지 표시
```

---

## 🔧 Non-Functional Requirements (비기능 요구사항)

### NFR-1: 성능 (Performance)

```
요구사항:
- 코스 생성 API: P95 < 8초
- 일반 API: P95 < 500ms
- 웹 초기 로딩: LCP 2.5초 이내 (모바일 4G 기준)
- 이미지 로딩: next/image 기반 Lazy Loading + WebP/AVIF 포맷
- Core Web Vitals: LCP < 2.5s, INP < 200ms, CLS < 0.1

측정 도구:
- Backend: Spring Boot Actuator + CloudWatch (Container Insights, ALB/ECS/RDS/ElastiCache 메트릭) + Sentry (Java SDK, 예외/트레이싱)
- Frontend: Lighthouse CI + Web Vitals + Sentry (@sentry/nextjs, 에러/성능/세션 추적)
- Monitoring: AWS CloudWatch (인프라/로그/알람) + Sentry (애플리케이션 에러/성능)

```

### NFR-2: 확장성 (Scalability)

```
목표:
- 동시 사용자: 500명 (피크 타임)
- 월간 요청: 100만 건 (성장 시)

Architecture:
- Stateless API 서버 → 수평 확장 가능
- Redis 캐싱 → DB 부하 감소
- CDN (CloudFront) → 정적 파일 분산

Load Testing:
- nGrinder: 500 concurrent users, 10분 지속
- 목표: Error Rate < 1%, Response Time P95 < 10초

```

### NFR-3: 가용성 (Availability)

```
SLA: 99.5% uptime (월 3.6시간 다운타임 허용)

장애 대응:
- Health Check: ALB Target Group + ECS 컨테이너 헬스체크 (/actuator/health, 30초마다)
- Auto Restart: 헬스체크 3회 연속 실패 시 ECS가 태스크 교체 (Deployment Circuit Breaker로 배포 실패 시 자동 롤백)
- Circuit Breaker: Claude API 장애 시 Fallback
- Runbook: 주요 장애 대응 매뉴얼 문서화

Monitoring:
- CloudWatch Alarm (Terraform으로 관리): ALB 5xx 비율 > 5%, ECS CPU/메모리 > 80%, 헬스체크 실패, RDS 연결/CPU 임계치 → SNS → Slack 알람
- Sentry Alert: 신규 이슈 발생, 에러 급증(Spike), 코스 생성 API 실패율 증가 → Slack 알람
- Critical 장애: SNS를 통한 온콜 담당자 알림 (SMS/이메일)
- Sentry Release 연동: GitHub Actions 배포 시 릴리스/소스맵 업로드 (배포별 에러 추적)
- 로그: CloudWatch Logs (구조화 JSON 로그, 보관 기간 3개월)

LLM Observability (Langfuse):
- 코스 생성 요청 1건 = 1 Trace (캐시 조회 → DB 후보 조회 → Claude 호출 → 검증 → 응답)
- 추적 항목: 입력 토큰/출력 토큰, 비용, 지연시간(P50/P95), 모델명, 프롬프트 버전
- 품질 지표: JSON 파싱 실패율, 검증 실패율(예산 초과/존재하지 않는 place_id), Fallback 전환율, 캐시 히트율
- 사용자 피드백(평점)을 Langfuse Score로 연결 → 프롬프트 버전별 품질 비교
- 개인정보(이름, 전화번호 등)는 Trace에 포함하지 않음 (지역/유형/예산만 기록)
- 운영: Langfuse Cloud 또는 ECS 자체 호스팅 (Terraform 관리) 중 선택 필요
- 알람: 일일 LLM 비용 임계치 초과, 검증 실패율 급증 시 Slack 알림 (PagerDuty 등 온콜 도구는 사용하지 않음)

```

### NFR-4: 보안 (Security)

```
요구사항:
1. HTTPS 필수 (Let's Encrypt SSL)
2. API Rate Limiting: 일반 API 10 req/min, 코스 생성은 비로그인 시간당 10회 / 로그인 시간당 30회 (guest_id + IP 기준, FR-1.3)
3. SQL Injection 방지: PreparedStatement 사용
4. XSS 방지: 사용자 입력 sanitization
5. 개인정보 최소 수집: 카카오에서 받는 항목만 저장 (전화번호·생년월일 미수집). 위치 정보 사용 시 AES-256 암호화
6. guest_id 쿠키: HttpOnly, Secure, SameSite=Lax

데이터 보관:
- 데이트 기록: 1년 후 자동 삭제
- 피드백: 익명화 후 영구 보관
- 접근 로그: 3개월 보관

```

### NFR-5: 법적 준수 (Compliance)

```
필수 사항:
1. 개인정보보호법
   - 개인정보처리방침 명시
   - 위치정보 수집 동의 (명시적)
   - 마케팅 수신 동의 (선택적)

2. 청소년보호법
   - 만 19세 미만에게 주류 장소 비노출 (FR-1.2)
   - 별도 본인인증 없이 카카오 연령 정보 또는 자가 확인 사용 (법무 검토 필요)

3. 이용약관
   - 서비스 제공 범위 명시
   - 면책 조항: "추천 장소의 영업/품질 보증하지 않음"
   - 환불 정책 (프리미엄)

개인정보 수집 항목:
- 필수(로그인 시): 카카오 회원번호, 닉네임
- 선택(로그인 시): 연령대/출생연도(만 19세 확인용), 프로필 사진, 선호 지역
- 비로그인: 개인정보 수집 없음 (익명 guest_id, 코스 생성 조건만 기록)
- 자동 수집: 서비스 이용 기록, 접속 로그

```

---

## 🚫 Out of Scope (MVP 범위 밖)

### Phase 2 이후 개발 (MVP에 포함 안 함)

```
❌ 실시간 채팅 (연인 간)
❌ 커뮤니티 기능 (후기 게시판)
❌ 영상 콘텐츠 (데이트 Vlog)
❌ 프리미엄 구독 (MVP는 전체 무료)
❌ 식당 예약 연동 (추후 B2B 제휴 시)
❌ 데이트 일정 캘린더
❌ 커플 앱 연동 (Between, 하루)
❌ 지방 광역시 (부산/대구) - MVP는 서울만
❌ 외국어 지원 (영어/일본어) - MVP는 한국어만

```

### 의도적 제외 사유

```
이유 1: 빠른 PMF 검증 집중
- 코어 기능(코스 추천)만 완벽하게 구현
- 부가 기능은 PMF 달성 후 추가

이유 2: 리소스 제약
- 개발 기간: 30일
- 팀 구성: 개발 3명, 디자인 1명, PM 1명
- 예산: 인프라 월 500만원 이내

이유 3: 기술적 복잡도 최소화
- Monolith 구조 유지
- 외부 연동 최소화 (Claude API만)

```

---

## 🏗️ Technical Architecture (기술 아키텍처)

### Tech Stack

```
Frontend (웹 우선):
- Framework: Next.js (App Router) + TypeScript
- State Management: Zustand (클라이언트 상태)
- Data Fetching: TanStack Query (서버 상태/캐싱)
- Routing: Next.js 파일 기반 라우팅 (React Navigation 대체)
- HTTP Client: fetch (필요 시 Axios)
- Design: Tailwind CSS
- 렌더링: 랜딩/공유 페이지는 SSR/SSG (SEO + OG 메타), 코스 생성/보관함은 CSR 중심
- 배포: Docker 컨테이너 → ECR → ECS Fargate 서비스 (Next.js `standalone` 출력 사용)
- 추후: 네이티브 앱은 PMF 검증 이후 별도 검토 (API는 클라이언트 비종속으로 설계)

Backend:
- Framework: Spring Boot 3.2 (Java 17)
- ORM: Spring Data JPA + QueryDSL
- Database: PostgreSQL 15
- Cache: Redis 7
- AI: Anthropic Claude Sonnet 4.5 API
- Monitoring: Spring Boot Actuator + AWS CloudWatch + Sentry

Infrastructure:
- Cloud: AWS
- Compute: ECS on Fargate
  - Service 1: frontend (Next.js SSR) - 태스크 2개 이상
  - Service 2: backend (Spring Boot) - 태스크 2개 이상
  - Application Auto Scaling (CPU/요청 수 기반)
- Container Registry: ECR (frontend / backend 리포지토리)
- Database: RDS PostgreSQL (db.t3.medium)
- Cache: ElastiCache Redis (cache.t3.micro)
- CDN: CloudFront
- Load Balancer: ALB (경로 기반 라우팅: /api/* → backend, 그 외 → frontend)
- Secrets: AWS Secrets Manager / SSM Parameter Store (Claude API 키, DB 비밀번호 등)
- Logs/Metrics/Alarms: CloudWatch (Logs, Container Insights, Alarms, SNS)
- Error Tracking/APM: Sentry (frontend + backend)
- LLM Observability: Langfuse (Claude 호출 트레이싱, 토큰/비용/지연시간, 프롬프트 버전 관리)
- 장소 데이터: 카카오 Local API(주) + 네이버 지역 검색 API(교차 검증) + 수작업 큐레이션
- 후기: 네이버 블로그 검색 API + 카카오(다음) 블로그 검색 API (공식 API만 사용, 크롤링 없음)
- IaC: Terraform
  - 모든 AWS 리소스를 Terraform으로 관리 (콘솔 수동 변경 금지)
  - Remote State: S3 백엔드 + 상태 잠금 (DynamoDB)
  - 구조: modules/ (network, ecr, ecs, rds, observability) + envs/prod (prod 단일 환경, dev 환경 없음)
  - terraform apply는 CI에서 자동화하지 않고 담당자가 로컬에서 직접 실행
- CI/CD: GitHub Actions
  - 앱: Docker 빌드 → ECR push → ECS 서비스 업데이트 (롤링 배포, prod)
  - 인프라(Terraform)는 CI/CD 대상이 아님

```

<aside>
🔄 **2026.10 변경:** Claude Sonnet 4.5 는 2026년 11월 30일에 API 지원이 끝나니 PRD와 코드의 모델은 `claude-sonnet-5-5` 로 지정하세요. 이 모델은 `temperature`·`top_p`·`top_k` 를 기본값이 아닌 값으로 보내면 400 오류가 나므로 코드에서 해당 인자를 지우세요. Spring Boot 3.2 도 지금은 Spring Initializr 에서 고를 수 없으니 4.x 로 적으세요.

</aside>

### System Architecture Diagram

```
[Client (Web Browser - Desktop/Mobile)]
         ↓
[CloudFront CDN] ← 정적 파일 (이미지, JS)
         ↓
[Application Load Balancer]
         ↓
[ECS Fargate - Next.js (SSR) 서비스]  →  API 호출 (/api/*는 ALB가 직접 라우팅)
         ↓
[ECS Fargate - Spring Boot 서비스 × 2+ 태스크]
    ↙       ↓       ↘
[Redis]  [RDS]  [Claude API]
(캐싱)   (메인DB)  (AI 추천)

```

### Database Schema (핵심 테이블)

```sql
-- 사용자
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    kakao_id VARCHAR(100) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    age_range VARCHAR(20),        -- 카카오 연령대 (선택 동의)
    is_adult BOOLEAN,             -- NULL: 미확인, 카카오 연령 정보로 판정
    created_at TIMESTAMP DEFAULT NOW(),
    last_login_at TIMESTAMP
);

-- 장소
CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    region VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    address TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    price_range INTEGER NOT NULL,  -- 1인당 평균 가격
    estimated_time VARCHAR(20),    -- "1~2시간"
    kakao_place_id VARCHAR(50) UNIQUE,  -- 카카오 Local API 장소 ID
    kakao_place_url TEXT,
    trust_score DECIMAL(3, 2),     -- 0.00~1.00, 신뢰도 (FR-5.3)
    data_source VARCHAR(20) NOT NULL DEFAULT 'kakao',  -- kakao / curated
    is_alcohol BOOLEAN DEFAULT FALSE,  -- 주류 중심 장소 (만 19세 확인 필요)
    user_rating DECIMAL(2, 1),     -- 1.0~5.0
    is_active BOOLEAN DEFAULT TRUE,
    last_updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_region_category (region, category),
    INDEX idx_active_rating (is_active, user_rating DESC)
);

-- 장소 관련 블로그 후기 (제목/스니펫/링크만 저장, 본문 저장 금지)
CREATE TABLE place_reviews (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT REFERENCES places(id),
    source VARCHAR(20) NOT NULL,   -- naver_blog / daum_blog
    title TEXT NOT NULL,
    snippet TEXT,
    link TEXT NOT NULL UNIQUE,
    blogger_name VARCHAR(100),
    posted_at DATE,
    match_confidence DECIMAL(3, 2),-- 장소 매칭 신뢰도
    is_hidden BOOLEAN DEFAULT FALSE,  -- 신고 시 숨김
    fetched_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_place_posted (place_id, posted_at DESC)
);

-- 코스 생성 요청
CREATE TABLE course_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT REFERENCES users(id),  -- 비로그인이면 NULL, 로그인 시 guest_id 기준으로 귀속
    guest_id VARCHAR(64),                 -- 비로그인 식별자 (익명)
    region VARCHAR(50) NOT NULL,
    date_type VARCHAR(50) NOT NULL,
    budget INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    response_time_ms INTEGER,  -- 성능 측정용

    INDEX idx_user_created (user_id, created_at DESC)
);

-- 생성된 코스
CREATE TABLE courses (
    id VARCHAR(50) PRIMARY KEY,
    request_id UUID REFERENCES course_requests(id),
    title VARCHAR(100) NOT NULL,
    total_cost INTEGER NOT NULL,
    total_time VARCHAR(20),
    places_json JSONB NOT NULL,  -- 장소 목록 (비정규화)
    created_at TIMESTAMP DEFAULT NOW()
);

-- 저장된 코스
CREATE TABLE saved_courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    course_id VARCHAR(50) REFERENCES courses(id),
    saved_at TIMESTAMP DEFAULT NOW(),
    visited_at TIMESTAMP,  -- 실제 방문 날짜
    is_deleted BOOLEAN DEFAULT FALSE,

    INDEX idx_user_saved (user_id, saved_at DESC)
);

-- 피드백
CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    saved_course_id BIGINT REFERENCES saved_courses(id),
    overall_rating INTEGER CHECK (overall_rating BETWEEN 1 AND 5),
    visited BOOLEAN NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 장소별 피드백
CREATE TABLE place_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT REFERENCES feedbacks(id),
    place_id BIGINT REFERENCES places(id),
    still_open BOOLEAN NOT NULL,
    actual_cost INTEGER,
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    visit_date DATE NOT NULL,

    INDEX idx_place_visit (place_id, visit_date DESC)
);

```

---

## 📅 Release Plan (출시 계획)

### Phase 0: 준비 (D-30 ~ D-15)

```
Week 1-2 (10/14 ~ 10/27):
✅ 기술 스택 셋업 (개발 환경 구축)
✅ DB 스키마 설계 & 마이그레이션
✅ Terraform 인프라 코드 작성 (VPC, ECS, ECR, RDS, Redis, ALB, CloudFront)
✅ CI/CD 파이프라인 구축 (GitHub Actions → ECR → ECS)
✅ 초기 장소 데이터 수집 (서울 5대 상권 × 100곳 = 500곳)
   - 데이터 소스: 카카오 Local API (무료) + 수작업 큐레이션 병행
   - 카카오/네이버 개발자 센터 앱 등록 및 API 키 발급 (Local, 블로그 검색)
✅ 법적 문서 준비 (이용약관, 개인정보처리방침)

```

### Phase 1: 코어 기능 개발 (D-14 ~ D-7)

```
Week 3 (10/28 ~ 11/3):
✅ 사용자 인증 (카카오 로그인 단일, 비로그인 게스트 세션, 로그인 모달 + 게스트 데이터 이관)
✅ 코스 추천 API (Claude 연동 + 캐싱)
✅ 코스 저장/조회 API
✅ 기본 UI/UX (홈, 코스 생성, 상세, 목록)

개발 담당:
- Backend: 김개발 (인증, API)
- Backend: 박서버 (AI 연동, 캐싱)
- Frontend: 최앱 (Next.js + TypeScript 웹 UI)

```

### Phase 2: 테스트 & 최적화 (D-6 ~ D-1)

```
Week 4 (11/4 ~ 11/10):
✅ 통합 테스트 (Postman + Jest)
✅ 부하 테스트 (nGrinder, 목표: 500 concurrent users)
✅ 성능 최적화 (쿼리 튜닝, 인덱스 추가)
✅ 보안 점검 (OWASP Top 10)
✅ QA (버그 수정, 엣지 케이스)

```

### Phase 3: 소프트 런칭 (D-Day ~ D+7)

```
Week 5 (11/11 ~ 11/17):
✅ 베타 테스터 모집 (50명)
   - 페르소나 1: 20명 (계획형)
   - 페르소나 2: 30명 (즉흥형)
✅ 피드백 수집 (버그 리포트, 개선 제안)
✅ 긴급 패치 (Critical 버그 우선 수정)
✅ 지표 모니터링 (Onboarding Completion, Time-to-First-Course)

목표:
- 에러 없는 세션 비율(Error-Free Session Rate, Sentry Release Health 기준) > 99%
- Onboarding Completion > 40%

```

### Phase 4: 공식 런칭 (D+8 ~)

```
Week 6+ (11/18 ~):
✅ 웹 서비스 정식 오픈 (도메인/SEO/OG 설정 완료)
✅ 마케팅 캠페인 (인스타 광고, 인플루언서)
✅ 사용자 피드백 기반 개선
✅ PMF 검증 (D7 Retention 40%, NPS 40 달성 여부)

Week 8 (12/2):
→ PMF 회의: 피봇 vs 스케일업 결정

```

---

## 📊 Success Criteria (MVP 성공 기준)

### 런칭 후 30일 내 달성 목표

```
✅ MUST HAVE (필수):
- MAU: 10,000명
- D7 Retention: 40%+
- NPS: 40+
- Error-Free Session Rate: 99%+
- Time-to-First-Course: <60초 (P95)

⚠️ SHOULD HAVE (중요):
- D30 Retention: 25%+
- Course Save Rate: 40%+
- Recommendation-to-Execution: 70%+
- Post-Date Satisfaction: 4.2/5.0

📊 NICE TO HAVE (추가):
- DAU/MAU: 25%+
- Organic Share Rate: 15%+

```

### PMF (Product-Market Fit) 판단 기준

```
✅ PMF 달성 = 다음 중 2가지 이상 충족:
1. D7 Retention 40%+ (3개월 연속)
2. NPS 40+ & Organic Growth 30%+
3. "Must Have" 설문: 40%+ 응답

→ PMF 달성 시: Series A 투자 유치 + 스케일업
→ PMF 미달 시: 피봇 or 개선 후 재도전

```

---

## 🔗 Appendix (부록)

### A. API 명세 전체 문서

```
Swagger/OpenAPI 문서: https://api.date-click.com/swagger-ui
Postman Collection: [첨부 파일 참조]

```

### B. 디자인 시스템

```
Figma 링크: https://figma.com/date-click-design
컬러 팔레트:
- Primary: #FF6B6B (코랄 핑크)
- Secondary: #4ECDC4 (민트)
- Background: #FFFBF0 (아이보리)
- Text: #2D3436 (다크 그레이)

```

### C. 개발 환경 셋업 가이드

```
GitHub: https://github.com/date-click/backend
README.md 참조

```

### D. 리스크 레지스터

```
위험 요소 | 대응 방안 | 담당자
-------|--------|-------
AI API 비용 초과 | 캐싱 + Rate Limiting | Backend 팀
데이터 품질 낮음 | 피드백 루프 + 수작업 큐레이션 + Trust Score | PM
카카오 API에 가격/평점 없음 | 큐레이션 + 피드백 기반 보정 | PM
검색 API 쿼터 제한/후기 부족 | DB 캐싱 + 배치 분산, 후기 없이도 노출되는 설계 | Backend 팀
후기 스니펫 이용 약관·저작권 | 공식 API만 사용, 원문 링크 연결, 본문 비저장, 법무 검토 | CEO
법적 문제 | 법무법인 검토 + 면책 조항 | CEO
개발 지연 | Agile 스프린트 + 매일 스탠드업 | PM

```

---

## ✅ Approval (승인)

| 역할 | 이름 | 서명 | 날짜 |
| --- | --- | --- | --- |
| Product Manager | [PM 이름] | _______ | 2025-10-13 |
| Engineering Lead | [Tech Lead 이름] | _______ | 2025-10-13 |
| Design Lead | [Design Lead 이름] | _______ | 2025-10-13 |
| CEO | [CEO 이름] | _______ | 2025-10-13 |

---

**Document Version History**

```
v1.0 (2025-10-13): 초안 작성
v1.0.1 (2026-10-13): 프론트엔드를 React Native 앱 → Next.js + TypeScript 웹(웹 우선)으로 변경
v1.0.2 (2026-10-13): 컴퓨팅을 EC2 → ECS Fargate로, 인프라 배포를 Terraform(IaC)으로 변경
v1.0.3 (2026-10-13): 모니터링을 DataDog/Prometheus/PagerDuty → AWS CloudWatch + Sentry로 변경
v1.0.7 (2026-10-14): 인프라를 prod 단일 환경으로 확정, terraform apply는 로컬 수동 실행(CI 자동화 제외)
v1.0.6 (2026-10-13): 인스타그램 연동 제거(Graph API는 본인 계정만 접근 가능), 크롤링 제외 → 카카오·네이버 공식 API(Local, 블로그 검색)만 사용하도록 FR-5 개편
v1.0.5 (2026-10-13): 카카오 로그인 단일화(PASS/타 소셜 로그인 제거), 비로그인 메인 기능 개방(Guest-first) 및 개인화 기능 시점 로그인 모달 도입
v1.0.4 (2026-10-13): 데이터 소스를 네이버 플레이스 → 카카오 Local API로, 인스타그램 후기/Trust Score(FR-5) 추가, Langfuse LLM Observability 추가
v1.1 (예정): 베타 테스트 피드백 반영

```

---

이 PRD는 실제 개발에 바로 투입 가능한 수준으로 작성되었습니다. 각 팀원이 이 문서만 보고도 "내가 뭘 만들어야 하는지" 명확하게 이해할 수 있도록 구체적인 API 스펙, DB 스키마, 기술 스택까지 포함했습니다.

**다음 액션:**

1. 이 PRD를 팀 전체가 리뷰 (30분)
2. 질문/이슈 사항 수집 후 최종 승인
3. Jira에 Epic/Story 생성 → 스프린트 시작! 🚀