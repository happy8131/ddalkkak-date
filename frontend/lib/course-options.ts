// 코스 입력 옵션 (prd.md FR-2.1 기준)

// 초기 지원 지역: 서울 5대 상권
export const REGIONS = [
  {
    id: "hongdae",
    name: "홍대",
    description: "공연 · 클럽 · 버스킹",
    // 일러스트 지도 위 위치 (viewBox 0~100 기준 %)
    x: 24,
    y: 42,
  },
  {
    id: "yeonnam",
    name: "연남",
    description: "골목 카페 · 경의선숲길",
    x: 22,
    y: 24,
  },
  {
    id: "itaewon",
    name: "이태원",
    description: "세계 음식 · 루프탑",
    x: 50,
    y: 50,
  },
  {
    id: "seongsu",
    name: "성수",
    description: "팝업 · 공방 · 서울숲",
    x: 74,
    y: 40,
  },
  {
    id: "gangnam",
    name: "강남",
    description: "쇼핑 · 맛집 · 야경",
    x: 70,
    y: 76,
  },
] as const;

export type Region = (typeof REGIONS)[number]["name"];

// 데이트 유형 (값은 백엔드 date_type enum과 동일한 한글 문자열)
export const DATE_TYPES = [
  {
    value: "문화데이트",
    label: "문화",
    emoji: "🎨",
    description: "전시 · 뮤지엄 · 카페",
  },
  {
    value: "맛집투어",
    label: "맛집",
    emoji: "🍝",
    description: "식당 · 디저트",
  },
  {
    value: "액티비티",
    label: "액티비티",
    emoji: "🎳",
    description: "방탈출 · 볼링 · 스크린골프",
  },
  {
    value: "감성데이트",
    label: "감성",
    emoji: "🌃",
    description: "루프탑 · 와인바 · 야경",
  },
] as const;

export type DateType = (typeof DATE_TYPES)[number]["value"];

// 예산 슬라이더 (원)
export const BUDGET = {
  min: 30_000,
  max: 300_000,
  step: 10_000,
  default: 100_000,
} as const;

export function formatWon(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}
